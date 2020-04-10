package route

import java.io.File
import java.util.UUID

import akka.http.scaladsl.model.Multipart.FormData
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, Multipart, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import core.Image
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import dto.ImageDTO
import dto.converters.ImageDTOConverter
import io.circe.generic.auto._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import service.ImageService

import scala.concurrent.Future


class RoutesSpec() extends AnyWordSpec with Matchers with ScalatestRouteTest {

  sealed trait RoutesSpecContext extends Routes {
    val catID: UUID = UUID.randomUUID()
    val dogID: UUID = UUID.randomUUID()
    val pigID: UUID = UUID.randomUUID()
    val catFile = new File("D:\\img\\pet", "cat.jpg")
    val dogFile = new File("D:\\img\\pet", "dog.jpg")
    val cat: Image = Image(Some(catID), "cat.jpg", Some(catFile), Some("D:\\img\\pet"), visibility = true)
    val dog: Image = Image(Some(dogID), "dog.jpg", Some(dogFile), Some("D:\\img\\pet"), visibility = false)
    val listImages: List[Image] = List(cat, dog)
    val listImagesDTO: List[ImageDTO] = listImages.map(ImageDTOConverter.fromImage)
    val listImagesDTOPublic: List[ImageDTO] = listImages.filter(_.visibility).map(ImageDTOConverter.fromImage)
    val validAuthenticate: BasicHttpCredentials = BasicHttpCredentials("jane", "123")
    val imageRoute: Route = route(new MockImageService(listImages))
    val ok: StatusCodes.Success = StatusCodes.OK
    val created: StatusCodes.Success = StatusCodes.Created
    val bad: StatusCodes.ClientError = StatusCodes.BadRequest
    val unauthorized: StatusCodes.ClientError = StatusCodes.Unauthorized
    val multipartForm: FormData.Strict =
      Multipart.FormData(Multipart.FormData.BodyPart.Strict(
        "image",
        HttpEntity(ContentTypes.`text/plain(UTF-8)`, "2,3,5\n7,11,13,17,23\n29,31,37\n"),
        Map("filename" -> "primes.jpeg")))
  }

  "The server" should {
    "return list of all images" in new RoutesSpecContext {
      Get("/album") ~> addCredentials(validAuthenticate) ~> imageRoute ~> check {
        status shouldBe StatusCodes.OK
        responseAs[List[ImageDTO]] shouldBe listImagesDTO
      }
    }
    "return list of images with visibility = true" in new RoutesSpecContext {
      Get("/album") ~> imageRoute ~> check {
        status shouldBe ok
        responseAs[List[ImageDTO]] shouldBe listImagesDTOPublic
      }
    }
    "return image by id with valid credentials when visibility = false" in new RoutesSpecContext {
      Get(s"/album/$dogID") ~> addCredentials(validAuthenticate) ~> imageRoute ~> check {
        status shouldBe ok
        responseAs[Option[ImageDTO]] shouldBe Some(ImageDTOConverter.fromImage(dog))
      }
    }
    "return image by id with valid credentials when visibility = true" in new RoutesSpecContext {
      Get(s"/album/$catID") ~> addCredentials(validAuthenticate) ~> imageRoute ~> check {
        status shouldBe ok
        responseAs[Option[ImageDTO]] shouldBe Some(ImageDTOConverter.fromImage(cat))
      }
    }
    "return image by id without credentials when visibility = true" in new RoutesSpecContext {
      Get(s"/album/$catID") ~> imageRoute ~> check {
        status shouldBe ok
        responseAs[Option[ImageDTO]] shouldBe Some(ImageDTOConverter.fromImage(cat))
      }
    }
    "return unauthorized error" in new RoutesSpecContext {
      Get(s"/album/$dogID") ~> imageRoute ~> check {
        status shouldBe unauthorized
        responseAs[String] shouldBe "You have to authorize for get access"
      }
    }
    "return unauthorized error when pass invalid id" in new RoutesSpecContext {
      Get(s"/album/$pigID") ~> imageRoute ~> check {
        status shouldBe unauthorized
        responseAs[String] shouldBe "You have to authorize for get access"
      }
    }
    "return bad request error" in new RoutesSpecContext {
      Get(s"/album/$pigID") ~> addCredentials(validAuthenticate) ~> imageRoute ~> check {
        status shouldBe bad
        responseAs[String] shouldBe "Image with this id not found"
      }
    }
    "delete image by id" in new RoutesSpecContext {
      Delete(s"/album/$catID") ~> addCredentials(validAuthenticate) ~> imageRoute ~> check {
        status shouldBe ok
      }
    }
    "create new entity" in new RoutesSpecContext {
      Post("/album/upload?visibility=true", multipartForm) ~> addCredentials(validAuthenticate) ~> imageRoute ~> check {
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
}
