package dao.converters

import core.Image
import dao.ImageDAO

object ImageDAOConverter {

  def toImage: ImageDAO => Image = dao => Image(dao.id, dao.name, None, Some(dao.uri), dao.visibility)

  def fromImage: Image => ImageDAO = img => ImageDAO(img.id, img.file.get.getName, img.uri.getOrElse(""), img.visibility)
}
