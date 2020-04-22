import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import authentication.Auth
import database.H2DBFile
import generator.UUIDGenerator
import repository.{ImageRepo, ImageRepoDB, UserRepo, UserRepoDB}
import route.Routes
import service.DBImageService
import store.ImageStore

import scala.concurrent.ExecutionContextExecutor

object Main extends App with Routes {
  implicit val system: ActorSystem = ActorSystem("photo-album")
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val generator = new UUIDGenerator
  val repo: ImageRepo = new ImageRepoDB(generator) with H2DBFile
  val userRepo: UserRepo = new UserRepoDB() with H2DBFile
  val store = new ImageStore {}

  Http().bindAndHandle(routes(new DBImageService(repo), new Auth(userRepo), store), "localhost", 9000)
}
