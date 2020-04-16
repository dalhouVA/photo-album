package authentication

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.{AuthenticationDirective, Credentials}
import core.User._
import repository.UserRepo


trait Authentication {

  def authenticate: AuthenticationDirective[User]


}

class Auth(repo: UserRepo) extends Authentication {
  private def authenticator(credentials: Credentials): Option[User] = credentials match {
    case p@Credentials.Provided(id) if p.verify(repo.getUserByName(id).map(_.pass).getOrElse("guest")) => repo.getUserByName(id).orElse(Some(Guest))
    case _ => Some(Guest)
  }

  def authenticate: AuthenticationDirective[User] =
    extractCredentials.map {
      case cred@Some(_) => cred.flatMap(_ => authenticator(Credentials(cred))).getOrElse(Guest)
      case None => Guest
    }
}
