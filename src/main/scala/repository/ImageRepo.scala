package repository

import java.util.UUID

import components.Image
import database.H2ImageDb

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ImageRepo {

  def create(img: Image): Future[Unit]

  def getAll: Future[List[Image]]

  def delete(id: UUID): Future[Unit]

  def getByID(id: UUID): Future[Image]
}

object H2ImageRepo extends ImageRepo {
  override def create(img: Image): Future[Unit] = H2ImageDb.create(img.toDAO)

  override def getAll: Future[List[Image]] = H2ImageDb.getAllImages.map(_.map(_.convert))

  override def delete(id: UUID): Future[Unit] = H2ImageDb.deleteImage(id)

  override def getByID(id: UUID): Future[Image] = H2ImageDb.getImageById(id).map(_.convert)
}
