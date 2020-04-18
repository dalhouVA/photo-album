package database

import slick.jdbc.JdbcBackend._
import slick.jdbc.{H2Profile, JdbcProfile}

trait DB {
  val config: JdbcProfile
  val db: DatabaseDef
}

trait H2DBFile extends DB {
  override lazy val config = H2Profile
  override lazy val db = Database.forConfig("h2file")
}

trait H2DBMem extends DB {
  override lazy val config = H2Profile
  override lazy val db = Database.forConfig("h2mem")
}
