package database

import java.util.UUID

import slick.jdbc.JdbcBackend._
import components.ImageDAO
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

trait ImageDb {

  val config: JdbcProfile
  val db: DatabaseDef

  def create(img: ImageDAO): Future[Unit]

  def getAllImages: Future[List[ImageDAO]]

  def getImageById(id: UUID): Future[ImageDAO]

  def deleteImage(id: UUID): Future[Unit]

}

object H2ImageDb extends ImageDb {

  val config: JdbcProfile = slick.jdbc.H2Profile

  val db: DatabaseDef = Database.forConfig("h2file")

  override def create(img: ImageDAO): Future[Unit] = new DataBase(config, db).createNewImage(img)

  override def getAllImages: Future[List[ImageDAO]] = new DataBase(config, db).getAllImage

  override def getImageById(id: UUID): Future[ImageDAO] = new DataBase(config, db).getImageByID(id)

  override def deleteImage(id: UUID): Future[Unit] = new DataBase(config, db).delete(id)
}
