package route

import java.util.UUID

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import authentication.Authentication
import authorization.Authorization
import components.{Album, Image, Role}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import dto.InImageDTO
import dto.converters.ImageDTOConverter
import io.circe.generic.auto._
import services.album.AlbumService

import scala.concurrent.{ExecutionContext, Future}

class AlbumRoute(albumService: AlbumService, auth: Authentication, authorization: Authorization)(implicit ex: ExecutionContext) {
  def route: Route = pathPrefix("albums") {
    auth.authenticate { user =>
      authorization.authorize(user, requiredLevel = Role.User) {
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
                      uploadImageFromAlbum(UUID.fromString(albumID), img)
                    }
                  }
                }
              }
          } ~
          put {
            pathPrefix(Segment) { albumID =>
              pathPrefix("images") {
                path(Segment) { image_id =>
                  complete(StatusCodes.Created, albumService.putImageIntoAlbum(UUID.fromString(image_id), UUID.fromString(albumID)))
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

  private def uploadImageFromAlbum(albumID: UUID, img: InImageDTO): Route = {
    onSuccess(albumService.createImageFromAlbum(Image(None, img.name, Some(""), img.visibility), albumID, img.base64Image).flatMap {
      case Some(id) => for {
        img <- albumService.getImage(albumID, id)
      } yield img match {
        case Some(image) => complete(StatusCodes.Created, ImageDTOConverter.fromImage(image))
        case None => complete(StatusCodes.BadRequest, "Image with this id not found")
      }
      case None => Future.successful(complete(StatusCodes.BadRequest, "Invalid image"))
    }) { res => res }
  }


  private def getAlbumHandler(fid: Future[UUID], successCode: StatusCode): Route =
    onSuccess(for {
      id <- fid
      alb <- albumService.getAlbumById(id)
    } yield alb match {
      case Some(album) => complete(successCode, album)
      case None => complete(StatusCodes.BadRequest, "Album with this id not found")
    }) { result => result }
}
