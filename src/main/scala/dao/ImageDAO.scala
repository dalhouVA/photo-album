package dao

import java.util.UUID

import core.Album

case class ImageDAO(id: Option[UUID], name: String, file: String, visibility: Boolean, albums: List[Album])

object ImageDAO {
  def empty: ImageDAO = ImageDAO(None, "", "", visibility = false,Nil)
}
