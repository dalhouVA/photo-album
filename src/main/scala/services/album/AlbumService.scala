package services.album

import java.util.UUID

import components.{Album, Image}
import repository.{AlbumRepo, PhotoRepo}

import scala.concurrent.{ExecutionContext, Future}

trait AlbumService {
  def getAllAlbums(): Future[List[Album]]

  def getAlbumById(albumID: UUID): Future[Option[Album]]

  def createAlbum(album: Album): Future[UUID]

  def getImagesByAlbumId(albumID: UUID): Future[List[Image]]

  def getImage(albumID: UUID, imageID: UUID): Future[Option[Image]]

  def createImageFromAlbum(image: Image, albumID: UUID, base64String: String): Future[Option[UUID]]

  def putImageIntoAlbum(imageID: UUID, albumID: UUID): Future[Unit]

  def deleteAlbum(uuid: UUID): Future[Unit]

  def deleteImageFromAlbum(imageID: UUID, albumID: UUID): Future[Unit]
}

class DBAlbumService(albumRepo: AlbumRepo, photoRepo: PhotoRepo)(implicit ec: ExecutionContext) extends AlbumService {
  override def getAllAlbums(): Future[List[Album]] = albumRepo.getAllAlbums()

  override def getAlbumById(albumID: UUID): Future[Option[Album]] = albumRepo.getAlbumById(albumID)

  override def getImagesByAlbumId(albumID: UUID): Future[List[Image]] = albumRepo.getImagesByAlbumID(albumID)

  override def getImage(albumID: UUID, imageID: UUID): Future[Option[Image]] = albumRepo.getImage(imageID, albumID)

  override def createAlbum(album: Album): Future[UUID] = albumRepo.createAlbum(album)

  override def createImageFromAlbum(image: Image, albumID: UUID, base64String: String): Future[Option[UUID]] =
    photoRepo.uploadImageInRepo(base64String).
      flatMap(path => albumRepo.createImageFromAlbum(image, albumID, path))

  override def putImageIntoAlbum(imageID: UUID, albumID: UUID): Future[Unit] = albumRepo.putImageIntoAlbum(imageID, albumID)

  override def deleteAlbum(id: UUID): Future[Unit] = albumRepo.deleteAlbum(id)

  override def deleteImageFromAlbum(imageID: UUID, albumID: UUID): Future[Unit] = albumRepo.deleteImageFromAlbum(imageID, albumID)
}