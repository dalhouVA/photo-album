package components

import java.io.File
import java.util.UUID

case class Image(id: UUID, name: String, file: Option[File], uri: Option[String], visibility: Boolean) {
  def isEmpty: Boolean = this == Image.empty

  def convert: ImageDTO = components.ImageDTO(id, name, uri.get)

  def toDAO: ImageDAO = ImageDAO(UUID.randomUUID(), id, file.get.getName, uri.getOrElse(""), visibility)
}

object Image {
  def empty: Image = Image(UUID.fromString(""), "", None, None, false)
}
