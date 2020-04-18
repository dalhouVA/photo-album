package repository

import java.util.UUID

import core.Image
import dao.ImageDAO
import dao.converters.ImageDAOConverter
import database.Tables

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ImageRepo {

  def create(img: Image): Future[Unit]

  def getAll: Future[List[Image]]

  def delete(id: UUID): Future[Unit]

  def getByID(id: UUID): Future[Option[Image]]

}

trait ImageRepoDB extends ImageRepo with Tables {

  import config.api._

  private def insert(img: ImageDAO) = DBIO.seq(images += img)

  private def delete(img: Image) = images.filter(_.id === img.id).delete

  override def create(img: Image): Future[Unit] = db.run(insert(ImageDAOConverter.fromImage(img)))

  override def getAll: Future[List[Image]] = db.run(images.result).map(_.map(ImageDAOConverter.toImage)).map(_.toList)

  override def delete(id: UUID): Future[Unit] = for {
    img <- getByID(id).map(_.getOrElse(Image.empty))
  } yield db.run(delete(img))

  override def getByID(id: UUID): Future[Option[Image]] = db.run(images.filter(_.id === id).result).map(_.headOption).map(_.map(ImageDAOConverter.toImage))
}
