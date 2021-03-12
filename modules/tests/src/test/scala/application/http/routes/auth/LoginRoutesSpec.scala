package application.http.routes.auth

import application.algebras.{ DbReader, DbWriter, Login }
import application.arbitrary._
import application.domain.Auth._
import application.domain.GoogleTokenAuthModels.GoogleTokenString
import application.domain.{ Auth, CookieService, JwtWriter }
import application.http.json._
import cats.effect.IO
import dev.profunktor.auth.jwt.JwtToken
import org.http4s.Method.POST
import org.http4s._
import org.http4s.implicits.http4sLiteralsSyntax
import suite.{ HttpTestSuite, IOAssertion }

class LoginRoutesSpec extends HttpTestSuite {

  def dataLogin(loginResult: LoginResult): TestLogin =
    new TestLogin {
      override def create(rawToken: GoogleTokenString): IO[LoginResult] = IO.pure(loginResult)
      override def login(rawToken: GoogleTokenString): IO[LoginResult]  = IO.pure(loginResult)
    }

  def dataWriter(refreshToken: RefreshToken): TestDbWriter =
    new TestDbWriter {
      override def createSession(loginUser: LoginUser): IO[RefreshToken] = IO.pure(refreshToken)
    }

  def dataReader(loginUser: LoginUser): TestDbReader =
    new TestDbReader {
      override def getUserByRefreshToken(refreshToken: RefreshToken): IO[Option[LoginUser]] = IO.pure(Some(loginUser))
    }

  def dataJwt(jwt: JwtToken): TestJwtWriter =
    new TestJwtWriter {
      override def create(loginUser: LoginUser): IO[JwtToken] = IO.pure(jwt)
    }

  def dataCookies(refreshToken: RefreshToken): TestCookies =
    new TestCookies {
      override def findSessionToken(req: Request[IO]): IO[Option[RefreshToken]] = IO.pure(Some(refreshToken))
    }

  val verificationFailed: TestLogin = new TestLogin {
    override def create(rawToken: GoogleTokenString): IO[LoginResult] =
      IO.raiseError(GoogleTokenVerificationError("test"))
    override def login(rawToken: GoogleTokenString): IO[LoginResult]  =
      IO.raiseError(GoogleTokenVerificationError("test"))
  }

  test("POST login [Ok]") {
    forAll { (loginUser: LoginUser, googleTokenString: GoogleTokenString, token: RefreshToken) =>
      IOAssertion {
        val request: Request[IO] = Request[IO](method = POST, uri = uri"/auth/login").withEntity(googleTokenString)
        val login                = dataLogin(SuccessfulLogin(loginUser))
        val reader               = new TestDbReader
        val writer               = dataWriter(token)
        val cookies              = new TestCookies
        val jwt                  = new TestJwtWriter
        val routes               = new LoginRoutes[IO](login, reader, writer, cookies, jwt).routes
        assertHttpStatus(routes, request)(Status.Ok)
      }
    }
  }

  test("POST login [Forbidden]") {
    forAll { (googleTokenString: GoogleTokenString) =>
      IOAssertion {
        val request: Request[IO] = Request[IO](method = POST, uri = uri"/auth/login").withEntity(googleTokenString)
        val reader               = new TestDbReader
        val writer               = new TestDbWriter
        val cookies              = new TestCookies
        val jwt                  = new TestJwtWriter
        val routes               = new LoginRoutes[IO](verificationFailed, reader, writer, cookies, jwt).routes
        assertHttpStatus(routes, request)(Status.Forbidden)
      }
    }
  }

  test("POST create [Ok]") {
    forAll { (loginUser: LoginUser, googleTokenString: GoogleTokenString, token: RefreshToken) =>
      IOAssertion {
        val request: Request[IO] = Request[IO](method = POST, uri = uri"/auth/createUser").withEntity(googleTokenString)
        val login                = dataLogin(UserCreated(loginUser))
        val reader               = new TestDbReader
        val writer               = dataWriter(token)
        val cookies              = new TestCookies
        val jwt                  = new TestJwtWriter
        val routes               = new LoginRoutes[IO](login, reader, writer, cookies, jwt).routes
        assertHttpStatus(routes, request)(Status.Created) // change to assert body and cookie
      }
    }
  }

  test("POST create [Forbidden]") {
    forAll { (googleTokenString: GoogleTokenString) =>
      IOAssertion {
        val request: Request[IO] = Request[IO](method = POST, uri = uri"/auth/createUser").withEntity(googleTokenString)
        val reader               = new TestDbReader
        val writer               = new TestDbWriter
        val cookies              = new TestCookies
        val jwt                  = new TestJwtWriter
        val routes               = new LoginRoutes[IO](verificationFailed, reader, writer, cookies, jwt).routes
        assertHttpStatus(routes, request)(Status.Forbidden)
      }
    }
  }

  test("POST refresh-token [Ok]") {
    forAll { (loginUser: LoginUser, refreshToken: RefreshToken, jwtToken: JwtToken) =>
      IOAssertion {
        val request: Request[IO] =
          Request[IO](method = POST, uri = uri"/auth/refresh-token")
            .withEntity("foo")
            .addCookie(RequestCookie("token", refreshToken.value.toString))
        val login                = new TestLogin
        val reader               = dataReader(loginUser)
        val writer               = new TestDbWriter
        val cookies              = dataCookies(refreshToken)
        val jwt                  = dataJwt(jwtToken)
        val routes               = new LoginRoutes[IO](login, reader, writer, cookies, jwt).routes
        assertHttp[JwtToken](routes, request)(Status.Ok, jwtToken)
      }
    }
  }
}

protected class TestLogin extends Login[IO] {
  def create(rawToken: GoogleTokenString): IO[LoginResult] = ???
  def login(rawToken: GoogleTokenString): IO[LoginResult]  = ???
}

protected class TestDbReader extends DbReader[IO] {
  def getUserByGoogleUserId(googleUserId: GoogleUserId): IO[Option[LoginUser]]      = IO.pure(None)
  def getUserByRefreshToken(refreshToken: Auth.RefreshToken): IO[Option[LoginUser]] = IO.pure(None)
}

protected class TestDbWriter extends DbWriter[IO] {
  def createNewUser(googleUserId: GoogleUserId, email: Email): IO[LoginResult] = ???
  def createSession(loginUser: LoginUser): IO[RefreshToken]                    = ???
}

protected class TestCookies extends CookieService[IO] {
  override def findCookie(req: Request[IO]): IO[Option[RequestCookie]]      = IO.pure(None)
  override def findSessionToken(req: Request[IO]): IO[Option[RefreshToken]] = IO.pure(None)
  override def removeCookie(res: Response[IO]): IO[Response[IO]]            = IO.pure(Response.notFound)
  override def sessionCookie(token: RefreshToken): IO[ResponseCookie]       = IO.pure(ResponseCookie("key", "test"))
}

protected class TestJwtWriter extends JwtWriter[IO] {
  override def create(loginUser: LoginUser): IO[JwtToken] = IO.pure(JwtToken("test"))
}
