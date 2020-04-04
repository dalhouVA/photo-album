package service


import java.util.UUID

import repository.Repo
import core.Image

import scala.concurrent.{ExecutionContext, Future}

trait Service {

  def upload(img: Image): Future[Unit]

  def getImg(id: UUID): Future[Image]

  def getAllImg: Future[List[Image]]

  def delete(id: UUID): Future[Unit]

}

case class PhotoService(repo: Repo)(implicit val ex: ExecutionContext) extends Service {

  override def upload(img: Image): Future[Unit] = repo.create(img)

  override def getImg(id: UUID): Future[Image] = repo.getByID(id)
                                                      .map(img =>
                                                        if (img.visibility) img
                                                        else Image.empty)

  override def getAllImg: Future[List[Image]] = repo.getAll

  override def delete(id: UUID): Future[Unit] = repo.delete(id)
}
