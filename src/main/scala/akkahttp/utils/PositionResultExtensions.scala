package akkahttp.utils

import java.util.UUID

import slick.jdbc.PositionedResult

object PositionResultExtensions {
  implicit class PgPositionedResult(val r: PositionedResult) extends AnyVal {
    def nextUUID: UUID               = UUID.fromString(r.nextString)
    def nextUUIDOption: Option[UUID] = r.nextStringOption().map(UUID.fromString(_))
  }
}
