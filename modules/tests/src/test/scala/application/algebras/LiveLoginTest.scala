package application.algebras

import application.domain.Auth._
import application.domain._
import application.suite.PureTestSuite
import cats.effect.IO
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken

class LiveLoginTest extends PureTestSuite {

}

protected class TestGoogleVerification extends GoogleVerificationWrapper[IO] {
  override def verify(idToken: GoogleTokenAuthModels.GoogleTokenString): IO[GoogleIdToken] = ???
  override def getEmail(payload: GoogleIdToken.Payload): IO[Auth.Email] = ???
  override def getGoogleUserId(payload: GoogleIdToken.Payload): IO[Auth.GoogleUserId] = ???
}

protected class TestDbReader extends DbReader[IO] {
  override def getUserByGoogleUserId(googleUserId: Auth.GoogleUserId): IO[Option[LoginUser]] =
    IO.pure(None)
}

protected class TestDbWriter extends DbWriter[IO] {
  override def createNewUser(googleUserId: Auth.GoogleUserId, email: Auth.Email): IO[LoginResult] = ???
}
