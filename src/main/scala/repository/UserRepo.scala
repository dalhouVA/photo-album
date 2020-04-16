package repository

import core.User.{RegisteredUser, User}
import database.{DB, H2DBFile}

trait UserRepo {

  def getUserByName(name: String): Option[User]

}

trait UserRepoDb extends UserRepo with DB {

  override def getUserByName(name: String): Option[User] = users.get(name).map(cred => RegisteredUser(name, cred._1, cred._2))

}
