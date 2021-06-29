package akkahttp.models

import java.time.LocalDateTime

import akkahttp.db.User
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.Json

case class LoggedInUser(
    user: User,
    oAuthToken: OAuthToken = new OAuthToken,
    loggedInAt: LocalDateTime = LocalDateTime.now())
    extends PlayJsonSupport

object LoggedInUser {
  implicit val userFormat = Json.format[LoggedInUser]
}
