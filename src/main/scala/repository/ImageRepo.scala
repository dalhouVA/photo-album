package repository

import java.util.UUID

import components.Image
import dao.converters.ImageDAOConverter
import database.{DB, Tables}
import generator.Generator

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ImageRepo {
  def createImage(img: Image, path: Option[String]): Future[Option[UUID]]

  def getAllImages(): Future[List[Image]]

  def delete(id: UUID): Future[Unit]

  def getImageByID(imageID: UUID): Future[Option[Image]]

}

trait ImageRepoDB extends ImageRepo with Tables with DB {

  import config.api._

  val generator: Generator

  private def deleteImage(img: Image) = images.filter(_.id === img.id).delete

  override def createImage(img: Image, optPath: Option[String]): Future[Option[UUID]] = {
    optPath match {
      case None => Future.successful(None)
      case Some(path) =>
        val imgID = Some(generator.id)
        db.run(DBIO.seq(images += ImageDAOConverter.fromImage(img).copy(id = imgID, uri = path))).map(_ => imgID)
    }
  }

  override def getAllImages(): Future[List[Image]] = db.run(images.result).map(_.map(img => Image(img.id, img.name, Some(img.uri), img.visibility))).map(_.toList)

  override def delete(id: UUID): Future[Unit] = for {
    img <- getImageByID(id).map(_.getOrElse(Image.empty))
  } yield db.run(deleteImage(img))

  override def getImageByID(imageID: UUID): Future[Option[Image]] = db.run(images.filter(_.id === imageID).result).map(_.headOption).map(_.map(img => Image(img.id, img.name, Some(img.uri), img.visibility)))

}
