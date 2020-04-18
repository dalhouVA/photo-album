package repository

import core.LoggedInUser
import database.Tables

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait UserRepo {

  def getUserByName(name: String): Future[LoggedInUser]

}

trait UserRepoDB extends UserRepo with Tables {

  import config.api._

  override def getUserByName(name: String): Future[LoggedInUser] = db.run(users.filter(_.name === name).result).map(_.headOption).map(_.getOrElse(LoggedInUser.guest))

}
