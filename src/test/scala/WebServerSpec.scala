import akka.http.scaladsl.model.{ContentTypes, HttpEntity, Multipart}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import io.circe.generic.auto._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._



class WebServerSpec() extends AnyWordSpec with Matchers with ScalatestRouteTest {

  "The server" should {
    "return file name" in  new WebServerSpec {
      Post("/album/upload") ~> WebServer.routes ~> check {
        println(response)
        responseAs[String] shouldBe "cat.jpg"
      }
    }
  }
}
