package application.algebras

import application.domain.Auth._
import application.domain._
import application.arbitrary._
import application.domain.GoogleTokenAuthModels.GoogleTokenString
import suite.{ IOAssertion, PureTestSuite }
import cats.effect.IO
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload

class LiveLoginTest extends PureTestSuite {
  def dataGoogleVerification(
      googleIdToken: GoogleIdToken,
      email: Email,
      googleUserId: GoogleUserId
  ): TestGoogleVerification =
    new TestGoogleVerification {
      override def verify(idToken: GoogleTokenAuthModels.GoogleTokenString): IO[GoogleIdToken] = IO.pure(googleIdToken)
      override def getEmail(payload: Payload): IO[Email]                                       = IO.pure(email)
      override def getGoogleUserId(payload: Payload): IO[GoogleUserId]                         = IO.pure(googleUserId)
    }

  def dataDbReader(loginUser: LoginUser): TestDbReader =
    new TestDbReader {
      override def getUserByGoogleUserId(googleUserId: GoogleUserId): IO[Option[LoginUser]] = IO.pure(Some(loginUser))
    }

  def dataDbWriter(loginResult: LoginResult): TestDbWriter =
    new TestDbWriter {
      override def createNewUser(googleUserId: GoogleUserId, email: Email): IO[LoginResult] = IO.pure(loginResult)
    }

  test("successful login") {
    forAll {
      (
          googleIdToken: GoogleIdToken,
          loginUser: LoginUser,
          googleTokenString: GoogleTokenString,
          email: Email,
          googleUserId: GoogleUserId
      ) =>
        IOAssertion {
          val verifier = dataGoogleVerification(googleIdToken, email, googleUserId)
          val dbReader = dataDbReader(loginUser)
          val dbWriter = new TestDbWriter
          val login    = LiveLogin.make(verifier, dbReader, dbWriter)
          login
            .login(googleTokenString)
            .map { result =>
              assert(result == SuccessfulLogin(loginUser))
            }
        }
    }
  }
}

protected class TestGoogleVerification extends GoogleVerificationWrapper[IO] {
  def verify(idToken: GoogleTokenString): IO[GoogleIdToken]    = ???
  def getEmail(payload: Payload): IO[Auth.Email]               = ???
  def getGoogleUserId(payload: Payload): IO[Auth.GoogleUserId] = ???
}

protected class TestDbReader extends DbReader[IO] {
  def getUserByGoogleUserId(googleUserId: GoogleUserId): IO[Option[LoginUser]] = IO.pure(None)
}

protected class TestDbWriter extends DbWriter[IO] {
  def createNewUser(googleUserId: GoogleUserId, email: Email): IO[LoginResult] = ???
}
