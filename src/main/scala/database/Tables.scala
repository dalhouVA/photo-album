package database

import java.util.UUID

import core.Role.{Guest, User, UserRole}
import core.{Album, LoggedInUser}
import dao.ImageDAO
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType

trait Tables extends DB {

  import config.api._

  implicit val RoleColumnType: JdbcType[UserRole] with BaseTypedType[UserRole] = MappedColumnType.base[UserRole, String](
    {
      case User => "user"
      case Guest => "guest"
    }, {
      case "user" => User
      case "guest" => Guest
    }
  )


  class Images(tag: Tag) extends Table[ImageDAO](tag, "IMAGES") {
    def id = column[UUID]("ID", O.PrimaryKey)

    def name = column[String]("NAME")

    def uri = column[String]("URI")

    def visibility = column[Boolean]("VISIBILITY")


    def * = (id.?, name, uri, visibility) <> ((ImageDAO.apply _).tupled, ImageDAO.unapply)

  }

  val images = TableQuery[Images]

  class Users(tag: Tag) extends Table[LoggedInUser](tag, "USERS") {
    def name = column[String]("NAME", O.Unique)

    def pass = column[String]("PASSWORD")

    def role = column[UserRole]("ROLE")

    def * = (name, pass, role) <> ((LoggedInUser.apply _).tupled, LoggedInUser.unapply)
  }

  val users = TableQuery[Users]

  class Albums(tag: Tag) extends Table[Album](tag, "ALBUMS") {
    def id = column[UUID]("ID", O.PrimaryKey)

    def name = column[String]("NAME")

    def * = (id.?, name) <> (Album.tupled, Album.unapply)
  }

  val albums = TableQuery[Albums]

  case class ImageAlbum(image_id: UUID, album_id: UUID)

  class ImageAlbums(tag: Tag) extends Table[ImageAlbum](tag, "IMAGE_ALBUMS") {
    def image_id = column[UUID]("IMAGE_ID")

    def album_id = column[UUID]("ALBUM_ID")

    def UX_imageID_albumID = index("UX_image-albums_imageID_albumID", (image_id, album_id), unique = true)

    def FK_imageID = foreignKey("FK_image_albums_imageID", image_id, images)(_.id, onDelete = ForeignKeyAction.Cascade)

    def FK_albumID = foreignKey("FK_image_albums_albumID", album_id, albums)(_.id, onDelete = ForeignKeyAction.Cascade)

    def * = (image_id, album_id).mapTo[ImageAlbum]

  }

  val imageAlbums = TableQuery[ImageAlbums]
}
