package route

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive0, Route}
import authentication.Authentication
import core.Role._
import core.{Album, Image, LoggedInUser, Role}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import dto.InImageDTO
import dto.converters.ImageDTOConverter
import io.circe.generic.auto._
import service.ImageService
import store.Store

import scala.concurrent.ExecutionContext

trait Routes {
  private def imagesRoute(service: ImageService, auth: Authentication, store: Store)(implicit exc: ExecutionContext): Route = pathPrefix("images") {
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
                val img_id = UUID.randomUUID()
                service.upload(Image(Some(img_id), img.name, Some(path), img.visibility))
                complete(StatusCodes.Created, service.getImgById(img_id).map(_.map(ImageDTOConverter.fromImage)))
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

  private def albumRoute(service: ImageService, auth: Authentication, store: Store)(implicit exc: ExecutionContext): Route = pathPrefix("albums") {
    auth.authenticate { user =>
      authorize(user, requiredLevel = Role.User) {
        get {
          path(Segment) { id =>
            onSuccess(for (alb <- service.getAlbumById(UUID.fromString(id))) yield alb match {
              case Some(album) => complete(StatusCodes.OK, album)
              case None => complete(StatusCodes.BadRequest, "Album with this id not found")
            }) { result => result }
          } ~
            pathPrefix(Segment) { album_id =>
              path("images") {
                complete(service.getImagesByAlbumId(UUID.fromString(album_id)).map(_.map(ImageDTOConverter.fromImage)))
              }
            } ~ complete(service.getAllAlbums)
        } ~
          post {
            path("create") {
              entity(as[Album]) { album =>
                val album_id = UUID.randomUUID()
                complete(StatusCodes.Created, service.createAlbum(Album(Some(album_id), album.name)))
              }
            } ~
              pathPrefix(Segment) { album_id =>
                pathPrefix("images") {
                  path("upload") {
                    entity(as[InImageDTO]) { img =>
                      val path = store.saveImage(img.base64Image).getAbsolutePath
                      val img_id = UUID.randomUUID()
                      complete(StatusCodes.Created, service.createImageFromAlbum(Image(Some(img_id), img.name, Some(path), img.visibility), UUID.fromString(album_id)))
                    }
                  }
                }
              }
          } ~
          put {
            pathPrefix(Segment) { album_id =>
              pathPrefix("images") {
                path(Segment) { image_id =>
                  complete(StatusCodes.Created, service.putImageIntoAlbum(UUID.fromString(image_id), UUID.fromString(album_id)))
                }
              }
            }
          } ~
          delete {
            path(Segment) { album_id =>
              complete(service.deleteAlbum(UUID.fromString(album_id)))
            }
          }
      }
    }
  }

  def routes(service: ImageService, auth: Authentication, store: Store)(implicit exc: ExecutionContext): Route =
    imagesRoute(service, auth, store) ~ albumRoute(service, auth, store)

  def authorize(user: LoggedInUser, requiredLevel: UserRole): Directive0 =
    if (user.role == requiredLevel)
      pass
    else reject

}
