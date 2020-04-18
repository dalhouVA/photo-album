import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import authentication.Auth
import database.H2DBFile
import repository.{ImageRepo, ImageRepoDB, UserRepo, UserRepoDB}
import route.Routes
import service.DBImageService

import scala.concurrent.ExecutionContextExecutor

object Main extends App with Routes {
  implicit val system: ActorSystem = ActorSystem("photo-album")
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val repo: ImageRepo = new ImageRepoDB() with H2DBFile
  val userRepo: UserRepo = new UserRepoDB() with H2DBFile

  Http().bindAndHandle(route(new DBImageService(repo), new Auth(userRepo)), "localhost", 9000)
}
