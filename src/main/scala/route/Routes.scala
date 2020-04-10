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

import scala.concurrent.{ExecutionContext, Future}

trait Routes {
  def route(service: ImageService)(implicit exc: ExecutionContext): Route = pathPrefix("album") {
    authenticateBasicAsync("secure", userAuthenticator) {
      case ok if ok == StatusCodes.OK =>
        get {
          path(Segment) { id =>
            onSuccess(for (img <- service.getImgById(UUID.fromString(id))) yield img match {
              case Some(image) => complete(ok, ImageDTOConverter.fromImage(image))
              case None => complete(StatusCodes.BadRequest, "Image with this id not found")
            }) { result => result }
          } ~
            complete(service.getAllImg.map(_.map(ImageDTOConverter.fromImage)))
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
            complete(service.getPublicImages.map(_.map(ImageDTOConverter.fromImage)))
        }
    }
  }

  def userAuthenticator(credentials: Credentials): Future[Option[StatusCode]] = credentials match {
    case p@Credentials.Provided(id) if p.verify(Users.listOfUsers.getOrElse(id, "")) => Future.successful(Some(StatusCodes.OK))
    case _ => Future.successful(Some(StatusCodes.Unauthorized))
  }
}

object Users {
  val listOfUsers: Map[String, String] = Map(
    "justice" -> "ololo",
    "jane" -> "123"
  )
}
