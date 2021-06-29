package akkahttp.db

import java.util.UUID

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

sealed trait Book extends PlayJsonSupport
final case class Internal(
    id: UUID,
    title: String)
    extends Book
final case class External(
    id: UUID,
    title: String,
    storage: String)
    extends Book

object Book {
  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  implicit val internalReads: Reads[Internal] =
    (JsPath \ "title").read[String].map(title => Internal(UUID.randomUUID(), title))
  implicit val internalWrites: Writes[Internal] = (
    (JsPath \ "id").write[UUID] and
      (JsPath \ "title").write[String]
  )(unlift(Internal.unapply))

  implicit val bookFormatExternal = Json.format[External]
  implicit val bookFormat         = Json.format[Book]
}
