package repository

import java.util.UUID

import components.{Album, Image}
import dao.ImageDAO
import database.{H2DBMem, Tables}
import generator.Generator
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.Future

class AlbumRepoSpec extends AnyFreeSpecLike with Matchers with ScalaFutures with BeforeAndAfterEach {
  val publicImageID: UUID = UUID.randomUUID()
  val privateImageID: UUID = UUID.randomUUID()
  val rndImageID: UUID = UUID.randomUUID()
  val albumID: UUID = UUID.randomUUID()
  val album: Album = Album(Some(albumID), "Dogs")

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(1, Seconds), interval = Span(20, Millis))

  sealed trait AlbumRepoContext {
    val mockGenerator = new MockUUIDGenerator
    val albumRepo: AlbumRepoDB with H2DBMem = new AlbumRepoDB with H2DBMem {
      override val generator: Generator = mockGenerator
    }
    val newImageID: UUID = mockGenerator.id
    val newAlbumID: UUID = mockGenerator.id
    val newAlbum: Album = Album(None, "Pets")
    val rndImage: Image = Image(Some(rndImageID), "duck.jpg", Some("D:\\img\\bird"), visibility = true)
    val publicImage: Image = Image(Some(publicImageID), "cat.jpg", Some("D:\\img\\pet"), visibility = true)
    val privateImage: Image = Image(Some(privateImageID), "dog.jpg", Some("D:\\img\\pet"), visibility = false)
    val newImage: Image = Image(Some(newImageID), "pig.png", Some(s"D:\\img\\$newImageID.gif"), visibility = false)
    val pathFromBase64String: Option[String] = Some(s"D:\\img\\$newImageID.gif")
    val emptyPathFromBase64String: Option[String] = None
    val listImages: List[Image] = List(publicImage, privateImage, rndImage)
    val listAlbums = List(album)
  }

  object DBContext extends H2DBMem with Tables {

    import config.api._

    def initDB(): Future[Unit] = db.run(
      DBIO.seq(
        (images.schema ++ albums.schema ++ imageAlbums.schema).create,

        images += ImageDAO(Some(publicImageID), "cat.jpg", "D:\\img\\pet", visibility = true),
        images += ImageDAO(Some(privateImageID), "dog.jpg", "D:\\img\\pet", visibility = false),
        images += ImageDAO(Some(rndImageID), "duck.jpg", "D:\\img\\bird", visibility = true),

        albums += album,

        imageAlbums += ImageAlbum(privateImageID, albumID),
        imageAlbums += ImageAlbum(publicImageID, albumID)
      )
    )

    def clearDB: Future[Unit] = db.run((images.schema ++ albums.schema ++ imageAlbums.schema).drop)
  }

  override def beforeEach: Unit = {
    DBContext.initDB().futureValue
  }

  override def afterEach {
    DBContext.clearDB.futureValue
  }

  "Album repository" - {
    "create new album" in new AlbumRepoContext {
      albumRepo.createAlbum(newAlbum).futureValue shouldBe newAlbumID
    }

    "get album by id" in new AlbumRepoContext {
      albumRepo.getAlbumById(albumID).futureValue shouldBe Some(album)
    }

    "get all albums" in new AlbumRepoContext {
      albumRepo.getAllAlbums().futureValue shouldBe listAlbums
    }

    "delete album" in new AlbumRepoContext {
      albumRepo.deleteAlbum(albumID).futureValue
      albumRepo.getAllAlbums().futureValue shouldBe Nil
    }

    "createImageFromAlbum" in new AlbumRepoContext {
      albumRepo.createImageFromAlbum(newImage, albumID, pathFromBase64String).futureValue shouldBe Some(newImageID)
      albumRepo.getImagesByAlbumID(albumID).futureValue shouldBe List(newImage, publicImage, privateImage)
      albumRepo.createImageFromAlbum(newImage, albumID, emptyPathFromBase64String).futureValue shouldBe None
    }

    "return images in album" in new AlbumRepoContext {
      albumRepo.getImagesByAlbumID(albumID).futureValue shouldBe List(publicImage, privateImage)
    }

    "put image in album" in new AlbumRepoContext {
      albumRepo.putImageIntoAlbum(rndImageID, albumID).futureValue
      albumRepo.getImagesByAlbumID(albumID).futureValue shouldBe List(rndImage, publicImage, privateImage)
    }

    "delete image from album" in new AlbumRepoContext {
      albumRepo.deleteImageFromAlbum(privateImageID, albumID).futureValue
      albumRepo.getImagesByAlbumID(albumID).futureValue shouldBe List(publicImage)
    }
  }

  class MockUUIDGenerator extends Generator {
    val uuid: UUID = UUID.randomUUID()

    override def id: UUID = uuid
  }

}