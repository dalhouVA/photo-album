package repository

import java.util.UUID

import components.Image
import dao.converters.ImageDAOConverter
import database.{DB, Tables}
import generator.Generator

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ImageRepo {
  def createImage(img: Image): Future[UUID]

  def getAllImages(): Future[List[Image]]

  def delete(id: UUID): Future[Unit]

  def getImageByID(imageID: UUID): Future[Option[Image]]

}

abstract class ImageRepoDB(generator: Generator) extends ImageRepo with Tables with DB {

  import config.api._

  private def deleteImage(img: Image) = images.filter(_.id === img.id).delete

  override def createImage(img: Image): Future[UUID] = {
    val img_id = generator.id
    db.run(DBIO.seq(images += ImageDAOConverter.fromImage(img).copy(id = Some(img_id)))).map(_ => img_id)
  }

  override def getAllImages(): Future[List[Image]] = db.run(images.result).map(_.map(img => Image(img.id, img.name, Some(img.file), img.visibility))).map(_.toList)

  override def delete(id: UUID): Future[Unit] = for {
    img <- getImageByID(id).map(_.getOrElse(Image.empty))
  } yield db.run(deleteImage(img))

  override def getImageByID(imageID: UUID): Future[Option[Image]] = db.run(images.filter(_.id === imageID).result).map(_.headOption).map(_.map(img => Image(img.id, img.name, Some(img.file), img.visibility)))

}
