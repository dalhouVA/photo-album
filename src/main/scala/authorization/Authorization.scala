package authorization

import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives.{pass, reject}
import components.LoggedInUser
import components.Role.UserRole

trait Authorization {

  def authorize(user: LoggedInUser, requiredLevel: UserRole): Directive0
}

class BasicAuthorization extends Authorization {
  override def authorize(user: LoggedInUser, requiredLevel: UserRole): Directive0 =
    if (user.role == requiredLevel)
      pass
    else reject
}