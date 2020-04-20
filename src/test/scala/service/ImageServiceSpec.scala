package service

import java.util.UUID

import core.Image
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import repository.ImageRepo

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ImageServiceSpec extends AnyWordSpec with Matchers with ScalaFutures {

  sealed trait ImageServiceSpecContext {
    val catID: UUID = UUID.randomUUID()
    val dogID: UUID = UUID.randomUUID()
    val cat: Image = Image(Some(catID), "cat.jpg", Some("D:\\img\\pet"), visibility = true,Nil)
    val dog: Image = Image(Some(dogID), "dog.jpg", Some("D:\\img\\pet"), visibility = false,Nil)
    val listImages: List[Image] = List(cat, dog)
    val listImagesPublic: List[Image] = listImages.filter(_.visibility)
    val service = new DBImageService(new MockImageRepo(listImages))
  }

  "Service" should {
    "upload image without error" in new ImageServiceSpecContext {
      service.upload(cat) shouldBe Future.unit
      service.upload(dog) shouldBe Future.unit
    }
    "return all images" in new ImageServiceSpecContext {
      whenReady(service.getAllImg) { result =>
        result shouldBe listImages
      }
    }
    "return images with visibility = true" in new ImageServiceSpecContext {
      whenReady(service.getPublicImages) { result =>
        result shouldBe listImagesPublic
      }
    }
    "return image by id" in new ImageServiceSpecContext {
      whenReady(service.getImgById(catID)) { result =>
        result shouldBe Some(cat)
      }
      whenReady(service.getImgById(dogID)) { result =>
        result shouldBe Some(dog)
      }
    }
    "return only image with visibility = true" in new ImageServiceSpecContext {
      whenReady(service.getPublicImageById(catID)) { result =>
        result shouldBe Some(cat)
      }
      whenReady(service.getPublicImageById(dogID)) { result =>
        result shouldBe None
      }
    }
    "delete image without error" in new ImageServiceSpecContext {
      service.delete(catID) shouldBe Future.unit
      service.delete(dogID) shouldBe Future.unit
    }
  }

  class MockImageRepo(images: List[Image]) extends ImageRepo {
    override def create(img: Image): Future[Unit] = Future.unit

    override def getAll: Future[List[Image]] = Future.successful(images)

    override def delete(id: UUID): Future[Unit] = Future.unit

    override def getByID(id: UUID): Future[Option[Image]] = Future.successful(images.find(_.id.contains(id)))
  }

}
