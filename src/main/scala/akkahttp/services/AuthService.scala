package akkahttp.services

import akka.actor.ActorSystem
import akka.http.scaladsl.server.directives.Credentials
import akka.pattern._
import akka.util.Timeout
import akkahttp.db.User
import akkahttp.models.{LoggedInUser, OAuthToken, UserLoginRequest}
import akkahttp.services.UserMessages.{RegisterUser, RetrieveUser}
import com.typesafe.scalalogging.LazyLogging
import akkahttp.utils.HashGenerator._

import scala.concurrent.duration.SECONDS
import scala.concurrent.{ExecutionContext, Future}

class AuthService(
    userService: UserService
  )(implicit actorSystem: ActorSystem,
    ec: ExecutionContext) extends LazyLogging {

  val userActorRef = actorSystem.actorSelection("/user/userServiceManager")

  val loggedInUserActorRef = actorSystem.actorSelection("/user/loggedInUserManager")

  implicit val timeout: Timeout = Timeout(10, SECONDS)

  def authenticator(credentials: Credentials) =
    credentials match {
      case p @ Credentials.Provided(_) => {
        val futureUser =
          (loggedInUserActorRef ? RetrieveLoggedInUser(p.identifier)).mapTo[Option[LoggedInUser]]
        futureUser.map {
          case Some(user) if p.verify(user.oAuthToken.access_token) => Some(user)
          case _                                                    => None
        }
      }
      case _ => Future.successful(None)
    }

  def login(loginRequest: UserLoginRequest): Future[Option[OAuthToken]] = {
    val futureUser = (userActorRef ? RetrieveUser(loginRequest.username)).mapTo[Option[User]]
    futureUser.map {
      case Some(user)
          if user.name == loginRequest.username && user.password == sha256Hash(loginRequest.password) => {
        logger.info(user + " logged in successfully")
        val loggedInUser = LoggedInUser(user)
        addToLoggedInUsers(loggedInUser)
        Some(loggedInUser.oAuthToken)
      }
      case _ => None
    }
  }

  def logout(token: String) = {
    loggedInUserActorRef ! RemoveLoggedInUser(token)
  }

  def addToLoggedInUsers(user: LoggedInUser) = {
    loggedInUserActorRef ! AddLoggedInUser(user)
  }

  def registerUser(user: User) = {
    logger.debug(s"registering user ${user.name}")
    (userActorRef ? RegisterUser(user)).mapTo[Int]
  }
}

case class RetrieveLoggedInUser(token: String)
case class AddLoggedInUser(user: LoggedInUser)
case class RemoveLoggedInUser(token: String)

class LoggedInUserManager extends BookShopActor {
  override def receive: Receive = withLoggedInUser(List.empty)

  def withLoggedInUser(users: List[LoggedInUser]): Receive = {
    case RetrieveLoggedInUser(token) => sender() ! users.find(_.oAuthToken.access_token == token)

    case AddLoggedInUser(user) => context.become(withLoggedInUser(users.::(user)))

    case RemoveLoggedInUser(token) => {
      val userToLogout = users.filter(_.oAuthToken.access_token != token)
      log.info(s"user logging out. ${userToLogout.head}. ")
      context.become(withLoggedInUser(userToLogout))
    }
  }
}
