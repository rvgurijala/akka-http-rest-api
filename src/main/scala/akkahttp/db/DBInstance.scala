package akkahttp.db
import java.util.UUID

import akkahttp.utils.HashGenerator._
import slick.jdbc.H2Profile
import slick.jdbc.H2Profile.api._
import slick.migration.api.H2Dialect

import scala.concurrent.ExecutionContext

class UserTable(tag: Tag) extends Table[(UUID, String, String, Boolean)](tag, "USERS") {
  val id       = column[UUID]("ID", O.PrimaryKey)
  val name     = column[String]("NAME", O.Unique)
  val password = column[String]("PASSWORD")
  val isAdmin  = column[Boolean]("IS_ADMIN")
  def *        = (id, name, password, isAdmin)
}

class BookTable(tag: Tag) extends Table[(UUID, String)](tag, "BOOKS") {
  val id    = column[UUID]("ID", O.PrimaryKey)
  val title = column[String]("TITLE")
  def *     = (id, title)
}

class OrderTable(tag: Tag) extends Table[(UUID, UUID, UUID)](tag, "ORDERS") {
  val id        = column[UUID]("ID", O.PrimaryKey)
  val bookId    = column[UUID]("BOOK_ID")
  val userId    = column[UUID]("USER_ID")
  def *         = (id, bookId, userId)
  def userTable = foreignKey("USER_FK", userId, TableQuery[UserTable])(_.id)
  def bookTable = foreignKey("BOOK_FK", bookId, TableQuery[BookTable])(_.id)
}

trait DBInstance {
  implicit val db: H2Profile.backend.Database = Database.forConfig("h2mem")

  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.global

  implicit val dialect: H2Dialect = new H2Dialect

  val userTable = TableQuery[UserTable]

  val bookTable = TableQuery[BookTable]

  val orderTable = TableQuery[OrderTable]

  val userId  = UUID.randomUUID()
  val adminId = UUID.randomUUID()

  val book1Id = UUID.randomUUID()
  val book2Id = UUID.randomUUID()

  def setup = DBIO.seq(
    (userTable.schema ++ bookTable.schema ++ orderTable.schema).create,
    userTable ++= Seq(
      (userId, "user", sha256Hash("user"), false),
      (adminId, "admin", sha256Hash("admin"), true)
    ),
    bookTable ++= Seq(
      (book1Id, "redbook"),
      (book2Id, "FP")
    )
  )

  def initiateDatabase = {
    db.run(setup)
  }
}
