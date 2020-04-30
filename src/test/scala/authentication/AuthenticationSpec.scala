package authentication

import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import components.{LoggedInUser, Role}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import repository.UserRepo

import scala.concurrent.Future

class AuthenticationSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {

  sealed trait AuthenticationContext {
    val userJack: LoggedInUser = LoggedInUser("jack", "123456", Role.User)
    val users = List(userJack)
    val repo = new MockUserRepo(users)
    val auth = new Auth(repo)
    val cred: BasicHttpCredentials = BasicHttpCredentials("jack", "123456")
    val randomCred: BasicHttpCredentials = BasicHttpCredentials("user", "pass")
  }

  "Authentication" should {
    "return guest, when credentials is empty" in new AuthenticationContext {
      Get("/") ~> auth.authenticate(x => complete(x)) ~> check {
        responseAs[LoggedInUser] shouldBe LoggedInUser.guest
      }
    }
    "return guest when credentials doesn't exist in db" in new AuthenticationContext {
      Get("/") ~> addCredentials(randomCred) ~> auth.authenticate(complete(_)) ~> check {
        responseAs[LoggedInUser] shouldBe LoggedInUser.guest
      }
    }
    "return guest when pass doesn't match" in new AuthenticationContext {
      Get("/") ~> addCredentials(BasicHttpCredentials("jack", "12")) ~> auth.authenticate(complete(_)) ~> check {
        responseAs[LoggedInUser] shouldBe LoggedInUser.guest
      }
    }
    "return registered user" in new AuthenticationContext {
      Get("/") ~> addCredentials(cred) ~> auth.authenticate(complete(_)) ~> check {
        responseAs[LoggedInUser] shouldBe userJack
      }
    }
  }

  class MockUserRepo(users: List[LoggedInUser]) extends UserRepo {
    override def getUserByName(name: String): Future[LoggedInUser] = Future.successful(users.find(_.login == name).getOrElse(LoggedInUser.guest))
  }

}
