package repository

import java.util.UUID

import components.{Album, Image}
import dao.converters.ImageDAOConverter
import database.{DB, Tables}
import generator.Generator

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait AlbumRepo {
  def getAllAlbums(): Future[List[Album]]

  def getAlbumById(albumID: UUID): Future[Option[Album]]

  def getImagesByAlbumID(albumID: UUID): Future[List[Image]]

  def getImage(imageID: UUID, albumID: UUID): Future[Option[Image]]

  def createAlbum(album: Album): Future[UUID]

  def createImageFromAlbum(image: Image, albumID: UUID, path: Option[String]): Future[Option[UUID]]

  def putImageIntoAlbum(imageID: UUID, albumID: UUID): Future[Unit]

  def deleteAlbum(id: UUID): Future[Unit]

  def deleteImageFromAlbum(imageID: UUID, albumID: UUID): Future[Unit]
}

trait AlbumRepoDB extends AlbumRepo with Tables with DB {

  import config.api._

  val generator: Generator

  private def deleteAlbum(album: Album) = albums.filter(_.id === album.id).delete

  private def getImageByID(imageID: UUID): Future[Option[Image]] = db.run(images.filter(_.id === imageID).result).map(_.headOption).map(_.map(img => Image(img.id, img.name, Some(img.uri), img.visibility)))

  override def getAllAlbums(): Future[List[Album]] = db.run(albums.result).map(_.toList)

  override def getAlbumById(albumID: UUID): Future[Option[Album]] = db.run(albums.filter(_.id === albumID).result).map(_.headOption)

  override def createAlbum(album: Album): Future[UUID] = {
    val album_id = generator.id
    db.run(DBIO.seq(albums += album.copy(id = Some(album_id)))).map(_ => album_id)
  }

  override def putImageIntoAlbum(imageID: UUID, albumID: UUID): Future[Unit] = db.run(DBIO.seq(imageAlbums += ImageAlbum(imageID, albumID)))

  override def createImageFromAlbum(image: Image, albumID: UUID, optPath: Option[String]): Future[Option[UUID]] = {
    optPath match {
      case None => Future.successful(None)
      case Some(path) =>
        val imgID = Some(generator.id)
        db.run(DBIO.seq(
          images += ImageDAOConverter.fromImage(image).copy(id = imgID, uri = path),
          imageAlbums += ImageAlbum(image.id.get, albumID)
        )).map(_ => imgID)
    }
  }

  override def deleteAlbum(id: UUID): Future[Unit] = for {
    album <- getAlbumById(id).map(_.getOrElse(Album(None, "")))
  } yield db.run(deleteAlbum(album))

  override def getImagesByAlbumID(albumID: UUID): Future[List[Image]] =
    db.run(imageAlbums.filter(_.album_id === albumID).result).map(_.map(_.image_id))
      .flatMap { list_ids =>
        list_ids.foldLeft(Future.successful[List[Image]](Nil))((f, id) => f.flatMap(l => getImageByID(id).map(_.getOrElse(Image.empty) :: l)))
      }

  override def deleteImageFromAlbum(imageID: UUID, albumID: UUID): Future[Unit] = db.run(DBIO.seq(imageAlbums.filter(ia => ia.album_id === albumID && ia.image_id === imageID).delete))

  override def getImage(imageID: UUID, albumID: UUID): Future[Option[Image]] = getImagesByAlbumID(albumID).map(_.find(_.id.contains(imageID)))
}
