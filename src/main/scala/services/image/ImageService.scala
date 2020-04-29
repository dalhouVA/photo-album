package services.image

import java.util.UUID

import components.Image
import repository.{ImageRepo, PhotoRepo}

import scala.concurrent.{ExecutionContext, Future}

trait ImageService {
  def upload(img: Image, base64String: String): Future[Option[UUID]]

  def getImgById(imageID: UUID): Future[Option[Image]]

  def getAllImg(): Future[List[Image]]

  def delete(id: UUID): Future[Unit]

  def getPublicImages(): Future[List[Image]]

  def getPublicImageById(id: UUID): Future[Option[Image]]

}

class DBImageService(imageRepo: ImageRepo, photoRepo: PhotoRepo)(implicit ex: ExecutionContext) extends ImageService {
  override def upload(img: Image, base64String: String): Future[Option[UUID]] =
    photoRepo.uploadImageInRepo(base64String).
      flatMap(path => imageRepo.createImage(img, path))

  override def getImgById(imageID: UUID): Future[Option[Image]] = imageRepo.getImageByID(imageID)

  override def getAllImg(): Future[List[Image]] = imageRepo.getAllImages()

  override def getPublicImages(): Future[List[Image]] = getAllImg().map(_.filter(_.visibility))

  override def delete(id: UUID): Future[Unit] = imageRepo.delete(id)

  override def getPublicImageById(id: UUID): Future[Option[Image]] = getPublicImages().map(_.find(_.id.contains(id)))

}
