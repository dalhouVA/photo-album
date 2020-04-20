package core

import java.util.UUID

case class Image(id: Option[UUID], name: String, uri: Option[String], visibility: Boolean, albums: List[Album]) {
  def isEmpty: Boolean = this == Image.empty
}

object Image {
  def empty: Image = Image(None, "", None, visibility = false, Nil)
}
