package akkahttp.db

import java.util.UUID

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

final case class Order(
    id: UUID,
    bookId: UUID,
    userId: UUID)
    extends PlayJsonSupport

object Order {
  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  implicit val orderReads: Reads[Order] = (JsPath \ "bookId")
    .read[UUID]
    .map(bookId => Order(UUID.randomUUID(), bookId, UUID.randomUUID()))

  implicit val orderWrites: Writes[Order] = (
    (JsPath \ "id").write[UUID] and
      (JsPath \ "bookId").write[UUID] and
      (JsPath \ "userId").write[UUID]
  )(unlift(Order.unapply))
}
