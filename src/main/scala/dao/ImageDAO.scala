package dao

import java.util.UUID

import components.Album

case class ImageDAO(id: Option[UUID], name: String, uri: String, visibility: Boolean)

object ImageDAO {
  def empty: ImageDAO = ImageDAO(None, "", "", visibility = false)
}
