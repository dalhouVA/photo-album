import java.io.File
import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.FileInfo

trait WebServer {
  val routes: Route =
    (post & path("album" / "upload")) {

      storeUploadedFile("jpg",destination){
        case (metadata,_) =>
          println(metadata.contentType)
          complete("file stored")
      }
    }
  def destination(fileInfo:FileInfo): File =  new File("D:\\img",s"${UUID.randomUUID()}.${fileInfo.contentType.mediaType.subType}")
  def headerVal = ???
}

object WebServer extends WebServer with App {
  implicit val system = ActorSystem("photo-album")
  implicit val ec = system.dispatcher

  Http().bindAndHandle(routes, "localhost", 9000)
}
