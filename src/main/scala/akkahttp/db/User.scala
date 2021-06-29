package akkahttp.db

import java.util.UUID

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.Json

final case class User(
    id: UUID,
    name: String,
    password: String,
    isAdmin: Boolean)
    extends PlayJsonSupport

object User {
  implicit val userFormat = Json.format[User]
}
