package authentication

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.Credentials
import core.LoggedInUser
import repository.UserRepo

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait Authentication {
  def authenticate: Directive1[LoggedInUser]
}

class Auth(repo: UserRepo) extends Authentication {
  private def authenticator(credentials: Credentials): Future[LoggedInUser] = credentials match {
    case p@Credentials.Provided(id) =>
      repo
        .getUserByName(id)
        .map(user => if (p.verify(user.pass)) user else LoggedInUser.guest)
    case _ => Future.successful(LoggedInUser.guest)
  }

  def authenticate: Directive1[LoggedInUser] =
    extractCredentials.flatMap { cred =>
      onSuccess(authenticator(Credentials(cred)))
    }
}
