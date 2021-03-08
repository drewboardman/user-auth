package application.http.routes.auth

import application.algebras.Login
import application.arbitrary._
import application.domain.Auth.{ LoginResult, LoginUser, SuccessfulLogin }
import application.domain.GoogleTokenAuthModels.GoogleTokenString
import application.http.json._
import cats.effect.IO
import org.http4s.Method.POST
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{ Request, Status }
import suite.{ HttpTestSuite, IOAssertion }

class LoginRoutesSpec extends HttpTestSuite {

  def dataLogin(loginResult: LoginResult): TestLogin =
    new TestLogin {
      override def create(rawToken: GoogleTokenString): IO[LoginResult] = IO.pure(loginResult)
      override def login(rawToken: GoogleTokenString): IO[LoginResult]  = IO.pure(loginResult)
    }

  test("POST login [Ok]") {
    forAll { (loginUser: LoginUser, googleTokenString: GoogleTokenString) =>
      IOAssertion {
        val request: Request[IO] = Request[IO](method = POST, uri = uri"/auth/login").withEntity(googleTokenString)
        val result               = SuccessfulLogin(loginUser)
        val routes               = new LoginRoutes[IO](dataLogin(result)).routes
        assertHttpStatus(routes, request)(Status.Ok)
      }
    }
  }
}

protected class TestLogin extends Login[IO] {
  def create(rawToken: GoogleTokenString): IO[LoginResult] = ???
  def login(rawToken: GoogleTokenString): IO[LoginResult]  = ???
}
