package route

import java.io.File
import java.util.UUID

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.FileInfo
import components.Image
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import service.ImageService

import scala.concurrent.ExecutionContext

trait Routes {
  def route(service: ImageService)(implicit exc: ExecutionContext): Route = pathPrefix("album") {
    get {
      path(Segment) { id =>
        complete(service.getImg(UUID.fromString(id)).map(_.convert))
      } ~ complete(service.getAllImg.map(_.map(_.convert)))
    } ~
      post {
        path("upload") {
          storeUploadedFile("jpg", destination) {
            case (metadata, file) =>
              complete(service.upload(Image(UUID.randomUUID(), metadata.fileName, Some(file), Some(file.getAbsolutePath), true)))
          }
        }
      } ~
      delete {
        path(Segment) { id =>
          complete(service.delete(UUID.fromString(id)))
        }
      }
  }

  def destination(fileInfo: FileInfo): File = new File("D:\\img", fileInfo.fileName)
}
