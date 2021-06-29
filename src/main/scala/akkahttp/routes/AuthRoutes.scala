package akkahttp.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, path, post, _}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import akkahttp.db.User
import akkahttp.models.UserLoginRequest
import akkahttp.services.AuthService
import akkahttp.utils.HttpEntityHelper._
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.SECONDS

trait AuthRoutes {
  implicit def executionContext: ExecutionContext

  def authServiceInstance: AuthService

  private implicit val timeout: Timeout = Timeout(10, SECONDS)

  def authRoute: Route =
    post {
      (path("login") & formFields(
        "grant_type".as[String],
        "username".as[String],
        "password".as[String]
      )) { (grantType, userName, password) =>
        complete {
          authServiceInstance.login(
            UserLoginRequest(grantType, userName, password)
          ) map {
            case Some(oauth) =>
              StatusCodes.OK ->
                getHttpEntity(Json.toJson(oauth).toString())
            case None =>
              StatusCodes.Unauthorized ->
                getHttpEntity("""{"message": "Invalid credentials"}""")
          }
        }
      } ~
        path("logout") {
          authenticateOAuth2Async(
            "akkahttp",
            authServiceInstance.authenticator
          ) { user =>
            {
              authServiceInstance.logout(user.oAuthToken.access_token)
              complete {
                StatusCodes.OK ->
                  getHttpEntity("""{"message": "logged out successfully"}""")
              }
            }
          }
        } ~
        (path("register") & entity(as[User])) { user =>
          {
            complete {
              authServiceInstance.registerUser(user).map { _ =>
                StatusCodes.Created ->
                  getHttpEntity("""{"message": "registered successfully"}""")
              }
            }
          }
        }
    }
}
