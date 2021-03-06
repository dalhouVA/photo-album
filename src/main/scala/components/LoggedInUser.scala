package components

import components.Role._

case class LoggedInUser(login: String = "",
                        pass: String = "",
                        role: UserRole = Role.Guest)

object LoggedInUser {
  def guest: LoggedInUser = LoggedInUser()
}
