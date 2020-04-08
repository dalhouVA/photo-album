package core

import java.io.File
import java.util.UUID

case class Image(id: Option[UUID], name: String, file: Option[File], uri: Option[String], visibility: Boolean) {
  def isEmpty: Boolean = this == Image.empty
}

object Image {
  def empty: Image = Image(None, "", None, None, false)
}
