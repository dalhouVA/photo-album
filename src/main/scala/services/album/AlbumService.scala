package services.album

import java.util.UUID

import components.{Album, Image}
import repository.AlbumRepo

import scala.concurrent.Future

trait AlbumService {
  def getAllAlbums(): Future[List[Album]]

  def getAlbumById(albumID: UUID): Future[Option[Album]]

  def createAlbum(album: Album): Future[UUID]

  def putImageIntoAlbum(imageID: UUID, albumID: UUID): Future[Unit]

  def createImageFromAlbum(image: Image, albumID: UUID): Future[UUID]

  def deleteAlbum(uuid: UUID): Future[Unit]

  def getImagesByAlbumId(albumID: UUID): Future[List[Image]]

  def deleteImageFromAlbum(imageID: UUID, albumID: UUID): Future[Unit]
}

class DBAlbumService(repo: AlbumRepo) extends AlbumService {
  override def getAllAlbums(): Future[List[Album]] = repo.getAllAlbums()

  override def getAlbumById(albumID: UUID): Future[Option[Album]] = repo.getAlbumById(albumID)

  override def createAlbum(album: Album): Future[UUID] = repo.createAlbum(album)

  override def putImageIntoAlbum(imageID: UUID, albumID: UUID): Future[Unit] = repo.putImageIntoAlbum(imageID, albumID)

  override def createImageFromAlbum(image: Image, albumID: UUID): Future[UUID] = repo.createImageFromAlbum(image, albumID)

  override def deleteAlbum(id: UUID): Future[Unit] = repo.deleteAlbum(id)

  override def getImagesByAlbumId(albumID: UUID): Future[List[Image]] = repo.getImagesByAlbumID(albumID)

  override def deleteImageFromAlbum(imageID: UUID, albumID: UUID): Future[Unit] = repo.deleteImageFromAlbum(imageID, albumID)
}