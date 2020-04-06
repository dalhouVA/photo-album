package database

import java.util.UUID

import components.ImageDAO
import slick.jdbc.JdbcBackend.DatabaseDef
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DataBase(config: JdbcProfile, db: DatabaseDef) {

  import config.api._

  private class Images(tag: Tag) extends Table[ImageDAO](tag, "IMAGES") {
    def id = column[UUID]("ID", O.PrimaryKey)

    def innerID = column[UUID]("INNER_ID")

    def name = column[String]("NAME")

    def uri = column[String]("URI")

    def visibility = column[Boolean]("VISIBILITY")

    def * = (id, innerID, name, uri, visibility) <> ((ImageDAO.mapperTo _).tupled, ImageDAO.unapply)

  }

  private val images = TableQuery[Images]

  private val setup = DBIO.seq(images.schema.create)

  def init: Future[Unit] = db.run(setup)

  private def insert(img: ImageDAO) = DBIO.seq(images += img)

  private def delete(img: ImageDAO) = images.filter(_.id === img.id).delete

  def createNewImage(img: ImageDAO): Future[Unit] = db.run(insert(img))

  def getAllImage: Future[List[ImageDAO]] = db.run(images.result).map(_.toList)

  def getImageByID(id: UUID): Future[ImageDAO] = db.run(images.filter(_.innerID === id).result).map(_.headOption.getOrElse(ImageDAO.empty))

  def delete(id: UUID): Future[Unit] = for {
    img <- getImageByID(id)
  } yield db.run(delete(img))

}
