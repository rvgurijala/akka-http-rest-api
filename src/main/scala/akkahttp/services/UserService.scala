package akkahttp.services

import akka.actor.ActorSystem
import akka.pattern._
import akka.util.Timeout
import akkahttp.db.User
import akkahttp.repositories.UserRepository
import akkahttp.services.UserMessages.{RegisterUser, RetrieveUser}
import akkahttp.utils.HashGenerator._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.SECONDS

object UserMessages {
  case class RetrieveUser(userName: String)
  case class RegisterUser(user: User)
}

class UserService(
    userRepository: UserRepository
  )(implicit actorSystem: ActorSystem,
    ec: ExecutionContext) {
  val userManagerRef = actorSystem.actorSelection("/user/userServiceManager")

  implicit val timeout: Timeout = Timeout(10, SECONDS)

  def retrieveUserByName(userName: String) =
    (userManagerRef ? RetrieveUser(userName)).mapTo[Option[User]]

  def registerUser(user: User) = {
    if(!user.isAdmin) {
      val encryptedPassword = sha256Hash(user.password)
      (userManagerRef ? RegisterUser(user.copy(password = encryptedPassword))).mapTo[Option[User]]
    } else {
      Future.successful(None)
    }
  }
}

class UserServiceManager(userRepository: UserRepository) extends BookShopActor {
  override def receive: Receive = {
    case RetrieveUser(userName: String) => userRepository.getUserByName(userName) pipeTo sender()
    case RegisterUser(user: User)       => {
      log.info(s"registering user: ${user.name}")
      userRepository.create(user) pipeTo sender()
    }

  }
}
