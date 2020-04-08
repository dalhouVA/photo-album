package store

import java.io.File

import akka.http.scaladsl.server.directives.FileInfo

trait Store {
  def saveImage(fileInfo: FileInfo):File
}

object StoreImage extends Store {
  override def saveImage(fileInfo: FileInfo): File =  new File("D:\\img", fileInfo.fileName)

}
