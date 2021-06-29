package akkahttp.models

import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.Json

case class OAuthToken(
    access_token: String = java.util.UUID.randomUUID().toString,
    token_type: String = "bearer",
    expires_in: Int = 3600)
    extends PlayJsonSupport

object OAuthToken {
  implicit val oAuthTokenFormat = Json.format[OAuthToken]
}
