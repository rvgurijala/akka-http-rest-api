package akkahttp.repositories

import java.util.UUID

import slick.jdbc.SetParameter

trait Repository {
  implicit val uuidSetter = SetParameter[UUID] { (uuid, params) =>
    params.setString(uuid.toString)
  }
}
