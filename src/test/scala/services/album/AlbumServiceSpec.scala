package services.album

import java.util.UUID

import components.{Album, Image}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import repository.AlbumRepo

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
    val service = new DBAlbumService(new MockAlbumRepo(listImages, listAlbums))
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
      service.createImageFromAlbum(privateImage, albumID).futureValue shouldBe privateImageID
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
  }

  class MockAlbumRepo(images: List[Image], albums: List[Album]) extends AlbumRepo {

    override def getAllAlbums(): Future[List[Album]] = Future.successful(albums)

    override def getAlbumById(albumID: UUID): Future[Option[Album]] = Future.successful(albums.find(_.id.contains(albumID)))

    override def createAlbum(album: Album): Future[UUID] = Future.successful(albumID)

    override def putImageIntoAlbum(imageID: UUID, albumID: UUID): Future[Unit] = Future.unit

    override def createImageFromAlbum(image: Image, albumID: UUID): Future[UUID] = Future.successful(privateImageID)

    override def getImagesByAlbumID(albumID: UUID): Future[List[Image]] = Future.successful(images)

    override def deleteAlbum(id: UUID): Future[Unit] = Future.unit

    override def deleteImageFromAlbum(imageID: UUID, albumID: UUID): Future[Unit] = Future.unit
  }

}