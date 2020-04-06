package database

import java.util.UUID

import components.ImageDAO

import scala.concurrent.Future

trait ImageDb {

  def create(img: ImageDAO): Future[Unit]

  def getAllImages: Future[List[ImageDAO]]

  def getImageById(id: UUID): Future[ImageDAO]

  def deleteImage(id: UUID): Future[Unit]

}

object H2ImageDb extends ImageDb {

  override def create(img: ImageDAO): Future[Unit] = H2DataBase.createNewImage(img)

  override def getAllImages: Future[List[ImageDAO]] = H2DataBase.getAllImage

  override def getImageById(id: UUID): Future[ImageDAO] = H2DataBase.getImageByID(id)

  override def deleteImage(id: UUID): Future[Unit] = H2DataBase.delete(id)
}
