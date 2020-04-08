package route

import java.util.UUID

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.Credentials
import core.Image
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import dto.converters.ImageDTOConverter
import io.circe.generic.auto._
import service.ImageService
import store.StoreImage

import scala.concurrent.ExecutionContext

trait Routes {
  def route(service: ImageService)(implicit exc: ExecutionContext): Route = pathPrefix("album") {
    authenticateBasic("secure", userAuthenticator) {
      case ok if ok == StatusCodes.OK =>
        get {
          path(Segment) { id =>
            onSuccess(for (img <- service.getImg(UUID.fromString(id))) yield img match {
              case Some(image) => complete(ok, ImageDTOConverter.fromImage(image))
              case None => complete(StatusCodes.BadRequest, "Image with this id not found")
            }) { result => result }
          } ~
            complete(service.getAllImg)
        } ~
          post {
            (path("upload") & parameter(Symbol("visibility").as[Boolean])) { vis =>
              storeUploadedFile("image", StoreImage.saveImage) {
                case (metadata, file) =>
                  complete(StatusCodes.Created, service.upload(Image(Some(UUID.randomUUID()), metadata.fileName, Some(file), Some(file.getAbsolutePath), vis)))
              }
            }
          } ~
          delete {
            path(Segment) { id =>
              complete(service.delete(UUID.fromString(id)))
            }
          }
      case code =>
        get {
          path(Segment) { id =>
            onSuccess(service.getPublicImageById(UUID.fromString(id)).map {
              case Some(img) => complete(StatusCodes.OK, ImageDTOConverter.fromImage(img))
              case None => complete(code, "You have to authorize for get access")
            }) { result => result }
          } ~
            complete(service.getPublicImages)
        }
    }
  }

  def userAuthenticator(credentials: Credentials): Option[StatusCode] = credentials match {
    case p@Credentials.Provided(id) if p.verify(UsersMap.map.getOrElse(id, "")) => Some(StatusCodes.OK)
    case _ => Some(StatusCodes.Unauthorized)
  }
}

object UsersMap {
  val map: Map[String, String] = Map(
    "justice" -> "ololo",
    "jane" -> "123"
  )
}
