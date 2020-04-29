package route

import java.util.UUID

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import authentication.Authentication
import authorization.Authorization
import components.{Image, Role}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import dto.InImageDTO
import dto.converters.ImageDTOConverter
import io.circe.generic.auto._
import services.image.ImageService

import scala.concurrent.ExecutionContext

class ImageRoute(imageService: ImageService, auth: Authentication, authorization: Authorization)(implicit ex: ExecutionContext) {

  def route: Route = pathPrefix("images") {
    auth.authenticate { user =>
      authorization.authorize(user, requiredLevel = Role.User) {
        get {
          path(Segment) { id =>
            getImageHandler(UUID.fromString(id), StatusCodes.OK)
          } ~
            complete(imageService.getAllImg().map(_.map(ImageDTOConverter.fromImage)))
        } ~
          post {
            path("upload") {
              entity(as[InImageDTO]) {
                img =>
                  uploadImage(img)
              }
            }
          } ~
          delete {
            path(Segment) { id =>
              complete(imageService.delete(UUID.fromString(id)))
            }
          }
      } ~
        authorization.authorize(user, requiredLevel = Role.Guest) {
          get {
            path(Segment) { id =>
              onSuccess(imageService.getPublicImageById(UUID.fromString(id)).map {
                case Some(img) => complete(StatusCodes.OK, ImageDTOConverter.fromImage(img))
                case None => complete(StatusCodes.Unauthorized, "You have to authorize for get access")
              }) { result => result }
            } ~
              complete(imageService.getPublicImages().map(_.map(ImageDTOConverter.fromImage)))
          }
        }
    }
  }

  private def uploadImage(img: InImageDTO): Route = {
    onSuccess(imageService.upload(Image(None, img.name, Some(""), img.visibility), img.base64Image).map {
      case Some(id) => getImageHandler(id, StatusCodes.Created)
      case None => complete(StatusCodes.BadRequest, "Invalid image")
    }) { res => res }
  }

  private def getImageHandler(id: UUID, successCode: StatusCode): Route =
    onSuccess(for {
      img <- imageService.getImgById(id)
    } yield img match {
      case Some(image) => complete(successCode, ImageDTOConverter.fromImage(image))
      case None => complete(StatusCodes.BadRequest, "Image with this id not found")
    }) { result => result }

}
