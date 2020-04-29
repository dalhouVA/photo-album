package route

import java.util.UUID

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive0, Route}
import authentication.Authentication
import components.{Album, Image, LoggedInUser, Role}
import components.Role._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import dto.InImageDTO
import dto.converters.ImageDTOConverter
import io.circe.generic.auto._
import services.album.AlbumService
import services.image.ImageService

import scala.concurrent.{ExecutionContext, Future}

trait Routes {
  val imageService: ImageService
  val albumService: AlbumService
  val auth: Authentication
  implicit val ec: ExecutionContext = ExecutionContext.global

  private def imagesRoute: Route = pathPrefix("images") {
    auth.authenticate { user =>
      authorize(user, requiredLevel = Role.User) {
        get {
          path(Segment) { id =>
            getImageHandler(Future.successful(UUID.fromString(id)), StatusCodes.OK)
          } ~
            complete(imageService.getAllImg().map(_.map(ImageDTOConverter.fromImage)))
        } ~
          post {
            path("upload") {
              entity(as[InImageDTO]) {
                img =>
                  imageService.uploadImageInStorage(img.base64Image) match {
                    case Some(file) =>
                      val img_id = imageService.upload(Image(None, img.name, Some(file.getAbsolutePath), img.visibility))
                      getImageHandler(img_id, StatusCodes.Created)
                    case None => complete(StatusCodes.BadRequest, "It's not an image")
                  }
              }
            }
          } ~
          delete {
            path(Segment) { id =>
              complete(imageService.delete(UUID.fromString(id)))
            }
          }
      } ~
        authorize(user, requiredLevel = Role.Guest) {
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

  private def albumRoute: Route = pathPrefix("albums") {
    auth.authenticate { user =>
      authorize(user, requiredLevel = Role.User) {
        get {
          path(Segment) { id =>
            getAlbumHandler(Future.successful(UUID.fromString(id)), StatusCodes.OK)
          } ~
            pathPrefix(Segment) { albumID =>
              path("images") {
                complete(albumService.getImagesByAlbumId(UUID.fromString(albumID)).map(_.map(ImageDTOConverter.fromImage)))
              }
            } ~ complete(albumService.getAllAlbums())
        } ~
          post {
            path("create") {
              entity(as[Album]) { album =>
                val albumID = albumService.createAlbum(Album(None, album.name))
                getAlbumHandler(albumID, StatusCodes.Created)
              }
            } ~
              pathPrefix(Segment) { albumID =>
                pathPrefix("images") {
                  path("upload") {
                    entity(as[InImageDTO]) { img =>
                      imageService.uploadImageInStorage(img.base64Image) match {
                        case Some(file) =>
                          val imgID = albumService.createImageFromAlbum(Image(None, img.name, Some(file.getAbsolutePath), img.visibility), UUID.fromString(albumID))
                          getImageHandler(imgID, StatusCodes.Created)
                        case None => complete(StatusCodes.BadRequest, "It's not an image")
                      }
                    }
                  }
                }
              }
          } ~
          put {
            pathPrefix(Segment) { album_id =>
              pathPrefix("images") {
                path(Segment) { image_id =>
                  complete(StatusCodes.Created, albumService.putImageIntoAlbum(UUID.fromString(image_id), UUID.fromString(album_id)))
                }
              }
            }
          } ~
          delete {
            path(Segment) { album_id =>
              complete(albumService.deleteAlbum(UUID.fromString(album_id)))
            } ~
              pathPrefix(Segment) { album_id =>
                pathPrefix("images") {
                  path(Segment) { image_id =>
                    complete(albumService.deleteImageFromAlbum(UUID.fromString(image_id), UUID.fromString(album_id)))
                  }
                }
              }
          }
      }
    }
  }

  private def getImageHandler(fid: Future[UUID], successCode: StatusCode): Route =
    onSuccess(for {
      id <- fid
      img <- imageService.getImgById(id)
    } yield img match {
      case Some(image) => complete(successCode, ImageDTOConverter.fromImage(image))
      case None => complete(StatusCodes.BadRequest, "Image with this id not found")
    }) { result => result }

  private def getAlbumHandler(fid: Future[UUID], successCode: StatusCode): Route =
    onSuccess(for {
      id <- fid
      alb <- albumService.getAlbumById(id)
    } yield alb match {
      case Some(album) => complete(successCode, album)
      case None => complete(StatusCodes.BadRequest, "Album with this id not found")
    }) { result => result }

  def routes: Route =
    imagesRoute ~ albumRoute

  private def authorize(user: LoggedInUser, requiredLevel: UserRole): Directive0 =
    if (user.role == requiredLevel)
      pass
    else reject

}
