package akkahttp.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, concat, path, _}
import akka.http.scaladsl.server.Route

trait ApiRoutes extends BooksRoutes with OrdersRoutes with AuthRoutes {
  def apiRoutes: Route = concat(healthCheckRoute, authRoute, booksRoute, ordersRoute)

  private def healthCheckRoute: Route = path("health")(complete(StatusCodes.OK))
}
