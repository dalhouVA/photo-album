package repository

import java.util.UUID

import core.Image
import dao.ImageDAO
import database.{H2DBMem, Tables}
import generator.Generator
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Future

class ImageRepoSpec extends AnyWordSpec with Matchers with ScalaFutures with BeforeAndAfterEach {
  val catID: UUID = UUID.randomUUID()
  val dogID: UUID = UUID.randomUUID()

  sealed trait ImageRepoContext {
    val generator = new MockUUIDGenerator
    val repo = new ImageRepoDB(generator) with H2DBMem
    val pigID: UUID = generator.id
    val cat: Image = Image(Some(catID), "cat.jpg", Some("D:\\img\\pet"), visibility = true)
    val dog: Image = Image(Some(dogID), "dog.jpg", Some("D:\\img\\pet"), visibility = false)
    val pig: Image = Image(Some(pigID), "pig.png", Some("D:\\img"), visibility = false)

    val listImages: List[Image] = List(cat, dog)
  }

  object DBContext extends H2DBMem with Tables {

    import config.api._

    def initDB: Future[Unit] = db.run(
      DBIO.seq(
        images.schema.create,
        images += ImageDAO(Some(catID), "cat.jpg", "D:\\img\\pet", visibility = true),
        images += ImageDAO(Some(dogID), "dog.jpg", "D:\\img\\pet", visibility = false)
      )
    )

    def clearDB: Future[Unit] = db.run(images.schema.drop)
  }

  override def beforeEach {
    DBContext.initDB.futureValue
  }

  override def afterEach {
    DBContext.clearDB.futureValue
  }

  "Repository" should {
    "return all entities" in new ImageRepoContext {
      repo.getAllImages.futureValue shouldBe listImages
    }

    "create new entity in database" in new ImageRepoContext {
      repo.createImage(pig).futureValue shouldBe pigID
      repo.getAllImages.futureValue shouldBe List(cat, dog, pig)
    }

    "get entity by id" in new ImageRepoContext {
      repo.getImageByID(catID).futureValue shouldBe Some(cat)
      repo.getImageByID(dogID).futureValue shouldBe Some(dog)
      repo.getImageByID(pigID).futureValue shouldBe None
    }
    
    "delete entity" in new ImageRepoContext {
      repo.delete(catID).futureValue
      repo.getAllImages.futureValue shouldBe List(dog)
    }
  }

  class MockUUIDGenerator extends Generator {
    val mockID = UUID.randomUUID()

    override def id: UUID = mockID
  }

}
