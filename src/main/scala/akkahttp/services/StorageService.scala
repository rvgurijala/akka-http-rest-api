package akkahttp.services

import java.util.UUID

import akka.NotUsed
import akka.actor.ActorSystem
import akka.pattern._
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.Timeout
import akkahttp.db._
import akkahttp.repositories.{BookRepository, OrderRepository}
import akkahttp.services.StorageMessages._
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration.SECONDS
import scala.concurrent.{ExecutionContext, Future}

object StorageMessages {
  case class CreateBook(book: Book)

  case class UpdateBook(
      id: UUID,
      book: Book)

  case class DeleteBook(id: UUID)

  case class RetrieveBook(id: UUID)

  case class CreateOrder(order: Order)

  case class RetrieveUserOrders(userId: UUID)

  case object RetrieveAllBooks

  case object RetrieveOrders
}

class StorageService(
    bookRepository: BookRepository,
    orderRepository: OrderRepository
  )(implicit actorSystem: ActorSystem,
    executionContext: ExecutionContext)
    extends LazyLogging {
  val internalStorageServiceRef =
    actorSystem.actorSelection("/user/internalStorageServiceManager")
  private val books1: List[External] =
    (1 to 3).map(x => External(UUID.randomUUID(), s"Book$x", "Storage1")).toList
  private val books2: List[External] =
    (4 to 6).map(x => External(UUID.randomUUID(), s"Book$x", "Storage2")).toList

  def retrieveAllBooks = {
    val internalBooks  = (internalStorageServiceRef ? RetrieveAllBooks).mapTo[Seq[Book]]
    val externalBooks1 = getBooksFromStorage1.toMat(Sink.seq)(Keep.right).run()
    val externalBooks2 = getBooksFromStorage2

    for {
      books  <- internalBooks
      books1 <- externalBooks1
      books2 <- externalBooks2
    } yield books ++ books1 ++ books2
  }

  implicit val timeout: Timeout = Timeout(10, SECONDS)

  def getBooksFromStorage1: Source[External, NotUsed] = Source(books1)


  def getBooksFromStorage2(implicit executionContext: ExecutionContext): Future[List[External]] =
    Future {
      Thread.sleep(5000)
      books2
    }

  def retrieveBook(bookId: UUID) =
    (internalStorageServiceRef ? RetrieveBook(bookId)).mapTo[Option[Book]]

  def createInternalBook(book: Internal) =
    (internalStorageServiceRef ? CreateBook(book)).mapTo[Option[Book]]

  def updateInternalBook(
      bookId: UUID,
      book: Internal
    ) =
    (internalStorageServiceRef ? UpdateBook(bookId, book)).mapTo[Option[Book]]

  def deleteInternalBook(bookId: UUID) =
    (internalStorageServiceRef ? DeleteBook(bookId)).mapTo[Option[Book]]

  def retrieveAllOrders =
    (internalStorageServiceRef ? RetrieveOrders).mapTo[Seq[Order]]

  def retrieveUserOrders(userId: UUID) =
    (internalStorageServiceRef ? RetrieveUserOrders(userId))
      .mapTo[Seq[Order]]

  def createOrder(order: Order) =
    (internalStorageServiceRef ? CreateOrder(order)).mapTo[Option[Order]]
}

class InternalStorageServiceManager(
    implicit bookRepository: BookRepository,
    orderRepository: OrderRepository)
    extends BookShopActor {
  import akkahttp.services.StorageMessages._

  def receive = {
    case RetrieveAllBooks => bookRepository.getAllBooks pipeTo sender()

    case CreateBook(book: Internal) => {
      log.info(s"creating book. book: ${book}")
      bookRepository.createBook(book) pipeTo sender()
    }

    case UpdateBook(id: UUID, book: Internal) => {
      log.info(s"updating book. book: ${book}")
      bookRepository.updateBook(id, book) pipeTo sender()
    }

    case DeleteBook(id: UUID) => {
      log.info(s"deleting user. userId: ${id}")
      bookRepository.deleteBook(id) pipeTo sender()
    }

    case RetrieveBook(id: UUID) =>
      bookRepository.getBookById(id) pipeTo sender()

    case RetrieveOrders => orderRepository.getAllOrders() pipeTo sender()

    case CreateOrder(order: Order) => {
      log.info(s"creating order. order: ${order}")
      orderRepository.createOrder(order) pipeTo sender()
    }

    case RetrieveUserOrders(userId: UUID) =>
      orderRepository.getUserOrders(userId) pipeTo sender()
  }
}
