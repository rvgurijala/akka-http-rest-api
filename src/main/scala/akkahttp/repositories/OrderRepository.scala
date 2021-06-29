package akkahttp.repositories

import java.util.UUID

import akkahttp.db.{Order, OrderTable}
import akkahttp.utils.PositionResultExtensions.PgPositionedResult
import slick.jdbc.GetResult
import slick.jdbc.H2Profile.api._

import scala.concurrent.ExecutionContext

class OrderRepository(
    implicit db: Database,
    ee: ExecutionContext)
    extends Repository {

  val orders                  = TableQuery[OrderTable]
  implicit val getOrderResult = GetResult(r => Order(r.nextUUID, r.nextUUID, r.nextUUID))

  def toOrder(order: (UUID, UUID, UUID)) = Order(order._1, order._2, order._3)

  def getAllOrders() = db.run(orders.result).map(_.map(toOrder))

  def getUserOrders(userId: UUID) =
    db.run(orders.filter(_.userId === userId).result).map(_.map(toOrder))

  def createOrder(order: Order) = {
    val insertActions = DBIO.seq(
      orders += (order.id, order.bookId, order.userId)
    )
    db.run(insertActions).flatMap(_ => getOrderById(order.id))
  }

  def getOrderById(id: UUID) =
    db.run(orders.filter(_.id === id).result).map(_.headOption).map(_.map(toOrder))
}
