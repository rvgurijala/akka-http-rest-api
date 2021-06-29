package akkahttp.routes

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, get, path, pathEndOrSingleSlash, _}
import akka.http.scaladsl.server.{AuthorizationFailedRejection, Route}
import akka.util.Timeout
import akkahttp.db.Internal
import akkahttp.services.{AuthService, StorageService}
import play.api.libs.json.Json
import akkahttp.utils.HttpEntityHelper._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.SECONDS

trait BooksRoutes {
  def storageServiceInstance: StorageService
  def authServiceInstance: AuthService

  implicit def executionContext: ExecutionContext

  private implicit val timeout: Timeout = Timeout(10, SECONDS)

  def booksRoute: Route =
    pathPrefix("books") {
      (get & authenticateOAuth2Async(
        "akkahttp",
        authServiceInstance.authenticator
      )) { _ =>
        pathEndOrSingleSlash {
          complete {
            storageServiceInstance.retrieveAllBooks.map { books =>
              StatusCodes.OK -> HttpEntity(
                ContentTypes.`application/json`,
                Json.toJson(books).toString()
              )
            }
          }
        } ~
          path(JavaUUID) { bookId =>
            {
              complete {
                storageServiceInstance.retrieveBook(bookId).map {
                  _ match {
                    case Some(book) =>
                      StatusCodes.OK -> getHttpEntity(Json.toJson(book).toString())
                    case _ => StatusCodes.NotFound -> getHttpEntity("""{"message": "No Data Found"}"""")
                  }
                }
              }
            }
          }
      } ~
        (post & authenticateOAuth2Async(
          "akkahttp",
          authServiceInstance.authenticator
        )) { user =>
          if (!user.user.isAdmin) {
            reject(AuthorizationFailedRejection)
          } else {
            entity(as[Internal]) { book =>
              {
                complete {
                  storageServiceInstance
                    .createInternalBook(book)
                    .map {
                      case Some(book) =>
                        StatusCodes.OK -> getHttpEntity(Json.toJson(book).toString())
                      case None =>
                        StatusCodes.InternalServerError ->
                          getHttpEntity("""{"message": "error in create"}""")
                    }
                }
              }
            }
          }
        } ~
        (put & path(JavaUUID) & authenticateOAuth2Async(
          "akkahttp",
          authServiceInstance.authenticator
        )) { (bookId, user) =>
          if (!user.user.isAdmin) {
            reject(AuthorizationFailedRejection)
          } else {
            entity(as[Internal]) { book =>
              {
                complete {
                  storageServiceInstance
                    .updateInternalBook(bookId, book)
                    .map {
                      case Some(book) =>
                        StatusCodes.OK ->
                          getHttpEntity(Json.toJson(book).toString())
                      case None =>
                        StatusCodes.InternalServerError ->
                          getHttpEntity(""""{message": "error in update"}""")
                    }
                }
              }
            }
          }
        } ~
        (delete & path(JavaUUID) & authenticateOAuth2Async(
          "akkahttp",
          authServiceInstance.authenticator
        )) { (bookId, user) =>
          if (!user.user.isAdmin) {
            reject(AuthorizationFailedRejection)
          } else {
            complete {
              storageServiceInstance
                .deleteInternalBook(bookId)
                .map { _ =>
                  StatusCodes.OK
                }
                .recover {
                  case ex => {
                    StatusCodes.InternalServerError
                  }
                }
            }
          }
        }
    }
}
