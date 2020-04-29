package repository

import java.io.File
import java.util.UUID

import generator.Generator
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers

import scala.io.Source

class ImageLocalRepoSpec extends AnyFreeSpecLike with Matchers {

  sealed trait ImageLocalRepoContext {
    val generator = new MockGenerator
    val id: UUID = generator.id
    val store: ImageLocalRepo = new ImageLocalRepo(generator)
    val base64String: String = Source.fromResource("base64Image").getLines().mkString
    val emptyBase64: String = ""
  }

  "Image store" - {
    "save image" in new ImageLocalRepoContext {
      store.upload(base64String) shouldBe Some(new File(s"/img/$id.gif"))
    }

    "return none when pass wrong/empty string" in new ImageLocalRepoContext {
      store.upload(emptyBase64) shouldBe None
      store.upload("invalid string") shouldBe None
    }
  }

  class MockGenerator extends Generator {
    val uuid: UUID = UUID.randomUUID()

    override def id: UUID = uuid
  }

}
