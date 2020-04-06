import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import database.H2DataBase
import route.Routes
import service.H2ImageService


object Main extends App with Routes {
  implicit val system = ActorSystem("photo-album")
  implicit val ec = system.dispatcher

  for {
    _ <- H2DataBase.init
    _ <- Http().bindAndHandle(route(H2ImageService), "localhost", 9000)
  } yield ()
}
