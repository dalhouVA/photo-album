package services.image

import java.io.File
import java.util.UUID

import components.{Album, Image}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import repository.{ImageRepo, PhotoRepo}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ImageServiceSpec extends AnyFreeSpecLike with Matchers with ScalaFutures {

  val publicImageID: UUID = UUID.randomUUID()
  val privateImageID: UUID = UUID.randomUUID()
  val albumID: UUID = UUID.randomUUID()
  val nonExistingAlbumID: UUID = UUID.randomUUID()
  val testFile = new File("D:\\img\\empty.txt")

  sealed trait ImageServiceSpecContext {
    val publicImage: Image = Image(Some(publicImageID), "cat.jpg", Some("D:\\img\\pet"), visibility = true)
    val privateImage: Image = Image(Some(privateImageID), "dog.jpg", Some("D:\\img\\pet"), visibility = false)
    val album: Album = Album(Some(albumID), "Pets")
    val listImages: List[Image] = List(publicImage, privateImage)
    val listAlbums: List[Album] = List(album)
    val listImagesPublic: List[Image] = listImages.filter(_.visibility)
    val base64: String = "base64String"
    val emptyBase64:String = ""
    val service = new DBImageService(new MockImageRepo(listImages, listAlbums),new MockPhotoRepo)
  }

  "Image service" - {
    "upload image with valid base64 string" in new ImageServiceSpecContext {
      service.upload(publicImage, base64).futureValue shouldBe Some(publicImageID)
      service.upload(publicImage,emptyBase64).futureValue shouldBe None
    }

    "return all images" in new ImageServiceSpecContext {
      service.getAllImg().futureValue shouldBe listImages
    }

    "return public images " in new ImageServiceSpecContext {
      service.getPublicImages().futureValue shouldBe listImagesPublic
    }

    "return image by id" in new ImageServiceSpecContext {
      service.getImgById(publicImageID).futureValue shouldBe Some(publicImage)
      service.getImgById(privateImageID).futureValue shouldBe Some(privateImage)
    }

    "return public image by id" in new ImageServiceSpecContext {
      service.getPublicImageById(publicImageID).futureValue shouldBe Some(publicImage)
      service.getPublicImageById(privateImageID).futureValue shouldBe None
    }

    "delete image without error" in new ImageServiceSpecContext {
      service.delete(publicImageID) shouldBe Future.unit
      service.delete(privateImageID) shouldBe Future.unit
    }

  }

  class MockImageRepo(images: List[Image], albums: List[Album]) extends ImageRepo {

    override def createImage(img: Image, path: Option[String]): Future[Option[UUID]] = path match {
      case Some(_) => Future.successful(Some(publicImageID))
      case None =>Future.successful(None)
    }


    override def getAllImages(): Future[List[Image]] = Future.successful(images)

    override def delete(id: UUID): Future[Unit] = Future.unit

    override def getImageByID(imageID: UUID): Future[Option[Image]] = Future.successful(images.find(_.id.contains(imageID)))
  }

  class MockPhotoRepo extends PhotoRepo{
    override def uploadImageInRepo(base64String: String): Future[Option[String]] =
      if (base64String.isEmpty)
        Future.successful(None)
      else
        Future.successful(Some("D:\\img\\"))
  }
}
