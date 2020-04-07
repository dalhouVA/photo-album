package service

import java.util.UUID

import components.Image
import repository.ImageRepo

import scala.concurrent.{ExecutionContext, Future}

trait ImageService {

  def upload(img: Image): Future[Unit]

  def getImg(id: UUID): Future[Image]

  def getAllImg: Future[List[Image]]

  def delete(id: UUID): Future[Unit]

}

class DBImageService(repo: ImageRepo)(implicit ex: ExecutionContext) extends ImageService {
  override def upload(img: Image): Future[Unit] = repo.create(img)

  override def getImg(id: UUID): Future[Image] = repo.getByID(id)

  override def getAllImg: Future[List[Image]] = repo.getAll

  override def delete(id: UUID): Future[Unit] = repo.delete(id)
}
