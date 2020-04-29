package services.album

import java.util.UUID

import components.{Album, Image}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import repository.{AlbumRepo, PhotoRepo}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AlbumServiceSpec extends AnyFreeSpecLike with Matchers with ScalaFutures {
  val publicImageID: UUID = UUID.randomUUID()
  val privateImageID: UUID = UUID.randomUUID()
  val albumID: UUID = UUID.randomUUID()
  val nonExistingAlbumID: UUID = UUID.randomUUID()

  sealed trait AlbumServiceSpecContext {
    val publicImage: Image = Image(Some(publicImageID), "cat.jpg", Some("D:\\img\\pet"), visibility = true)
    val privateImage: Image = Image(Some(privateImageID), "dog.jpg", Some("D:\\img\\pet"), visibility = false)
    val album: Album = Album(Some(albumID), "Pets")
    val listImages: List[Image] = List(publicImage, privateImage)
    val listAlbums: List[Album] = List(album)
    val base64: String = "base64String"
    val emptyBase64: String = ""
    val service = new DBAlbumService(new MockAlbumRepo(listImages, listAlbums), new MockPhotoRepo)
  }

  "Album service" - {
    "create new album" in new AlbumServiceSpecContext {
      service.createAlbum(album).futureValue shouldBe albumID
    }

    "return album by id" in new AlbumServiceSpecContext {
      service.getAlbumById(albumID).futureValue shouldBe Some(album)
      service.getAlbumById(nonExistingAlbumID).futureValue shouldBe None
    }

    "return all albums" in new AlbumServiceSpecContext {
      service.getAllAlbums().futureValue shouldBe listAlbums
    }

    "create new image in existing album" in new AlbumServiceSpecContext {
      service.createImageFromAlbum(privateImage, albumID, base64).futureValue shouldBe Some(privateImageID)
      service.createImageFromAlbum(privateImage, albumID, emptyBase64).futureValue shouldBe None
    }

    "put image into album" in new AlbumServiceSpecContext {
      service.putImageIntoAlbum(privateImageID, albumID) shouldBe Future.unit
    }

    "get images from album" in new AlbumServiceSpecContext {
      service.getImagesByAlbumId(albumID).futureValue shouldBe listImages
    }

    "delete album" in new AlbumServiceSpecContext {
      service.deleteAlbum(albumID) shouldBe Future.unit
    }

    "delete image in album" in new AlbumServiceSpecContext {
      service.deleteImageFromAlbum(privateImageID, albumID) shouldBe Future.unit
    }

    "return image in album" in new AlbumServiceSpecContext {
      service.getImage(albumID, publicImageID).futureValue shouldBe Some(publicImage)
    }
  }

  class MockAlbumRepo(images: List[Image], albums: List[Album]) extends AlbumRepo {

    override def getAllAlbums(): Future[List[Album]] = Future.successful(albums)

    override def getAlbumById(albumID: UUID): Future[Option[Album]] = Future.successful(albums.find(_.id.contains(albumID)))

    override def createAlbum(album: Album): Future[UUID] = Future.successful(albumID)

    override def putImageIntoAlbum(imageID: UUID, albumID: UUID): Future[Unit] = Future.unit

    override def createImageFromAlbum(image: Image, albumID: UUID, path: Option[String]): Future[Option[UUID]] = path match {
      case Some(_) => Future.successful(Some(privateImageID))
      case None => Future.successful(None)
    }

    override def getImagesByAlbumID(albumID: UUID): Future[List[Image]] = Future.successful(images)

    override def deleteAlbum(id: UUID): Future[Unit] = Future.unit

    override def deleteImageFromAlbum(imageID: UUID, albumID: UUID): Future[Unit] = Future.unit

    override def getImage(imageID: UUID, albumID: UUID): Future[Option[Image]] = Future.successful(images.find(_.id.contains(imageID)))
  }

  class MockPhotoRepo extends PhotoRepo {
    override def uploadImageInRepo(base64String: String): Future[Option[String]] =
      if (base64String.isEmpty)
        Future.successful(None)
      else
        Future.successful(Some("D:\\img\\"))
  }

}