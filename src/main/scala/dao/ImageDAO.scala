package dao

import java.util.UUID

import akka.http.scaladsl.model.Uri
import core.Image

case class ImageDAO(id: UUID, innerID: UUID, name: String, uri: Uri, visibility: Boolean) {
  def convert: Image = Image(innerID, name, None, Some(uri),visibility)
}
