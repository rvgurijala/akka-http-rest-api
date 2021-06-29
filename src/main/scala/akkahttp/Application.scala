package akkahttp

import akka.Done
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akkahttp.db.DBInstance
import akkahttp.repositories.{BookRepository, OrderRepository, UserRepository}
import akkahttp.routes.ApiRoutes
import akkahttp.services._
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object Application extends LazyLogging with DBInstance with ApiRoutes {
  implicit val actorSystem: ActorSystem                    = ActorSystem()
  override implicit val executionContext: ExecutionContext = actorSystem.dispatcher


  def main(args: Array[String]): Unit = {
    for {
      _ <- initiateDatabase
      _ <- Http().newServerAt("0.0.0.0", 8080).bindFlow(apiRoutes).andThen {
        case Success(bind) =>
          logger.info(s"Started HTTP server on [${bind.localAddress}]")
        case Failure(err) =>
          logger.error("Could not start HTTP server", err)
          Thread.sleep(1000)
          System.exit(1)
      }
    } yield Done
  }

  val bookRepo  = new BookRepository()
  val userRepo  = new UserRepository()
  val orderRepo = new OrderRepository()

  val storageService = new StorageService(bookRepo, orderRepo)

  val userService = new UserService(userRepo)

  override def authServiceInstance: AuthService = new AuthService(userService)

  override def storageServiceInstance: StorageService = storageService

  val loggedInUserManagerRef =
    actorSystem.actorOf(Props[LoggedInUserManager], "loggedInUserManager")
  val internalStorageServiceRef = actorSystem.actorOf(
    Props(classOf[InternalStorageServiceManager], bookRepo, orderRepo),
    "internalStorageServiceManager"
  )
  val userManagerRef =
    actorSystem.actorOf(Props(classOf[UserServiceManager], userRepo), "userServiceManager")
}
