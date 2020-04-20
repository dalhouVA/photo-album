package route

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive0, Route}
import authentication.Authentication
import core.Role._
import core.{Image, LoggedInUser, Role}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import dto.InImageDTO
import dto.converters.ImageDTOConverter
import io.circe.generic.auto._
import service.ImageService
import store.Store

import scala.concurrent.ExecutionContext

trait Routes {
  def route(service: ImageService, auth: Authentication, store: Store)(implicit exc: ExecutionContext): Route = pathPrefix("album") {
    auth.authenticate { user =>
      authorize(user, requiredLevel = Role.User) {
        get {
          path(Segment) { id =>
            onSuccess(for (img <- service.getImgById(UUID.fromString(id))) yield img match {
              case Some(image) => complete(StatusCodes.OK, ImageDTOConverter.fromImage(image))
              case None => complete(StatusCodes.BadRequest, "Image with this id not found")
            }) { result => result }
          } ~
            complete(service.getAllImg.map(_.map(ImageDTOConverter.fromImage)))
        } ~
          post {
            path("upload") {
              entity(as[InImageDTO]) {
                img =>
                  val path = store.saveImage(img.base64Image).getAbsolutePath
                  complete(StatusCodes.Created, service.upload(Image(Some(UUID.randomUUID()), img.name, Some(path), img.visibility,img.albums)))
              }
            }
          } ~
          delete {
            path(Segment) { id =>
              complete(service.delete(UUID.fromString(id)))
            }
          }
      } ~
        authorize(user, requiredLevel = Role.Guest) {
          get {
            path(Segment) { id =>
              onSuccess(service.getPublicImageById(UUID.fromString(id)).map {
                case Some(img) => complete(StatusCodes.OK, ImageDTOConverter.fromImage(img))
                case None => complete(StatusCodes.Unauthorized, "You have to authorize for get access")
              }) { result => result }
            } ~
              complete(service.getPublicImages.map(_.map(ImageDTOConverter.fromImage)))
          }
        }
    }
  }

  def authorize(user: LoggedInUser, requiredLevel: UserRole): Directive0 =
    if (user.role == requiredLevel)
      pass
    else reject

}
