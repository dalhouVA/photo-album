package database

import java.util.UUID

import components.Role.{Guest, User, UserRole}
import components.{Album, LoggedInUser}
import dao.ImageDAO
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType

trait Tables {

  this: DB =>

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
    def id: Rep[UUID] = column[UUID]("ID", O.PrimaryKey)

    def name: Rep[String] = column[String]("NAME")

    def uri: Rep[String] = column[String]("URI")

    def visibility: Rep[Boolean] = column[Boolean]("VISIBILITY")


    def * = (id.?, name, uri, visibility) <> ((ImageDAO.apply _).tupled, ImageDAO.unapply)

  }

  val images = TableQuery[Images]

  class Users(tag: Tag) extends Table[LoggedInUser](tag, "USERS") {
    def name: Rep[String] = column[String]("NAME", O.Unique)

    def pass: Rep[String] = column[String]("PASSWORD")

    def role: Rep[UserRole] = column[UserRole]("ROLE")

    def * = (name, pass, role) <> ((LoggedInUser.apply _).tupled, LoggedInUser.unapply)
  }

  val users = TableQuery[Users]

  class Albums(tag: Tag) extends Table[Album](tag, "ALBUMS") {
    def id: Rep[UUID] = column[UUID]("ID", O.PrimaryKey)

    def name: Rep[String] = column[String]("NAME")

    def * = (id.?, name) <> (Album.tupled, Album.unapply)
  }

  val albums = TableQuery[Albums]

  case class ImageAlbum(image_id: UUID, album_id: UUID)

  class ImageAlbums(tag: Tag) extends Table[ImageAlbum](tag, "IMAGE_ALBUMS") {
    def image_id: Rep[UUID] = column[UUID]("IMAGE_ID")

    def album_id: Rep[UUID] = column[UUID]("ALBUM_ID")

    def UX_imageID_albumID = index("UX_image-albums_imageID_albumID", (image_id, album_id), unique = true)

    def FK_imageID = foreignKey("FK_image_albums_imageID", image_id, images)(_.id, onDelete = ForeignKeyAction.Cascade)

    def FK_albumID = foreignKey("FK_image_albums_albumID", album_id, albums)(_.id, onDelete = ForeignKeyAction.Cascade)

    def * = (image_id, album_id).mapTo[ImageAlbum]

  }

  val imageAlbums = TableQuery[ImageAlbums]
}
