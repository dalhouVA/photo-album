package service


import java.util.UUID

import components.Image
import repository.H2ImageRepo

import scala.concurrent.Future

trait ImageService {

  def upload(img: Image): Future[Unit]

  def getImg(id: UUID): Future[Image]

  def getAllImg: Future[List[Image]]

  def delete(id: UUID): Future[Unit]

}

object H2ImageService extends ImageService {
  override def upload(img: Image): Future[Unit] = H2ImageRepo.create(img)

  override def getImg(id: UUID): Future[Image] = H2ImageRepo.getByID(id)

  override def getAllImg: Future[List[Image]] = H2ImageRepo.getAll

  override def delete(id: UUID): Future[Unit] = H2ImageRepo.delete(id)
}
