package repository

import java.util.UUID

import scala.concurrent.Future
import core.Image

trait Repo {

  def create(img:Image): Future[Unit]

  def getAll: Future[List[Image]]

  def delete(id: UUID): Future[Unit]

  def getByID(id: UUID): Future[Image]
}

object PhotoRepo extends Repo {
  override def create(img: Image): Future[Unit] = ???

  override def getAll: Future[List[Image]] = ???

  override def delete(id: UUID): Future[Unit] = ???

  override def getByID(id: UUID): Future[Image] = ???
}