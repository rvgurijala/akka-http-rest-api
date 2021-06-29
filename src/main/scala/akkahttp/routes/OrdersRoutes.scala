package akkahttp.routes

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, get, post, _}
import akka.http.scaladsl.server.Route
import akkahttp.db.Order
import akkahttp.services.{AuthService, StorageService}
import akkahttp.utils.HttpEntityHelper._
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext

trait OrdersRoutes {
  def storageServiceInstance: StorageService

  def authServiceInstance: AuthService

  implicit def executionContext: ExecutionContext

  def ordersRoute: Route =
    pathPrefix("orders") {
      (get & authenticateOAuth2Async(
        "akkahttp",
        authServiceInstance.authenticator
      )) { user =>
        complete {
          if (user.user.isAdmin) {
            storageServiceInstance.retrieveAllOrders.map { orders =>
              StatusCodes.OK -> getHttpEntity(Json.toJson(orders).toString())
            }
          } else {
            storageServiceInstance.retrieveUserOrders(user.user.id).map { orders =>
              StatusCodes.OK -> getHttpEntity( Json.toJson(orders).toString())

            }
          }
        }
      } ~
        (post & authenticateOAuth2Async(
          "akkahttp",
          authServiceInstance.authenticator
        )) { user =>
          entity(as[Order]) { order =>
            complete {
              storageServiceInstance
                .createOrder(
                  Order(UUID.randomUUID(), order.bookId, user.user.id)
                )
                .map {
                  case Some(created) =>
                    StatusCodes.OK ->
                      getHttpEntity(Json.toJson(created).toString())
                  case None =>
                    StatusCodes.InternalServerError ->
                      getHttpEntity("""{"message": "error in create"}""")
                }
            }
          }
        }
    }
}
