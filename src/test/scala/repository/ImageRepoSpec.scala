package repository

import java.util.UUID

import core.{Album, Image}
import dao.ImageDAO
import database.{H2DBMem, Tables}
import generator.Generator
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future

class ImageRepoSpec extends AnyFreeSpecLike with Matchers with ScalaFutures with BeforeAndAfterEach {
  val publicImageID: UUID = UUID.randomUUID()
  val privateImageID: UUID = UUID.randomUUID()
  val rndImageID: UUID = UUID.randomUUID()
  val albumID: UUID = UUID.randomUUID()
  val album: Album = Album(Some(albumID), "Dogs")

  sealed trait ImageRepoContext {
    val generator = new MockUUIDGenerator
    val repo = new ImageRepoDB(generator) with H2DBMem
    val newImageID: UUID = generator.id
    val newAlbumID: UUID = generator.id
    val newAlbum: Album = Album(None, "Pets")
    val rndImage: Image = Image(Some(rndImageID), "duck.jpg", Some("D:\\img\\bird"), visibility = true)
    val publicImage: Image = Image(Some(publicImageID), "cat.jpg", Some("D:\\img\\pet"), visibility = true)
    val privateImage: Image = Image(Some(privateImageID), "dog.jpg", Some("D:\\img\\pet"), visibility = false)
    val newImage: Image = Image(Some(newImageID), "pig.png", Some("D:\\img"), visibility = false)

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

  "Repository" - {
    "Images table" - {
      "return all entities" in new ImageRepoContext {
        repo.getAllImages.futureValue shouldBe listImages
      }

      "create new entity in database" in new ImageRepoContext {
        repo.createImage(newImage).futureValue shouldBe newImageID
        repo.getAllImages.futureValue shouldBe listImages ::: List(newImage)
      }

      "get entity by id" in new ImageRepoContext {
        repo.getImageByID(publicImageID).futureValue shouldBe Some(publicImage)
        repo.getImageByID(privateImageID).futureValue shouldBe Some(privateImage)
        repo.getImageByID(newImageID).futureValue shouldBe None
      }

      "delete entity" in new ImageRepoContext {
        repo.delete(publicImageID).futureValue
        repo.getAllImages.futureValue shouldBe List(privateImage, rndImage)
      }
    }
    "Albums table" - {
      "create new album" in new ImageRepoContext {
        repo.createAlbum(newAlbum).futureValue shouldBe newAlbumID
      }

      "get album by id" in new ImageRepoContext {
        repo.getAlbumById(albumID).futureValue shouldBe Some(album)
      }

      "get all albums" in new ImageRepoContext {
        repo.getAllAlbums.futureValue shouldBe listAlbums
      }

      "delete album" in new ImageRepoContext {
        repo.deleteAlbum(albumID).futureValue
        repo.getAllAlbums.futureValue shouldBe Nil
      }
    }
    "Image-Albums table" - {
      "createImageFromAlbum" in new ImageRepoContext {
        repo.createImageFromAlbum(newImage, albumID).futureValue shouldBe newImageID
        repo.getAllImages.futureValue shouldBe listImages ::: List(newImage)
        repo.getImagesByAlbumID(albumID).futureValue shouldBe List(newImage, publicImage, privateImage)
      }

      "return images in album" in new ImageRepoContext {
        repo.getImagesByAlbumID(albumID).futureValue shouldBe List(publicImage, privateImage)
      }

      "put image in album" in new ImageRepoContext {
        repo.putImageIntoAlbum(rndImageID, albumID).futureValue
        repo.getImagesByAlbumID(albumID).futureValue shouldBe List(rndImage, publicImage, privateImage)
      }

      "delete image from album" in new ImageRepoContext {
        repo.deleteImageFromAlbum(privateImageID, albumID).futureValue
        repo.getImagesByAlbumID(albumID).futureValue shouldBe List(publicImage)
      }
    }
  }

  class MockUUIDGenerator extends Generator {
    val uuid: UUID = UUID.randomUUID()

    override def id: UUID = uuid
  }

}
