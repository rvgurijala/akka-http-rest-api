package akkahttp.repositories

import java.util.UUID

import akkahttp.db.{User, UserTable}
import akkahttp.utils.PositionResultExtensions.PgPositionedResult
import slick.jdbc.GetResult
import slick.jdbc.H2Profile.api._

import scala.concurrent.ExecutionContext

class UserRepository(
    implicit db: Database,
    ec: ExecutionContext)
    extends Repository {
  val users = TableQuery[UserTable]
  implicit val getUserResult = GetResult(r => User(r.nextUUID, r.<<, r.<<, r.<<))

  def toUser(user: (UUID, String, String, Boolean)) = User(user._1, user._2, user._3, user._4)

  def getUserById(id: UUID) = db.run(users.filter(_.id === id).result).map(_.headOption).map(_.map(toUser))

  def create(user: User) =  {
    val insertActions = DBIO.seq(
      users += (user.id, user.name, user.password, user.isAdmin)
    )
    db.run(insertActions).flatMap(_ => getUserById(user.id))
  }

  def getUserByName(userName: String) = db.run(users.filter(_.name === userName).result).map(_.headOption).map(_.map(toUser))
}
