package services.image

import java.io.File
import java.util.UUID

import components.Image
import repository.{ImageRepo, LocalRepo}

import scala.concurrent.{ExecutionContext, Future}

trait ImageService {
  def upload(img: Image): Future[UUID]

  def getImgById(imageID: UUID): Future[Option[Image]]

  def getAllImg(): Future[List[Image]]

  def delete(id: UUID): Future[Unit]

  def getPublicImages(): Future[List[Image]]

  def getPublicImageById(id: UUID): Future[Option[Image]]

  def uploadImageInStorage(base64String: String): Option[File]
}

class DBImageService(repo: ImageRepo, storage: LocalRepo)(implicit ex: ExecutionContext) extends ImageService {
  override def upload(img: Image): Future[UUID] = repo.createImage(img)

  override def getImgById(imageID: UUID): Future[Option[Image]] = repo.getImageByID(imageID)

  override def getAllImg(): Future[List[Image]] = repo.getAllImages()

  override def getPublicImages(): Future[List[Image]] = getAllImg().map(_.filter(_.visibility))

  override def delete(id: UUID): Future[Unit] = repo.delete(id)

  override def getPublicImageById(id: UUID): Future[Option[Image]] = getPublicImages().map(_.find(_.id.contains(id)))

  override def uploadImageInStorage(base64String: String): Option[File] = storage.upload(base64String)
}
