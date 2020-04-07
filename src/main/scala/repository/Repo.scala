package repository

import java.util.UUID

import components.{Image, ImageDAO}
import database.{DB, H2DB}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ImageRepo {

  def create(img: Image): Future[Unit]

  def getAll: Future[List[Image]]

  def delete(id: UUID): Future[Unit]

  def getByID(id: UUID): Future[Image]
}

trait DBImageRepo extends ImageRepo with DB{

  import config.api._

  private def insert(img: ImageDAO) = DBIO.seq(images += img)

  private def delete(img: Image) = images.filter(_.innerID === img.id).delete

  override def create(img: Image): Future[Unit] = db.run(insert(img.toDAO))

  override def getAll: Future[List[Image]] = db.run(images.result).map(_.map(_.convert)).map(_.toList)

  override def delete(id: UUID): Future[Unit] = for {
    img <- getByID(id)
  } yield db.run(delete(img))

  override def getByID(id: UUID): Future[Image] = db.run(images.filter(_.innerID === id).result).map(_.headOption.getOrElse(ImageDAO.empty)).map(_.convert)
}
