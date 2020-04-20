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
import core.{Image, LoggedInUser, Role}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import dto.converters.ImageDTOConverter
import dto.{InImageDTO, OutImageDTO}
import io.circe.generic.auto._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import service.ImageService
import store.Store

import scala.concurrent.Future


class RoutesSpec() extends AnyWordSpec with Matchers with ScalatestRouteTest {

  sealed trait RoutesSpecContext extends Routes {
    val catID: UUID = UUID.randomUUID()
    val dogID: UUID = UUID.randomUUID()
    val pigID: UUID = UUID.randomUUID()
    val cat: Image = Image(Some(catID), "cat.jpg", Some("D:\\img\\pet"), visibility = true,Nil)
    val dog: Image = Image(Some(dogID), "dog.jpg", Some("D:\\img\\pet"), visibility = false,Nil)
    val catDTO: InImageDTO = InImageDTO("cat", "asdqwr", visibility = true,Nil)
    val listImages: List[Image] = List(cat, dog)
    val listImagesDTO: List[OutImageDTO] = listImages.map(ImageDTOConverter.fromImage)
    val listImagesDTOPublic: List[OutImageDTO] = listImages.filter(_.visibility).map(ImageDTOConverter.fromImage)
    val validAuthenticate: BasicHttpCredentials = BasicHttpCredentials("jane", "123")
    val imageRoute: Route = route(new MockImageService(listImages), new MockAuthenticate(Some(validAuthenticate)), new MockStore())
    val guestImageRoute: Route = route(new MockImageService(listImages), new MockAuthenticate(None), new MockStore())
    val ok: StatusCodes.Success = StatusCodes.OK
    val created: StatusCodes.Success = StatusCodes.Created
    val bad: StatusCodes.ClientError = StatusCodes.BadRequest
    val unauthorized: StatusCodes.ClientError = StatusCodes.Unauthorized
  }

  "The server" should {
    "return list of all images" in new RoutesSpecContext {
      Get("/album") ~> imageRoute ~> check {
        status shouldBe ok
        responseAs[List[OutImageDTO]] shouldBe listImagesDTO
      }
    }
    "return list of images with visibility = true" in new RoutesSpecContext {
      Get("/album") ~> guestImageRoute ~> check {
        status shouldBe ok
        responseAs[List[OutImageDTO]] shouldBe listImagesDTOPublic
      }
    }
    "return image by id with valid credentials when visibility = false" in new RoutesSpecContext {
      Get(s"/album/$dogID") ~> imageRoute ~> check {
        status shouldBe ok
        responseAs[Option[OutImageDTO]] shouldBe Some(ImageDTOConverter.fromImage(dog))
      }
    }
    "return image by id with valid credentials when visibility = true" in new RoutesSpecContext {
      Get(s"/album/$catID") ~> imageRoute ~> check {
        status shouldBe ok
        responseAs[Option[OutImageDTO]] shouldBe Some(ImageDTOConverter.fromImage(cat))
      }
    }
    "return image by id without credentials when visibility = true" in new RoutesSpecContext {
      Get(s"/album/$catID") ~> guestImageRoute ~> check {
        status shouldBe ok
        responseAs[Option[OutImageDTO]] shouldBe Some(ImageDTOConverter.fromImage(cat))
      }
    }
    "return unauthorized error" in new RoutesSpecContext {
      Get(s"/album/$dogID") ~> guestImageRoute ~> check {
        status shouldBe unauthorized
        responseAs[String] shouldBe "You have to authorize for get access"
      }
    }
    "return unauthorized error when pass invalid id" in new RoutesSpecContext {
      Get(s"/album/$pigID") ~> guestImageRoute ~> check {
        status shouldBe unauthorized
        responseAs[String] shouldBe "You have to authorize for get access"
      }
    }
    "return bad request error" in new RoutesSpecContext {
      Get(s"/album/$pigID") ~> imageRoute ~> check {
        status shouldBe bad
        responseAs[String] shouldBe "Image with this id not found"
      }
    }
    "delete image by id" in new RoutesSpecContext {
      Delete(s"/album/$catID") ~> imageRoute ~> check {
        status shouldBe ok
      }
    }
    "create new entity" in new RoutesSpecContext {
      Post("/album/upload", content = catDTO) ~> imageRoute ~> check {
        status shouldBe created
      }
    }
  }

  class MockImageService(images: List[Image]) extends ImageService {
    override def upload(img: Image): Future[Unit] = Future.unit

    override def getImgById(id: UUID): Future[Option[Image]] = getAllImg.map(_.find(_.id.contains(id)))

    override def getAllImg: Future[List[Image]] = Future.successful(images)

    override def delete(id: UUID): Future[Unit] = Future.unit

    override def getPublicImages: Future[List[Image]] = Future.successful(images.filter(_.visibility))

    override def getPublicImageById(id: UUID): Future[Option[Image]] = getPublicImages.map(_.find(_.id.contains(id)))
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
