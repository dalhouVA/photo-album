package dao.converters

import components.Image
import dao.ImageDAO

object ImageDAOConverter {

  def toImage: ImageDAO => Image = dao => Image(dao.id, dao.name, Some(dao.uri), dao.visibility)

  def fromImage: Image => ImageDAO = img => ImageDAO(img.id, img.name, img.uri.getOrElse(""), img.visibility)
}
