package repository

import java.util.UUID

import components.Image
import dao.ImageDAO
import database.{H2DBMem, Tables}
import generator.Generator
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.Future

class ImageRepoSpec extends AnyFreeSpecLike with Matchers with ScalaFutures with BeforeAndAfterEach {
  val publicImageID: UUID = UUID.randomUUID()
  val privateImageID: UUID = UUID.randomUUID()
  val rndImageID: UUID = UUID.randomUUID()

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(1, Seconds), interval = Span(20, Millis))

  sealed trait ImageRepoContext {
    val mockGenerator = new MockUUIDGenerator
    val imageRepo: ImageRepoDB with H2DBMem = new ImageRepoDB with H2DBMem {
      override val generator: Generator = mockGenerator
    }
    val newImageID: UUID = mockGenerator.id
    val rndImage: Image = Image(Some(rndImageID), "duck.jpg", Some("D:\\img\\bird"), visibility = true)
    val publicImage: Image = Image(Some(publicImageID), "cat.jpg", Some("D:\\img\\pet"), visibility = true)
    val privateImage: Image = Image(Some(privateImageID), "dog.jpg", Some("D:\\img\\pet"), visibility = false)
    val newImage: Image = Image(Some(newImageID), "pig.png", Some(s"D:\\img\\$newImageID.gif"), visibility = false)
    val base64String: Option[String] = Some(s"D:\\img\\$newImageID.gif")
    val emptyBase64String: Option[String] = None
    val listImages: List[Image] = List(publicImage, privateImage, rndImage)
  }

  object DBContext extends H2DBMem with Tables {

    import config.api._

    def initDB(): Future[Unit] = db.run(
      DBIO.seq(
        images.schema.create,

        images += ImageDAO(Some(publicImageID), "cat.jpg", "D:\\img\\pet", visibility = true),
        images += ImageDAO(Some(privateImageID), "dog.jpg", "D:\\img\\pet", visibility = false),
        images += ImageDAO(Some(rndImageID), "duck.jpg", "D:\\img\\bird", visibility = true)
      )
    )

    def clearDB: Future[Unit] = db.run(images.schema.drop)
  }

  override def beforeEach: Unit = {
    DBContext.initDB().futureValue
  }

  override def afterEach {
    DBContext.clearDB.futureValue
  }

  "Image repository" - {
    "return all entities" in new ImageRepoContext {
      imageRepo.getAllImages().futureValue shouldBe listImages
    }

    "create new entity in database" in new ImageRepoContext {
      imageRepo.createImage(newImage, base64String).futureValue shouldBe Some(newImageID)
      imageRepo.getAllImages().futureValue shouldBe listImages ::: List(newImage)
      imageRepo.createImage(newImage, emptyBase64String).futureValue shouldBe None
    }

    "get entity by id" in new ImageRepoContext {
      imageRepo.getImageByID(publicImageID).futureValue shouldBe Some(publicImage)
      imageRepo.getImageByID(privateImageID).futureValue shouldBe Some(privateImage)
      imageRepo.getImageByID(newImageID).futureValue shouldBe None
    }

    "delete entity" in new ImageRepoContext {
      imageRepo.delete(publicImageID).futureValue
      imageRepo.getAllImages().futureValue shouldBe List(privateImage, rndImage)
    }
  }

  class MockUUIDGenerator extends Generator {
    val uuid: UUID = UUID.randomUUID()

    override def id: UUID = uuid
  }

}
