package services.album

import java.util.UUID

import components.{Album, Image}
import repository.AlbumRepo

import scala.concurrent.Future

trait AlbumService {
  def getAllAlbums(): Future[List[Album]]

  def getAlbumById(albumID: UUID): Future[Option[Album]]

  def createAlbum(album: Album): Future[UUID]

  def getImagesByAlbumId(albumID: UUID): Future[List[Image]]

  def getImage(albumID:UUID,imageID:UUID):Future[Option[Image]]

  def createImageFromAlbum(image: Image, albumID: UUID,base64String:String): Future[Option[UUID]]

  def putImageIntoAlbum(imageID: UUID, albumID: UUID): Future[Unit]

  def deleteAlbum(uuid: UUID): Future[Unit]

  def deleteImageFromAlbum(imageID: UUID, albumID: UUID): Future[Unit]
}

class DBAlbumService(repo: AlbumRepo) extends AlbumService {
  override def getAllAlbums(): Future[List[Album]] = repo.getAllAlbums()

  override def getAlbumById(albumID: UUID): Future[Option[Album]] = repo.getAlbumById(albumID)

  override def getImagesByAlbumId(albumID: UUID): Future[List[Image]] = repo.getImagesByAlbumID(albumID)

  override def getImage(albumID: UUID, imageID: UUID): Future[Option[Image]] = repo.getImage(imageID,albumID)

  override def createAlbum(album: Album): Future[UUID] = repo.createAlbum(album)

  override def createImageFromAlbum(image: Image, albumID: UUID,base64String:String): Future[Option[UUID]] = repo.createImageFromAlbum(image, albumID,base64String)

  override def putImageIntoAlbum(imageID: UUID, albumID: UUID): Future[Unit] = repo.putImageIntoAlbum(imageID, albumID)

  override def deleteAlbum(id: UUID): Future[Unit] = repo.deleteAlbum(id)

  override def deleteImageFromAlbum(imageID: UUID, albumID: UUID): Future[Unit] = repo.deleteImageFromAlbum(imageID, albumID)
}