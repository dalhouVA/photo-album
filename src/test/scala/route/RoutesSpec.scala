package route

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{BasicHttpCredentials, HttpCredentials}
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.AuthenticationDirective
import akka.http.scaladsl.testkit.ScalatestRouteTest
import authentication.Authentication
import authorization.{Authorization, BasicAuthorization}
import components.{Album, Image, LoggedInUser, Role}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import dto.{InImageDTO, OutImageDTO}
import io.circe.generic.auto._
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import services.album.AlbumService
import services.image.ImageService

import scala.concurrent.Future

class RoutesSpec extends AnyFreeSpecLike with Matchers with ScalatestRouteTest {
  val publicImageID: UUID = UUID.randomUUID()
  val privateImageID: UUID = UUID.randomUUID()
  val nonExistentImageID: UUID = UUID.randomUUID()
  val albumID: UUID = UUID.randomUUID()
  val nonExistentAlbumID: UUID = UUID.randomUUID()

  sealed trait Common {
    val publicImage: Image = Image(Some(publicImageID), "cat.jpg", Some("D:\\img\\pet"), visibility = true)
    val privateImage: Image = Image(Some(privateImageID), "dog.jpg", Some("D:\\img\\pet"), visibility = false)
    val inImageWithCorrectBase64String: InImageDTO = InImageDTO("cat", visibility = true, base64Image = "base64String")
    val inImageWithInvalidBase64String: InImageDTO = InImageDTO("duck", visibility = false, base64Image = "")
    val listImages: List[Image] = List(publicImage, privateImage)
    val publicOutImage: OutImageDTO = OutImageDTO(publicImageID, "cat.jpg", "D:\\img\\pet", visibility = true)
    val privateOutImage: OutImageDTO = OutImageDTO(privateImageID, "dog.jpg", "D:\\img\\pet", visibility = false)
    val listImagesDTO: List[OutImageDTO] = List(publicOutImage, privateOutImage)
    val listImagesDTOPublic: List[OutImageDTO] = listImagesDTO.filter(_.visibility)
    val album: Album = Album(Some(albumID), "Pets")
    val nonExistentAlbum: Album = Album(Some(nonExistentAlbumID), "Waters")
    val listAlbums = List(album)
    val ok: StatusCodes.Success = StatusCodes.OK
    val created: StatusCodes.Success = StatusCodes.Created
    val bad: StatusCodes.ClientError = StatusCodes.BadRequest
    val unauthorized: StatusCodes.ClientError = StatusCodes.Unauthorized
  }

  sealed trait RoutesSpecContext extends Routes with Common {
    val validAuthenticate: BasicHttpCredentials = BasicHttpCredentials("jane", "123")
    override val authorization: Authorization = new BasicAuthorization
    override val auth: Authentication = new MockAuthenticate(Some(validAuthenticate))
    override val imageService: ImageService = new MockImageService(listImages, listAlbums)
    override val albumService: AlbumService = new MockAlbumService(listImages, listAlbums)
  }

  sealed trait GuestRoutesContext extends Routes with Common {
    override val auth: Authentication = new MockAuthenticate(None)
    override val authorization: Authorization = new BasicAuthorization
    override val imageService: ImageService = new MockImageService(listImages, listAlbums)
    override val albumService: AlbumService = new MockAlbumService(listImages, listAlbums)
  }

  "Routes" - {
    "ImageRoutes should" - {
      "Authorized user" - {
        "return list of all images" in new RoutesSpecContext {
          Get("/images") ~> routes ~> check {
            status shouldBe ok
            responseAs[List[OutImageDTO]] shouldBe listImagesDTO
          }
        }

        "return private image by id with authorized access" in new RoutesSpecContext {
          Get(s"/images/$privateImageID") ~> routes ~> check {
            status shouldBe ok
            responseAs[Option[OutImageDTO]] shouldBe Some(privateOutImage)
          }
        }

        "return public image by id with authorized access" in new RoutesSpecContext {
          Get(s"/images/$publicImageID") ~> routes ~> check {
            status shouldBe ok
            responseAs[Option[OutImageDTO]] shouldBe Some(publicOutImage)
          }
        }

        "return bad request error" in new RoutesSpecContext {
          Get(s"/images/$nonExistentImageID") ~> routes ~> check {
            status shouldBe bad
            responseAs[String] shouldBe "Image with this id not found"
          }
        }

        "create new image" in new RoutesSpecContext {
          Post("/images/upload", content = inImageWithCorrectBase64String) ~> routes ~> check {
            status shouldBe created
            responseAs[Option[OutImageDTO]] shouldBe Some(publicOutImage)
          }
        }

        "return bad request when base64 string is invalid" in new RoutesSpecContext {
          Post("/images/upload", content = inImageWithInvalidBase64String) ~> routes ~> check {
            status shouldBe bad
            responseAs[String] shouldBe "Invalid image"
          }
        }

        "delete image by id" in new RoutesSpecContext {
          Delete(s"/images/$publicImageID") ~> routes ~> check {
            status shouldBe ok
          }
        }
      }
      "Guest" - {
        "return list of public images" in new GuestRoutesContext {
          Get("/images") ~> routes ~> check {
            status shouldBe ok
            responseAs[List[OutImageDTO]] shouldBe listImagesDTOPublic
          }
        }

        "return public image by id with guest access" in new GuestRoutesContext {
          Get(s"/images/$publicImageID") ~> routes ~> check {
            status shouldBe ok
            responseAs[Option[OutImageDTO]] shouldBe Some(publicOutImage)
          }
        }

        "return unauthorized error with guest access to private image" in new GuestRoutesContext {
          Get(s"/images/$privateImageID") ~> routes ~> check {
            status shouldBe unauthorized
            responseAs[String] shouldBe "You have to authorize for get access"
          }
        }

        "return unauthorized error when pass invalid id" in new GuestRoutesContext {
          Get(s"/images/$nonExistentImageID") ~> routes ~> check {
            status shouldBe unauthorized
            responseAs[String] shouldBe "You have to authorize for get access"
          }
        }
      }
    }

    "AlbumRoutes should" - {
      "return list of all albums" in new RoutesSpecContext {
        Get("/albums") ~> routes ~> check {
          status shouldBe ok
          responseAs[List[Album]] shouldBe listAlbums
        }
      }

      "return album by id" in new RoutesSpecContext {
        Get(s"/albums/$albumID") ~> routes ~> check {
          status shouldBe ok
          responseAs[Album] shouldBe album
        }
      }

      "return error when pass invalid id" in new RoutesSpecContext {
        Get(s"/albums/$nonExistentAlbumID") ~> routes ~> check {
          status shouldBe bad
          responseAs[String] shouldBe "Album with this id not found"
        }
      }

      "return images in album" in new RoutesSpecContext {
        Get(s"/albums/$albumID/images") ~> routes ~> check {
          status shouldBe ok
          responseAs[List[OutImageDTO]] shouldBe listImagesDTO
        }
      }

      "create new album" in new RoutesSpecContext {
        Post("/albums/create", content = album) ~> routes ~> check {
          status shouldBe created
          responseAs[Option[Album]] shouldBe Some(album)
        }
      }

      "create image in album" in new RoutesSpecContext {
        Post(s"/albums/$albumID/images/upload", content = inImageWithCorrectBase64String) ~> routes ~> check {
          status shouldBe created
          responseAs[Option[OutImageDTO]] shouldBe Some(publicOutImage)
        }
      }

      "return bad request when base64string is invalid" in new RoutesSpecContext {
        Post(s"/albums/$albumID/images/upload", content = inImageWithInvalidBase64String) ~> routes ~> check {
          status shouldBe bad
          responseAs[String] shouldBe "Invalid image"
        }
      }

      "insert image into album" in new RoutesSpecContext {
        Put(s"/albums/$albumID/images/$publicImageID") ~> routes ~> check {
          status shouldBe created
        }
      }

      "delete album" in new RoutesSpecContext {
        Delete(s"/albums/$albumID") ~> routes ~> check {
          status shouldBe ok
        }
      }

      "delete image in current album" in new RoutesSpecContext {
        Delete(s"/albums/$albumID/images/$privateImageID") ~> routes ~> check {
          status shouldBe ok
        }
      }
    }
  }

  class MockImageService(images: List[Image], albums: List[Album]) extends ImageService {
    override def upload(img: Image, base64String: String): Future[Option[UUID]] =
      if (base64String.isEmpty)
        Future.successful(None)
      else
        Future.successful(Some(publicImageID))

    override def getImgById(imageID: UUID): Future[Option[Image]] = Future.successful(images.find(_.id.contains(imageID)))

    override def getAllImg(): Future[List[Image]] = Future.successful(images)

    override def delete(id: UUID): Future[Unit] = Future.unit

    override def getPublicImages(): Future[List[Image]] = Future.successful(images.filter(_.visibility))

    override def getPublicImageById(id: UUID): Future[Option[Image]] = getPublicImages().map(_.find(_.id.contains(id)))

  }

  class MockAlbumService(images: List[Image], albums: List[Album]) extends AlbumService {
    override def getAllAlbums(): Future[List[Album]] = Future.successful(albums)

    override def getAlbumById(albumID: UUID): Future[Option[Album]] = Future.successful(albums.find(_.id.contains(albumID)))

    override def createAlbum(album: Album): Future[UUID] = Future.successful(albumID)

    override def putImageIntoAlbum(imageID: UUID, albumID: UUID): Future[Unit] = Future.unit

    override def createImageFromAlbum(image: Image, albumID: UUID, base64String: String): Future[Option[UUID]] =
      if (base64String.isEmpty)
        Future.successful(None)
      else
        Future.successful(Some(publicImageID))

    override def deleteAlbum(uuid: UUID): Future[Unit] = Future.unit

    override def getImagesByAlbumId(albumID: UUID): Future[List[Image]] = Future.successful(images)

    override def deleteImageFromAlbum(imageID: UUID, albumID: UUID): Future[Unit] = Future.unit

    override def getImage(albumID: UUID, imageID: UUID): Future[Option[Image]] = Future.successful(images.find(_.id.contains(imageID)))
  }

  class MockAuthenticate(credentials: Option[HttpCredentials]) extends Authentication {
    override def authenticate: Directive1[LoggedInUser] = {
      credentials match {
        case Some(_) => AuthenticationDirective(provide(LoggedInUser("user", "pass", Role.User)))
        case None => AuthenticationDirective(provide(LoggedInUser.guest))
      }
    }
  }

}
