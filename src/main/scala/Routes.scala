import java.io.File
import java.util.UUID

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.FileInfo
import core.Image
import io.circe.generic.auto._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import service.Service
import store.StoreImage

import scala.concurrent.ExecutionContext

trait Routes {
  def route(service: Service)(implicit exc: ExecutionContext): Route = pathPrefix("album") {
    get {
      path(Segment) { id =>
        complete(service.getImg(UUID.fromString(id)).map(_.convert))
      } ~ complete(service.getAllImg.map(_.map(_.convert)))
    } ~
      post {
        path("album" / "upload") {
          storeUploadedFile("jpg", destination) {
            case (metadata, file) =>
              println(metadata.contentType)
              complete(service.upload(Image(UUID.randomUUID(), metadata.fileName, Some(file), None, true)))
          }
        }
      } ~
      delete {
        path(Segment) { id =>
          complete(service.delete(UUID.fromString(id)))
        }
      }
  }

  def destination(fileInfo: FileInfo): File = new File(StoreImage.saveImage.path.toString())
}
