package route

import java.util.UUID

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
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

import scala.concurrent.{ExecutionContext, Future}

trait Routes {
  private def imagesRoute(service: ImageService, auth: Authentication, store: Store)(implicit exc: ExecutionContext): Route = pathPrefix("images") {
    auth.authenticate { user =>
      authorize(user, requiredLevel = Role.User) {
        get {
          path(Segment) { id =>
            getImageHandler(Future.successful(UUID.fromString(id)), service, StatusCodes.OK)
          } ~
            complete(service.getAllImg.map(_.map(ImageDTOConverter.fromImage)))
        } ~
          post {
            path("upload") {
              entity(as[InImageDTO]) {
                img =>
                  val path = store.saveImage(img.base64Image).getAbsolutePath
                  val img_id = service.upload(Image(None, img.name, Some(path), img.visibility))
                  getImageHandler(img_id, service, StatusCodes.Created)
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
            getAlbumHandler(Future.successful(UUID.fromString(id)), service, StatusCodes.OK)
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
                val album_id = service.createAlbum(Album(None, album.name))
                getAlbumHandler(album_id, service, StatusCodes.Created)
              }
            } ~
              pathPrefix(Segment) { album_id =>
                pathPrefix("images") {
                  path("upload") {
                    entity(as[InImageDTO]) { img =>
                      val path = store.saveImage(img.base64Image).getAbsolutePath
                      val img_id = service.createImageFromAlbum(Image(None, img.name, Some(path), img.visibility), UUID.fromString(album_id))
                      getImageHandler(img_id, service, StatusCodes.Created)
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
            } ~
              pathPrefix(Segment) { album_id =>
                pathPrefix("images") {
                  path(Segment) { image_id =>
                    complete(service.deleteImageFromAlbum(UUID.fromString(image_id), UUID.fromString(album_id)))
                  }
                }
              }
          }
      }
    }
  }

  private def getImageHandler(fid: Future[UUID], service: ImageService, successCode: StatusCode)(implicit ex: ExecutionContext): Route =
    onSuccess(for {
      id <- fid
      img <- service.getImgById(id)
    } yield img match {
      case Some(image) => complete(successCode, ImageDTOConverter.fromImage(image))
      case None => complete(StatusCodes.BadRequest, "Image with this id not found")
    }) { result => result }

  private def getAlbumHandler(fid: Future[UUID], service: ImageService, successCode: StatusCode)(implicit ex: ExecutionContext): Route =
    onSuccess(for {
      id <- fid
      alb <- service.getAlbumById(id)
      val s = alb
    } yield alb match {
      case Some(album) => complete(successCode, album)
      case None => complete(StatusCodes.BadRequest, "Album with this id not found")
    }) { result => result }

  def routes(service: ImageService, auth: Authentication, store: Store)(implicit exc: ExecutionContext): Route =
    imagesRoute(service, auth, store) ~ albumRoute(service, auth, store)

  private def authorize(user: LoggedInUser, requiredLevel: UserRole): Directive0 =
    if (user.role == requiredLevel)
      pass
    else reject

}
