package service

import java.util.UUID

import core.Image
import repository.ImageRepo

import scala.concurrent.{ExecutionContext, Future}

trait ImageService {

  def upload(img: Image): Future[Unit]

  def getImgById(id: UUID): Future[Option[Image]]

  def getAllImg: Future[List[Image]]

  def delete(id: UUID): Future[Unit]

  def getPublicImages: Future[List[Image]]

  def getPublicImageById(id: UUID): Future[Option[Image]]

}

class DBImageService(repo: ImageRepo)(implicit ex: ExecutionContext) extends ImageService {
  override def upload(img: Image): Future[Unit] = repo.create(img)

  override def getImgById(id: UUID): Future[Option[Image]] = repo.getByID(id)

  override def getAllImg: Future[List[Image]] = repo.getAll

  override def getPublicImages: Future[List[Image]] = getAllImg.map(_.filter(_.visibility))

  override def delete(id: UUID): Future[Unit] = repo.delete(id)

  override def getPublicImageById(id: UUID): Future[Option[Image]] = getPublicImages.map(_.find(_.id.contains(id)))
}
