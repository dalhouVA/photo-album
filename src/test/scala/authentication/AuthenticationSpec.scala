package authentication

import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import core.Role
import core.User._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import repository.UserRepo

class AuthenticationSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {

  sealed trait AuthenticationContext {
    val userJack: RegisteredUser = RegisteredUser("jack", "123456", Role.User)
    val users = List(userJack)
    val repo = new MockUserRepo(users)
    val auth = new Auth(repo)
    val cred: BasicHttpCredentials = BasicHttpCredentials("jack", "123456")
    val randomCred: BasicHttpCredentials = BasicHttpCredentials("user", "pass")
  }

  "Authentication" should {
    "return guest, when credentials is empty" in new AuthenticationContext {
      Get("/") ~> auth.authenticate(x => complete(x)) ~> check {
        responseAs[User] shouldBe Guest
      }
    }
    "return guest when credentials doesn't exist in db" in new AuthenticationContext {
      Get("/") ~> addCredentials(randomCred) ~> auth.authenticate(complete(_)) ~> check {
        responseAs[User] shouldBe Guest
      }
    }
    "return guest when pass doesn't match" in new AuthenticationContext {
      Get("/") ~> addCredentials(BasicHttpCredentials("jack", "12")) ~> auth.authenticate(complete(_)) ~> check {
        responseAs[User] shouldBe Guest
      }
    }
    "return registered user" in new AuthenticationContext {
      Get("/") ~> addCredentials(cred) ~> auth.authenticate(complete(_)) ~> check {
        responseAs[User] shouldBe userJack
      }
    }
  }

  class MockUserRepo(users: List[User]) extends UserRepo {
    override def getUserByName(name: String): Option[User] = users.find(_.login == name)
  }

}
