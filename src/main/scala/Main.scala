import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import database.H2DB
import repository.{DBImageRepo, ImageRepo}
import route.Routes
import service.DBImageService

import scala.concurrent.ExecutionContextExecutor

object Main extends App with Routes{
  implicit val system: ActorSystem = ActorSystem("photo-album")
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val repo: ImageRepo = new DBImageRepo() with H2DB

  Http().bindAndHandle(route(new DBImageService(repo)), "localhost", 9000)
}
