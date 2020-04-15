package authentication

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import core.User._
import io.circe.generic.auto._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AuthenticationSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {

  sealed trait AuthenticationContext {
    val auth = new Auth
  }

  "Authentication" should {
    "return guest, when credentials is empty" in new AuthenticationContext {
      Get("/") ~> auth.authenticate(x=>complete(x)) ~> check {
        responseAs[User] shouldBe Guest
      }
    }
  }

}
