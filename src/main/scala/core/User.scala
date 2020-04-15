package core

import core.Role._

object User {
  val listOfUsers: Map[String, String] = Map(
    "justice" -> "ololo",
    "jane" -> "123"
  )


  trait User {
    val login: String
    val pass: String
    val role: UserRole
  }

  object Guest extends User {
    override val login: String = "guest"
    override val pass: String = "guest"
    override val role: UserRole = Role.Guest
  }

  case class RegisteredUser(login: String, pass: String, role: UserRole) extends User

}
