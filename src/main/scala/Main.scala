import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import database.{DataBase, H2ImageDb}
import route.Routes
import service.H2ImageService


object Main extends App with Routes {
  implicit val system = ActorSystem("photo-album")
  implicit val ec = system.dispatcher

  for {
    _ <- new DataBase(H2ImageDb.config, H2ImageDb.db).init
    _ <- Http().bindAndHandle(route(H2ImageService), "localhost", 9000)
  } yield ()
}
