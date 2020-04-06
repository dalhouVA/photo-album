package components

import java.util.UUID

case class ImageDAO(id: UUID, innerID: UUID, name: String, uri: String, visibility: Boolean) {
  def convert: Image = Image(innerID, name, None, Some(uri), visibility)
}

object ImageDAO {
  def empty: ImageDAO = ImageDAO(UUID.fromString(""), UUID.fromString(""), "", "", false)

  def mapperTo(id: UUID, innerID: UUID, name: String, uri: String, visibility: Boolean): ImageDAO = apply(id, innerID, name, uri, visibility)
}
