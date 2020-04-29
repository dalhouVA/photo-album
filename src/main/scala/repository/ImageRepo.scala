package repository

import java.io.{ByteArrayInputStream, File}
import java.util.{Base64, UUID}

import components.Image
import dao.converters.ImageDAOConverter
import database.{DB, Tables}
import generator.Generator
import javax.imageio.ImageIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait ImageRepo {
  def createImage(img: Image, base64String: String): Future[Option[UUID]]

  def getAllImages(): Future[List[Image]]

  def delete(id: UUID): Future[Unit]

  def getImageByID(imageID: UUID): Future[Option[Image]]

}

abstract class ImageRepoDB(generator: Generator) extends ImageRepo with Tables with DB with Repo {

  import config.api._

  override def uploadImageInRepo(base64String: String): Option[File] = Try {
    val base64 = base64String.split(",")
    val (data, img_string) = (base64.head, base64.last)

    def extension: String = {
      val idx = data.indexOf('/')
      val idx_ = data.indexOf(';')
      data.substring(idx + 1, idx_)
    }

    val byteArray = Base64.getDecoder.decode(img_string)
    val bis = new ByteArrayInputStream(byteArray)
    val file = new File(s"/img/${generator.id}.$extension")
    ImageIO.write(ImageIO.read(bis), s"$extension", file)
    file
  } match {
    case Failure(_) => None
    case Success(value) => Some(value)
  }

  private def deleteImage(img: Image) = images.filter(_.id === img.id).delete

  override def createImage(img: Image, base64String: String): Future[Option[UUID]] = {
    uploadImageInRepo(base64String) match {
      case None => Future.successful(None)
      case Some(file) =>
        val imgID = Some(generator.id)
        db.run(DBIO.seq(images += ImageDAOConverter.fromImage(img).copy(id = imgID, uri = file.getAbsolutePath))).map(_ => imgID)
    }

  }

  override def getAllImages(): Future[List[Image]] = db.run(images.result).map(_.map(img => Image(img.id, img.name, Some(img.uri), img.visibility))).map(_.toList)

  override def delete(id: UUID): Future[Unit] = for {
    img <- getImageByID(id).map(_.getOrElse(Image.empty))
  } yield db.run(deleteImage(img))

  override def getImageByID(imageID: UUID): Future[Option[Image]] = db.run(images.filter(_.id === imageID).result).map(_.headOption).map(_.map(img => Image(img.id, img.name, Some(img.uri), img.visibility)))

}
