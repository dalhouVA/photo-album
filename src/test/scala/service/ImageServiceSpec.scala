package service

import java.util.UUID

import core.{Album, Image}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import repository.ImageRepo

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ImageServiceSpec extends AnyFreeSpecLike with Matchers with ScalaFutures {

  val publicImageID: UUID = UUID.randomUUID()
  val privateImageID: UUID = UUID.randomUUID()
  val albumID: UUID = UUID.randomUUID()
  val nonExistingAlbumID: UUID = UUID.randomUUID()

  sealed trait ImageServiceSpecContext {
    val publicImage: Image = Image(Some(publicImageID), "cat.jpg", Some("D:\\img\\pet"), visibility = true)
    val privateImage: Image = Image(Some(privateImageID), "dog.jpg", Some("D:\\img\\pet"), visibility = false)
    val album: Album = Album(Some(albumID), "Pets")
    val listImages: List[Image] = List(publicImage, privateImage)
    val listAlbums: List[Album] = List(album)
    val listImagesPublic: List[Image] = listImages.filter(_.visibility)
    val service = new DBImageService(new MockImageRepo(listImages, listAlbums))
  }


  "Service" - {
    "Images" - {
      "upload image without error" in new ImageServiceSpecContext {
        service.upload(publicImage).futureValue shouldBe publicImageID
      }

      "return all images" in new ImageServiceSpecContext {
        service.getAllImg.futureValue shouldBe listImages
      }

      "return public images " in new ImageServiceSpecContext {
        service.getPublicImages.futureValue shouldBe listImagesPublic
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
    "Albums" - {
      "create new album" in new ImageServiceSpecContext {
        service.createAlbum(album).futureValue shouldBe albumID
      }

      "return album by id" in new ImageServiceSpecContext {
        service.getAlbumById(albumID).futureValue shouldBe Some(album)
        service.getAlbumById(nonExistingAlbumID).futureValue shouldBe None
      }

      "return all albums" in new ImageServiceSpecContext {
        service.getAllAlbums.futureValue shouldBe listAlbums
      }

      "create new image in existing album" in new ImageServiceSpecContext {
        service.createImageFromAlbum(privateImage, albumID).futureValue shouldBe privateImageID
      }

      "put image into album" in new ImageServiceSpecContext {
        service.putImageIntoAlbum(privateImageID, albumID) shouldBe Future.unit
      }

      "get images from album" in new ImageServiceSpecContext {
        service.getImagesByAlbumId(albumID).futureValue shouldBe listImages
      }

      "delete album" in new ImageServiceSpecContext {
        service.deleteAlbum(albumID) shouldBe Future.unit
      }

      "delete image in album" in new ImageServiceSpecContext {
        service.deleteImageFromAlbum(privateImageID,albumID) shouldBe Future.unit
      }
    }
  }

  class MockImageRepo(images: List[Image], albums: List[Album]) extends ImageRepo {

    override def createImage(img: Image): Future[UUID] = Future.successful(publicImageID)

    override def getAllImages: Future[List[Image]] = Future.successful(images)

    override def delete(id: UUID): Future[Unit] = Future.unit

    override def getImageByID(image_id: UUID): Future[Option[Image]] = Future.successful(images.find(_.id.contains(image_id)))

    override def getAllAlbums: Future[List[Album]] = Future.successful(albums)

    override def getAlbumById(album_id: UUID): Future[Option[Album]] = Future.successful(albums.find(_.id.contains(album_id)))

    override def createAlbum(album: Album): Future[UUID] = Future.successful(albumID)

    override def putImageIntoAlbum(image_id: UUID, album_id: UUID): Future[Unit] = Future.unit

    override def createImageFromAlbum(image: Image, album_id: UUID): Future[UUID] = Future.successful(privateImageID)

    override def getImagesByAlbumID(album_id: UUID): Future[List[Image]] = Future.successful(images)

    override def deleteAlbum(id: UUID): Future[Unit] = Future.unit

    override def deleteImageFromAlbum(image_id: UUID, album_id: UUID): Future[Unit] = Future.unit
  }

}
