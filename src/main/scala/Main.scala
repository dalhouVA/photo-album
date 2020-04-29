import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import authentication.{Auth, Authentication}
import authorization.BasicAuthorization
import database.H2DBFile
import generator.{Generator, UUIDGenerator}
import repository._
import route.Routes
import services.album.{AlbumService, DBAlbumService}
import services.image.{DBImageService, ImageService}

import scala.concurrent.ExecutionContextExecutor

object Main extends App with Routes {
  implicit val system: ActorSystem = ActorSystem("photo-album")
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val uuidGen: Generator = new UUIDGenerator
  val userRepo: UserRepo = new UserRepoDB() with H2DBFile
  val photoRepo: PhotoRepo = new LocalPhotoRepo(uuidGen)

  val imageRepo: ImageRepo = new ImageRepoDB with H2DBFile {
    override val generator: Generator = new UUIDGenerator
  }

  val albumRepo: AlbumRepo = new AlbumRepoDB with H2DBFile {
    override val generator: Generator = new UUIDGenerator
  }

  override val authorization = new BasicAuthorization
  override val imageService: ImageService = new DBImageService(imageRepo, photoRepo)
  override val albumService: AlbumService = new DBAlbumService(albumRepo, photoRepo)
  override val auth: Authentication = new Auth(userRepo)

  Http().bindAndHandle(routes, "localhost", 9000)
}
