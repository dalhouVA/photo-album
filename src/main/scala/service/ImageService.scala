package service

import java.util.UUID

import core.{Album, Image}
import repository.ImageRepo

import scala.concurrent.{ExecutionContext, Future}

trait ImageService {

  def upload(img: Image): Future[Unit]

  def getImgById(image_id: UUID): Future[Option[Image]]

  def getAllImg: Future[List[Image]]

  def delete(id: UUID): Future[Unit]

  def getPublicImages: Future[List[Image]]

  def getPublicImageById(id: UUID): Future[Option[Image]]

  def getAllAlbums:Future[List[Album]]

  def getAlbumById(album_id:UUID):Future[Option[Album]]

  def createAlbum(album: Album):Future[Unit]

  def putImageIntoAlbum(image_id: UUID, album_id: UUID):Future[Unit]

  def createImageFromAlbum(image: Image,album_id:UUID):Future[Unit]

  def deleteAlbum(uuid: UUID): Future[Unit]

  def getImagesByAlbumId(album_id: UUID): Future[List[Image]]
}

class DBImageService(repo: ImageRepo)(implicit ex: ExecutionContext) extends ImageService {
  override def upload(img: Image): Future[Unit] = repo.createImage(img)

  override def getImgById(image_id: UUID): Future[Option[Image]] = repo.getImageByID(image_id)

  override def getAllImg: Future[List[Image]] = repo.getAllImages

  override def getPublicImages: Future[List[Image]] = getAllImg.map(_.filter(_.visibility))

  override def delete(id: UUID): Future[Unit] = repo.delete(id)

  override def getPublicImageById(id: UUID): Future[Option[Image]] = getPublicImages.map(_.find(_.id.contains(id)))

  override def getAllAlbums: Future[List[Album]] = repo.getAllAlbums

  override def getAlbumById(album_id:UUID): Future[Option[Album]] = repo.getAlbumById(album_id)

  override def createAlbum(album: Album): Future[Unit] = repo.createAlbum(album)

  override def putImageIntoAlbum(image_id: UUID, album_id: UUID): Future[Unit] = repo.putImageIntoAlbum(image_id,album_id)

  override def createImageFromAlbum(image: Image, album_id: UUID): Future[Unit] = repo.createImageFromAlbum(image,album_id)

  override def deleteAlbum(id: UUID): Future[Unit] = repo.deleteAlbum(id)

  override def getImagesByAlbumId(album_id: UUID): Future[List[Image]] = repo.getImagesByAlbumID(album_id)
}
