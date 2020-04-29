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
    val generator = new MockUUIDGenerator
    val repo = new ImageRepoDB(generator) with H2DBMem
    val newImageID: UUID = generator.id
    val rndImage: Image = Image(Some(rndImageID), "duck.jpg", Some("D:\\img\\bird"), visibility = true)
    val publicImage: Image = Image(Some(publicImageID), "cat.jpg", Some("D:\\img\\pet"), visibility = true)
    val privateImage: Image = Image(Some(privateImageID), "dog.jpg", Some("D:\\img\\pet"), visibility = false)
    val newImage: Image = Image(Some(newImageID), "pig.png", Some("D:\\img"), visibility = false)

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
      repo.getAllImages().futureValue shouldBe listImages
    }

    "create new entity in database" in new ImageRepoContext {
      repo.createImage(newImage).futureValue shouldBe newImageID
      repo.getAllImages().futureValue shouldBe listImages ::: List(newImage)
    }

    "get entity by id" in new ImageRepoContext {
      repo.getImageByID(publicImageID).futureValue shouldBe Some(publicImage)
      repo.getImageByID(privateImageID).futureValue shouldBe Some(privateImage)
      repo.getImageByID(newImageID).futureValue shouldBe None
    }

    "delete entity" in new ImageRepoContext {
      repo.delete(publicImageID).futureValue
      repo.getAllImages().futureValue shouldBe List(privateImage, rndImage)
    }
  }

  class MockUUIDGenerator extends Generator {
    val uuid: UUID = UUID.randomUUID()

    override def id: UUID = uuid
  }

}
