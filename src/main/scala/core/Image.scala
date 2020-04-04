package core

import java.io.File
import java.util.UUID

import akka.http.scaladsl.model.Uri
import dto.ImageDTO

case class Image(id: UUID, name: String, file: Option[File], uri: Option[Uri], visibility: Boolean) {
  def isEmpty: Boolean = this == Image.empty

  def convert: ImageDTO = ImageDTO(id, name, uri.get)
}

object Image {
  def empty: Image = Image(UUID.fromString(""), "", None, None, false)
}
