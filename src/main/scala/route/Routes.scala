package route

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import authentication.Authentication
import authorization.Authorization
import services.album.AlbumService
import services.image.ImageService

import scala.concurrent.ExecutionContext.Implicits.global

trait Routes {
  val imageService: ImageService
  val albumService: AlbumService
  val auth: Authentication
  val authorization: Authorization

  def imagesRoute: Route = new ImageRoute(imageService, auth, authorization).route

  def albumRoute: Route = new AlbumRoute(albumService, auth, authorization).route

  def routes: Route =
    imagesRoute ~ albumRoute

}
