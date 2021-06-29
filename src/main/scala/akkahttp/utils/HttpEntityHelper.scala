package akkahttp.utils

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}

object HttpEntityHelper {
  def getHttpEntity(msg: String) = HttpEntity(ContentTypes.`application/json`, msg)
}
