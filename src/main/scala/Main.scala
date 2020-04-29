import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import authentication.{Auth, Authentication}
import authorization.BasicAuthorization
import database.H2DBFile
import generator.UUIDGenerator
import repository._
import route.Routes
import services.album.{AlbumService, DBAlbumService}
import services.image.{DBImageService, ImageService}

import scala.concurrent.ExecutionContextExecutor

object Main extends App with Routes {
  implicit val system: ActorSystem = ActorSystem("photo-album")
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val generator = new UUIDGenerator
  val imageRepo: ImageRepo = new ImageRepoDB(generator) with H2DBFile
  val albumRepo: AlbumRepo = new AlbumRepoDB(generator) with H2DBFile
  val userRepo: UserRepo = new UserRepoDB() with H2DBFile

  override val authorization = new BasicAuthorization
  override val imageService: ImageService = new DBImageService(imageRepo)
  override val albumService: AlbumService = new DBAlbumService(albumRepo)
  override val auth: Authentication = new Auth(userRepo)

  Http().bindAndHandle(routes, "localhost", 9000)
}
