package core

import core.Role._

object User {
  sealed trait User {
    val login: String
    val pass: String
    val role: UserRole
  }

  case object Guest extends User {
    override val login: String = "guest"
    override val pass: String = "guest"
    override val role: UserRole = Role.Guest
  }

  case class RegisteredUser(login: String, pass: String, role: UserRole) extends User

  object RegisteredUser{
    def mapperTo(login: String, pass: String, role: UserRole): RegisteredUser = apply(login, pass, role)
  }

}
