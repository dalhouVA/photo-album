import akka.http.scaladsl.model.{ContentTypes, HttpEntity, Multipart}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import io.circe.generic.auto._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import repository.PhotoRepo
import service.PhotoService



class WebServerSpec() extends AnyWordSpec with Matchers with ScalatestRouteTest  {

  sealed trait ServerSpec extends Routes {
    val routes: Route = route(PhotoService(PhotoRepo))
  }

  "The server" should {
    "return file name" in  new ServerSpec  {
      Post("/album/upload") ~> routes ~> check {
        println(response)
        responseAs[String] shouldBe "cat.jpg"
      }
    }
  }
}
