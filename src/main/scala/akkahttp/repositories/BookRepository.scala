package akkahttp.repositories

import java.util.UUID

import akkahttp.db.{BookTable, Internal}
import akkahttp.utils.PositionResultExtensions.PgPositionedResult
import slick.jdbc.GetResult
import slick.jdbc.H2Profile.api._

import scala.concurrent.ExecutionContext

class BookRepository(
    implicit db: Database,
    ec: ExecutionContext)
    extends Repository {

  val books = TableQuery[BookTable]
  implicit val getBookResult = GetResult(r => Internal(r.nextUUID, r.<<))

  def toBook(book: (UUID, String)) = Internal(book._1, book._2)

  def getAllBooks  = db.run(books.result).map(_.map(toBook))

  def getBookById(id: UUID) = db.run(books.filter(_.id === id).result).map(_.headOption).map(_.map(toBook))

  def createBook(book: Internal) = {
    val insertActions = DBIO.seq(
      books += (book.id, book.title)
    )
    db.run(insertActions).flatMap(_ => getBookById(book.id))
  }

  def updateBook(
      id: UUID,
      book: Internal
    ) = {
     db.run(
      (for {
        b <- books if b.id === id
      } yield {
        b
      }).map(_.title).update(book.title)).flatMap(_ => getBookById(id))
  }

  def deleteBook(id: UUID) = {
    val q = books.filter(_.id === id)
    db.run(q.delete).flatMap(_ => getBookById(id))
  }
}
