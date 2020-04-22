package route

import java.io.File
import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{BasicHttpCredentials, HttpCredentials}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.AuthenticationDirective
import akka.http.scaladsl.server.{Directive1, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import authentication.Authentication
import core.{Album, Image, LoggedInUser, Role}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import dto.{InImageDTO, OutImageDTO}
import io.circe.generic.auto._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import service.ImageService
import store.Store

import scala.concurrent.Future

class RoutesSpec() extends AnyWordSpec with Matchers with ScalatestRouteTest {
  val catID: UUID = UUID.randomUUID()
  val dogID: UUID = UUID.randomUUID()
  val pigID: UUID = UUID.randomUUID()
  val birdsID: UUID = UUID.randomUUID()
  val petsID: UUID = UUID.randomUUID()
  val watersID: UUID = UUID.randomUUID()

  sealed trait RoutesSpecContext extends Routes {
    val cat: Image = Image(Some(catID), "cat.jpg", Some("D:\\img\\pet"), visibility = true)
    val dog: Image = Image(Some(dogID), "dog.jpg", Some("D:\\img\\pet"), visibility = false)
    val catInDTO: InImageDTO = InImageDTO("cat", "asdqwr", visibility = true)
    val listImages: List[Image] = List(cat, dog)
    val catOut: OutImageDTO = OutImageDTO(catID, "cat.jpg", "D:\\img\\pet", visibility = true)
    val dogOut: OutImageDTO = OutImageDTO(dogID, "dog.jpg", "D:\\img\\pet", visibility = false)
    val listImagesDTO: List[OutImageDTO] = List(catOut, dogOut)
    val listImagesDTOPublic: List[OutImageDTO] = listImagesDTO.filter(_.visibility)
    val validAuthenticate: BasicHttpCredentials = BasicHttpCredentials("jane", "123")
    val birdsAlbum: Album = Album(Some(birdsID), "Birds")
    val petsAlbum: Album = Album(Some(petsID), "Pets")
    val watersAlbum: Album = Album(Some(watersID), "Waters")
    val listAlbums = List(birdsAlbum, petsAlbum)
    val route: Route = routes(new MockImageService(listImages, listAlbums), new MockAuthenticate(Some(validAuthenticate)), new MockStore())
    val guestRoute: Route = routes(new MockImageService(listImages, listAlbums), new MockAuthenticate(None), new MockStore())
    val ok: StatusCodes.Success = StatusCodes.OK
    val created: StatusCodes.Success = StatusCodes.Created
    val bad: StatusCodes.ClientError = StatusCodes.BadRequest
    val unauthorized: StatusCodes.ClientError = StatusCodes.Unauthorized
  }

  "The server" should {
    "return list of all images" in new RoutesSpecContext {
      Get("/images") ~> route ~> check {
        status shouldBe ok
        responseAs[List[OutImageDTO]] shouldBe listImagesDTO
      }
    }

    "return list of images with visibility = true" in new RoutesSpecContext {
      Get("/images") ~> guestRoute ~> check {
        status shouldBe ok
        responseAs[List[OutImageDTO]] shouldBe listImagesDTOPublic
      }
    }

    "return image by id with valid credentials when visibility = false" in new RoutesSpecContext {
      Get(s"/images/$dogID") ~> route ~> check {
        status shouldBe ok
        responseAs[Option[OutImageDTO]] shouldBe Some(dogOut)
      }
    }

    "return image by id with valid credentials when visibility = true" in new RoutesSpecContext {
      Get(s"/images/$catID") ~> route ~> check {
        status shouldBe ok
        responseAs[Option[OutImageDTO]] shouldBe Some(catOut)
      }
    }

    "return image by id without credentials when visibility = true" in new RoutesSpecContext {
      Get(s"/images/$catID") ~> guestRoute ~> check {
        status shouldBe ok
        responseAs[Option[OutImageDTO]] shouldBe Some(catOut)
      }
    }

    "return unauthorized error" in new RoutesSpecContext {
      Get(s"/images/$dogID") ~> guestRoute ~> check {
        status shouldBe unauthorized
        responseAs[String] shouldBe "You have to authorize for get access"
      }
    }

    "return unauthorized error when pass invalid id" in new RoutesSpecContext {
      Get(s"/images/$pigID") ~> guestRoute ~> check {
        status shouldBe unauthorized
        responseAs[String] shouldBe "You have to authorize for get access"
      }
    }

    "return bad request error" in new RoutesSpecContext {
      Get(s"/images/$pigID") ~> route ~> check {
        status shouldBe bad
        responseAs[String] shouldBe "Image with this id not found"
      }
    }

    "delete image by id" in new RoutesSpecContext {
      Delete(s"/images/$catID") ~> route ~> check {
        status shouldBe ok
      }
    }

    "create new image" in new RoutesSpecContext {
      Post("/images/upload", content = catInDTO) ~> route ~> check {
        status shouldBe created
        responseAs[Option[OutImageDTO]] shouldBe Some(catOut)
      }
    }

    "return list of all albums" in new RoutesSpecContext {
      Get("/albums") ~> route ~> check {
        status shouldBe ok
        responseAs[List[Album]] shouldBe listAlbums
      }
    }

    "return album by id" in new RoutesSpecContext {
      Get(s"/albums/$birdsID") ~> route ~> check {
        status shouldBe ok
        responseAs[Album] shouldBe birdsAlbum
      }
    }

    "return error when pass invalid id" in new RoutesSpecContext {
      Get(s"/albums/$watersID") ~> route ~> check {
        status shouldBe bad
        responseAs[String] shouldBe "Album with this id not found"
      }
    }

    "return images in album" in new RoutesSpecContext {
      Get(s"/albums/$petsID/images") ~> route ~> check {
        status shouldBe ok
        responseAs[List[OutImageDTO]] shouldBe listImagesDTO
      }
    }

    "create new album" in new RoutesSpecContext {
      Post("/albums/create", content = petsAlbum) ~> route ~> check {
        status shouldBe created
        responseAs[Option[Album]] shouldBe Some(petsAlbum)
      }
    }

    "create image in album" in new RoutesSpecContext {
      Post(s"/albums/$petsID/images/upload", content = catInDTO) ~> route ~> check {
        status shouldBe created
      }
    }

    "insert image into album" in new RoutesSpecContext {
      Put(s"/albums/$petsID/images/$catID") ~> route ~> check {
        status shouldBe created
      }
    }

    "delete album" in new RoutesSpecContext {
      Delete(s"/albums/$birdsID") ~> route ~> check {
        status shouldBe ok
      }
    }

    "delete image in current album" in new RoutesSpecContext {
      Delete(s"/albums/$petsID/images/$dogID") ~> route ~> check {
        status shouldBe ok
      }
    }
  }

  class MockImageService(images: List[Image], albums: List[Album]) extends ImageService {
    override def upload(img: Image): Future[UUID] = Future.successful(images.find(_.id.contains(catID)).flatMap(_.id).get)

    override def getImgById(image_id: UUID): Future[Option[Image]] = Future.successful(images.find(_.id.contains(image_id)))

    override def getAllImg: Future[List[Image]] = Future.successful(images)

    override def delete(id: UUID): Future[Unit] = Future.unit

    override def getPublicImages: Future[List[Image]] = Future.successful(images.filter(_.visibility))

    override def getPublicImageById(id: UUID): Future[Option[Image]] = getPublicImages.map(_.find(_.id.contains(id)))

    override def getAllAlbums: Future[List[Album]] = Future.successful(albums)

    override def getAlbumById(album_id: UUID): Future[Option[Album]] = Future.successful(albums.find(_.id.contains(album_id)))

    override def createAlbum(album: Album): Future[UUID] = Future.successful(petsID)

    override def putImageIntoAlbum(image_id: UUID, album_id: UUID): Future[Unit] = Future.unit

    override def createImageFromAlbum(image: Image, album_id: UUID): Future[UUID] = upload(image)

    override def deleteAlbum(uuid: UUID): Future[Unit] = Future.unit

    override def getImagesByAlbumId(album_id: UUID): Future[List[Image]] = Future.successful(images)

    override def deleteImageFromAlbum(image_id: UUID, album_id: UUID): Future[Unit] = Future.unit
  }

  class MockAuthenticate(credentials: Option[HttpCredentials]) extends Authentication {
    override def authenticate: Directive1[LoggedInUser] = {
      credentials match {
        case Some(_) => AuthenticationDirective(provide(LoggedInUser("user", "pass", Role.User)))
        case None => AuthenticationDirective(provide(LoggedInUser.guest))
      }
    }
  }

  class MockStore() extends Store {
    override def saveImage(base64String: String): File = new File("D:\\img\\empty.txt")
  }

}
