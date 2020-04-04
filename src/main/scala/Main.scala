import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import repository.PhotoRepo
import service.PhotoService


object Main extends App with Routes {
  implicit val system = ActorSystem("photo-album")
  implicit val ec = system.dispatcher

  Http().bindAndHandle(route(PhotoService(PhotoRepo)), "localhost", 9000)
}
