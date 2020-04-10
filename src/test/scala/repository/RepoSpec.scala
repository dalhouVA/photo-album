package repository

import java.io.File
import java.util.UUID

import core.Image
import dao.ImageDAO
import database.DB
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import slick.jdbc.H2Profile
import slick.jdbc.JdbcBackend._

import scala.concurrent.Future


class RepoSpec extends AnyWordSpec with Matchers with ScalaFutures with BeforeAndAfterEach {
  val catID: UUID = UUID.randomUUID()
  val dogID: UUID = UUID.randomUUID()
  val pigID: UUID = UUID.randomUUID()
  val context: ImageRepoContext = new ImageRepoContext {}

  sealed trait ImageRepoContext extends H2DBMem {

    import config.api._

    def initDB: Future[Unit] = db.run(
      DBIO.seq(
        images.schema.create,
        images += ImageDAO(Some(catID), "cat.jpg", "D:\\img\\pet", visibility = true),
        images += ImageDAO(Some(dogID), "dog.jpg", "D:\\img\\pet", visibility = false)
      )
    )

    def clearDB: Future[Unit] = db.run(images.schema.drop)

    val repo = new DBImageRepo with H2DBMem
    val pigFile = new File("D:\\img", "pig.png")
    val cat: Image = Image(Some(catID), "cat.jpg", None, Some("D:\\img\\pet"), visibility = true)
    val dog: Image = Image(Some(dogID), "dog.jpg", None, Some("D:\\img\\pet"), visibility = false)
    val pig: Image = Image(Some(pigID), "pig.png", Some(pigFile), Some("D:\\img"), visibility = false)
    val pigFromDB: Image = pig.copy(file = None)
    val listImages: List[Image] = List(cat, dog)

  }

  override def beforeEach{
    context.initDB.futureValue
  }

  override def afterEach{
    context.clearDB.futureValue
  }

  "Repository" should {
    "return all entities" in new ImageRepoContext {
      repo.getAll.futureValue shouldBe listImages
    }
    "create new entity in database" in new ImageRepoContext {
      repo.create(pig).futureValue
      repo.getAll.futureValue shouldBe List(cat, dog, pigFromDB)
    }
    "get entity by id" in new ImageRepoContext {
      repo.getByID(catID).futureValue shouldBe Some(cat)
      repo.getByID(dogID).futureValue shouldBe Some(dog)
      repo.getByID(pigID).futureValue shouldBe None
    }
    "delete entity" in new ImageRepoContext {
      repo.delete(catID).futureValue
      repo.getAll.futureValue shouldBe List(dog)
    }
  }

  trait H2DBMem extends DB {
    override lazy val config = H2Profile
    override lazy val db = Database.forConfig("h2mem")
  }

}
