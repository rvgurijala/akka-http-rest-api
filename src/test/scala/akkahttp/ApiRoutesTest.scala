package codingtest

import java.util.UUID

import akka.actor.Props
import akka.http.scaladsl.model.StatusCodes.{NotFound, OK}
import akka.http.scaladsl.model.headers.{HttpChallenge, OAuth2BearerToken}
import akka.http.scaladsl.model.{FormData, StatusCodes}
import akka.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import akka.http.scaladsl.server.{AuthenticationFailedRejection, AuthorizationFailedRejection}
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit.TestDuration
import akkahttp.db.DBInstance
import akkahttp.repositories.{BookRepository, OrderRepository, UserRepository}
import akkahttp.routes.ApiRoutes
import akkahttp.services.{AuthService, InternalStorageServiceManager, LoggedInUserManager, StorageService, UserService, UserServiceManager}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json._

import scala.concurrent.duration.DurationInt


class ApiRoutesTest extends AnyWordSpec with ScalatestRouteTest with Matchers with DBInstance with ApiRoutes with OptionValues {
  import PlayJsonSupport._
  implicit val timeout: RouteTestTimeout = RouteTestTimeout(6.seconds.dilated)

  private val httpChallenge = HttpChallenge("Bearer", "akkahttp")
  private val missingCredentials = AuthenticationFailedRejection(CredentialsMissing, httpChallenge)
  private val rejectedCredentials = AuthenticationFailedRejection(CredentialsRejected, httpChallenge)

  private var userToken = ""
  private var adminToken = ""
  private var createdBookId = ""

  private val titleToCreate = "New book"
  private val bookToCreate = JsObject(Map("title" -> JsString(titleToCreate)))
  private val titleToUpdate = "Updated book"
  private val bookToUpdate = JsObject(Map("title" -> JsString(titleToUpdate)))
  private val unknownId = UUID.randomUUID()

  private lazy val orderToCreate = JsObject(Map("bookId" -> JsString(createdBookId)))

  val startDB = initiateDatabase

  "health check" in {
    Get("/health") ~> apiRoutes ~> check {
      response.status shouldEqual StatusCodes.OK
    }
  }

  "api should work well" in {
    Get("/books") ~> apiRoutes ~> check {
      rejection shouldBe missingCredentials
    }

    Get(s"/books/$unknownId") ~> apiRoutes ~> check {
      rejection shouldBe missingCredentials
    }

    Post("/books", JsObject.empty) ~> apiRoutes ~> check {
      rejection shouldBe missingCredentials
    }

    Put(s"/books/$unknownId", JsObject.empty) ~> apiRoutes ~> check {
      rejection shouldBe missingCredentials
    }

    Delete(s"/books/$unknownId") ~> apiRoutes ~> check {
      rejection shouldBe missingCredentials
    }

    Post("/login", FormData("grant_type" -> "password", "username" -> "user", "password" -> "user")) ~> apiRoutes ~> check {
      userToken = (responseAs[JsValue] \ "access_token").as[String]
      status shouldEqual OK
    }

    Post("/login", FormData("grant_type" -> "password", "username" -> "admin", "password" -> "admin")) ~> apiRoutes ~> check {
      adminToken = (responseAs[JsValue] \ "access_token").as[String]
      status shouldEqual OK
    }

    Get("/books") ~> addCredentials(OAuth2BearerToken(userToken)) ~> apiRoutes ~> check {
      responseAs[JsArray].value.nonEmpty shouldBe true
      status shouldBe OK
    }

    Post("/books", bookToCreate) ~> addCredentials(OAuth2BearerToken(userToken)) ~> apiRoutes ~> check {
      rejection shouldBe AuthorizationFailedRejection
    }

    Put(s"/books/$unknownId", JsObject.empty) ~> addCredentials(OAuth2BearerToken(userToken)) ~> apiRoutes ~> check {
      rejection shouldBe AuthorizationFailedRejection
    }

    Delete(s"/books/$unknownId") ~> addCredentials(OAuth2BearerToken(userToken)) ~> apiRoutes ~> check {
      rejection shouldBe AuthorizationFailedRejection
    }

    Post("/books", bookToCreate) ~> addCredentials(OAuth2BearerToken(adminToken)) ~> apiRoutes ~> check {
      (responseAs[JsValue] \ "title").as[String] shouldBe titleToCreate
      createdBookId = (responseAs[JsValue] \ "id").as[String]

      status shouldEqual OK
    }

    Put(s"/books/$createdBookId", bookToUpdate) ~> addCredentials(OAuth2BearerToken(adminToken)) ~> apiRoutes ~> check {
      (responseAs[JsValue] \ "title").as[String] shouldBe titleToUpdate
      createdBookId = (responseAs[JsValue] \ "id").as[String]

      status shouldEqual OK
    }

    Get(s"/books/$createdBookId") ~> addCredentials(OAuth2BearerToken(userToken)) ~> apiRoutes ~> check {
      (responseAs[JsValue] \ "id").as[String] shouldBe createdBookId
      (responseAs[JsValue]\ "title").as[String] shouldBe titleToUpdate

      status shouldBe OK
    }

    Delete(s"/books/$createdBookId") ~> addCredentials(OAuth2BearerToken(adminToken)) ~> apiRoutes ~> check {
      status shouldEqual OK
    }

    Get(s"/books/$createdBookId") ~> addCredentials(OAuth2BearerToken(userToken)) ~> apiRoutes ~> check {
      status shouldBe NotFound
    }

    Post("/books", bookToCreate) ~> addCredentials(OAuth2BearerToken(adminToken)) ~> apiRoutes ~> check {
      (responseAs[JsValue] \ "title").as[String] shouldBe titleToCreate
      createdBookId = (responseAs[JsValue] \ "id").as[String]

      status shouldEqual OK
    }

    Get("/orders") ~> apiRoutes ~> check {
      rejection shouldBe missingCredentials
    }
    Post("/orders") ~> apiRoutes ~> check {
      rejection shouldBe missingCredentials
    }

    Get("/orders") ~> addCredentials(OAuth2BearerToken(userToken)) ~> apiRoutes ~> check {
      responseAs[JsArray].value.isEmpty shouldBe true
      status shouldBe OK
    }

    Post("/orders", orderToCreate) ~> addCredentials(OAuth2BearerToken(userToken)) ~> apiRoutes ~> check {
      (responseAs[JsValue] \ "bookId").as[String] shouldBe createdBookId
      status shouldBe OK
    }

    Post("/logout") ~> addCredentials(OAuth2BearerToken(userToken)) ~> apiRoutes ~> check {
      status shouldEqual OK
    }

    Get("/orders") ~> addCredentials(OAuth2BearerToken(userToken)) ~> apiRoutes ~> check {
      rejection shouldBe rejectedCredentials
    }
  }

  val bookRepo = new BookRepository()
  val userRepo = new UserRepository()
  val orderRepo = new OrderRepository()

  val storageService = new StorageService(bookRepo, orderRepo)
  val userService = new UserService(userRepo)

  override def authServiceInstance: AuthService = new AuthService(userService)

  override def storageServiceInstance: StorageService = storageService

  val loggedInUserManagerRef = system.actorOf(Props[LoggedInUserManager], "loggedInUserManager")
  val internalStorageServiceRef = system.actorOf(Props(classOf[InternalStorageServiceManager],bookRepo, orderRepo), "internalStorageServiceManager")
  val userManagerRef = system.actorOf(Props(classOf[UserServiceManager],userRepo), "userServiceManager")
}
