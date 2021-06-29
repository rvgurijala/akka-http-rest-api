package akkahttp.models

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.Json

case class UserLoginRequest(
    grant_type: String,
    username: String,
    password: String)
    extends PlayJsonSupport

object UserLoginRequest {
  implicit val loginRequestFormat = Json.format[UserLoginRequest]
}
