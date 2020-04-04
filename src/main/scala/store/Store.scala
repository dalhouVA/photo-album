package store

import akka.http.scaladsl.model.Uri

trait Store {
  def saveImage:Uri
}

object StoreImage extends Store {
  override def saveImage: Uri = ???
}
