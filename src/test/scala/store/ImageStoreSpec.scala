package store

import java.io.File
import java.util.UUID

import generator.Generator
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers

import scala.io.Source

class ImageStoreSpec extends AnyFreeSpecLike with Matchers {

  sealed trait ImageStoreContext {
    val generator = new MockGenerator
    val id: UUID = generator.id
    val store: ImageStore = new ImageStore(generator)
    val base64String: String = Source.fromResource("base64Image").getLines().mkString
    val emptyBase64: String = ""
  }

  "Image store" - {
    "save image" in new ImageStoreContext {
      store.saveImage(base64String) shouldBe Some(new File(s"/img/$id.gif"))
    }

    "return none when pass wrong/empty string" in new ImageStoreContext {
      store.saveImage(emptyBase64) shouldBe None
      store.saveImage("invalid string") shouldBe None
    }
  }

  class MockGenerator extends Generator {
    val uuid: UUID = UUID.randomUUID()

    override def id: UUID = uuid
  }

}
