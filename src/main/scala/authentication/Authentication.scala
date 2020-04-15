package authentication

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.{AuthenticationDirective, Credentials}
import core.Role.UserRole
import core.User._
import core.{Role, User}


trait Authentication {

  def authenticate: AuthenticationDirective[User]

  protected def extractRole(name: String): UserRole =
    roles.getOrElse(name, Role.Guest)

  protected val roles: Map[String, UserRole] = Map(
    "justice" -> Role.User,
    "jane" -> Role.User
  )
}

class Auth extends Authentication {
  private def authenticator(credentials: Credentials): Option[User] = credentials match {
    case p@Credentials.Provided(id) if p.verify(User.listOfUsers.getOrElse(id, "guest")) => Some(RegisteredUser(id, User.listOfUsers(id), extractRole(id)))
    case _ => Some(Guest)
  }

  def authenticate: AuthenticationDirective[User] =
    extractCredentials.map {
      case cred@Some(_) => cred.flatMap(_ => authenticator(Credentials(cred))).getOrElse(Guest)
      case None => Guest
    }
}
