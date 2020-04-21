package repository

import java.util.UUID

import core.{Album, Image}
import dao.converters.ImageDAOConverter
import database.Tables

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ImageRepo {
  def createImage(img: Image): Future[Unit]

  def getAllImages: Future[List[Image]]

  def delete(id: UUID): Future[Unit]

  def getImageByID(image_id: UUID): Future[Option[Image]]

  def getAllAlbums: Future[List[Album]]

  def getAlbumById(album_id: UUID): Future[Option[Album]]

  def createAlbum(album: Album): Future[Unit]

  def putImageIntoAlbum(image_id: UUID, album_id: UUID): Future[Unit]

  def createImageFromAlbum(image: Image, album_id: UUID): Future[Unit]

  def deleteAlbum(id: UUID): Future[Unit]

  def getImagesByAlbumID(album_id: UUID): Future[List[Image]]
}

trait ImageRepoDB extends ImageRepo with Tables {

  import config.api._

  db.run(DBIO.seq((images.schema ++ albums.schema ++ users.schema ++ imageAlbums.schema).create))

  private def deleteImage(img: Image) = images.filter(_.id === img.id).delete

  private def deleteAlbum(album: Album) = albums.filter(_.id === album.id).delete

  override def createImage(img: Image): Future[Unit] = db.run(DBIO.seq(images += ImageDAOConverter.fromImage(img)))

  override def getAllImages: Future[List[Image]] = db.run(images.result).map(_.map(img => Image(img.id, img.name, Some(img.file), img.visibility))).map(_.toList)

  override def delete(id: UUID): Future[Unit] = for {
    img <- getImageByID(id).map(_.getOrElse(Image.empty))
  } yield db.run(deleteImage(img))

  override def getImageByID(image_id: UUID): Future[Option[Image]] = db.run(images.filter(_.id === image_id).result).map(_.headOption).map(_.map(img => Image(img.id, img.name, Some(img.file), img.visibility)))

  override def getAllAlbums: Future[List[Album]] = db.run(albums.result).map(_.toList)

  override def getAlbumById(album_id: UUID): Future[Option[Album]] = db.run(albums.filter(_.id === album_id).result).map(_.headOption)

  override def createAlbum(album: Album): Future[Unit] = db.run(DBIO.seq(albums += album))

  override def putImageIntoAlbum(image_id: UUID, album_id: UUID): Future[Unit] = db.run(DBIO.seq(imageAlbums += ImageAlbum(image_id, album_id)))

  override def createImageFromAlbum(image: Image, album_id: UUID): Future[Unit] =
    db.run(DBIO.seq(
      images += ImageDAOConverter.fromImage(image),
      imageAlbums += ImageAlbum(image.id.get, album_id)
    ))

  override def deleteAlbum(id: UUID): Future[Unit] = for {
    album <- getAlbumById(id).map(_.getOrElse(Album(None, "")))
  } yield db.run(deleteAlbum(album))

  override def getImagesByAlbumID(album_id: UUID): Future[List[Image]] =
    db.run(imageAlbums.filter(_.album_id === album_id).result).map(_.map(_.image_id))
      .flatMap { list_ids =>
        list_ids.foldLeft(Future.successful[List[Image]](Nil))((f, id) => f.flatMap(l => getImageByID(id).map(_.getOrElse(Image.empty) :: l)))
      }
}
