package dao.converters

import core.Image
import dao.ImageDAO

object ImageDAOConverter {

  def toImage: ImageDAO => Image = dao => Image(dao.id, dao.name, Some(dao.file), dao.visibility, dao.albums)

  def fromImage: Image => ImageDAO = img => ImageDAO(img.id, img.name, img.uri.getOrElse(""), img.visibility, img.albums)
}
