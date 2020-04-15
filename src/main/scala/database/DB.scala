package database

import java.util.UUID

import core.Role.{Guest, _}
import core.User._
import dao.ImageDAO
import slick.ast.BaseTypedType
import slick.jdbc.JdbcBackend._
import slick.jdbc.{H2Profile, JdbcProfile, JdbcType}

trait DB {
  val config: JdbcProfile
  val db: DatabaseDef

  import config.api._

  class Images(tag: Tag) extends Table[ImageDAO](tag, "IMAGES") {
    def id = column[UUID]("ID", O.PrimaryKey)

    def name = column[String]("NAME")

    def uri = column[String]("URI")

    def visibility = column[Boolean]("VISIBILITY")

    def * = (id.?, name, uri, visibility) <> ((ImageDAO.mapperTo _).tupled, ImageDAO.unapply)

  }

  val images = TableQuery[Images]
}

trait H2DBFile extends DB {
  override lazy val config = H2Profile
  override lazy val db = Database.forConfig("h2file")
}

trait H2DBMem extends DB {
  override lazy val config = H2Profile
  override lazy val db = Database.forConfig("h2mem")
}
