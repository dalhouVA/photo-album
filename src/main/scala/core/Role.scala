package core

object Role {

  sealed trait UserRole

  case object User extends UserRole

  case object Guest extends UserRole

}
