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

  val userDoesNotExistReader: TestDbReader = new TestDbReader {
    override def getUserByGoogleUserId(googleUserId: GoogleUserId): IO[Option[LoginUser]] = IO.pure(None)
  }

  val failWriter: TestDbWriter = new TestDbWriter {
    override def createNewUser(googleUserId: GoogleUserId, email: Email): IO[LoginResult] =
      IO.raiseError(GoogleUserIdAlreadyExists(googleUserId))
  }

  val failVerifier: TestGoogleVerification = new TestGoogleVerification {
    override def verify(idToken: GoogleTokenString): IO[GoogleIdToken] =
      IO.raiseError(GoogleTokenVerificationError("test"))
  }

  def dataDbWriter(loginResult: LoginResult): TestDbWriter =
    new TestDbWriter {
      override def createNewUser(googleUserId: GoogleUserId, email: Email): IO[LoginResult] = IO.pure(loginResult)
    }

  // ------------ login ------------------
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

  test("login user does not exist") {
    forAll {
      (
          googleIdToken: GoogleIdToken,
          googleTokenString: GoogleTokenString,
          email: Email,
          googleUserId: GoogleUserId
      ) =>
        IOAssertion {
          val verifier = dataGoogleVerification(googleIdToken, email, googleUserId)
          val dbReader = userDoesNotExistReader
          val dbWriter = new TestDbWriter
          val login    = LiveLogin.make(verifier, dbReader, dbWriter)
          login
            .login(googleTokenString)
            .attempt
            .map {
              case Left(err) => assert(err == UserDoesNotExist(googleUserId))
              case Right(_)  => fail("expected UserDoesNotExist")
            }
        }
    }
  }

  test("login: google token fails to verify") {
    forAll { (googleTokenString: GoogleTokenString) =>
      IOAssertion {
        val verifier = failVerifier
        val dbReader = new TestDbReader
        val dbWriter = new TestDbWriter
        val login    = LiveLogin.make(verifier, dbReader, dbWriter)
        login
          .login(googleTokenString)
          .attempt
          .map {
            case Left(err) => assert(err == GoogleTokenVerificationError("test"))
            case Right(_)  => fail("expected google verification failure")
          }
      }
    }
  }

  // ------------- create new user ------------------
  test("successful user creation") {
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
          val dbReader = new TestDbReader
          val created  = UserCreated(loginUser)
          val dbWriter = dataDbWriter(created)
          val login    = LiveLogin.make(verifier, dbReader, dbWriter)
          login
            .create(googleTokenString)
            .map { result =>
              assert(result == created)
            }
        }
    }
  }

  test("user already exists") {
    forAll {
      (
          googleIdToken: GoogleIdToken,
          googleTokenString: GoogleTokenString,
          email: Email,
          googleUserId: GoogleUserId
      ) =>
        IOAssertion {
          val verifier = dataGoogleVerification(googleIdToken, email, googleUserId)
          val dbWriter = failWriter
          val dbReader = new TestDbReader
          val login    = LiveLogin.make(verifier, dbReader, dbWriter)
          login
            .create(googleTokenString)
            .attempt
            .map {
              case Left(err) => assert(err == GoogleUserIdAlreadyExists(googleUserId))
              case Right(_)  => fail("expected google user id to already exist error.")
            }
        }
    }
  }

  test("create: google token fails to verify") {
    forAll { (googleTokenString: GoogleTokenString) =>
      IOAssertion {
        val verifier = failVerifier
        val dbReader = new TestDbReader
        val dbWriter = new TestDbWriter
        val login    = LiveLogin.make(verifier, dbReader, dbWriter)
        login
          .create(googleTokenString)
          .attempt
          .map {
            case Left(err) => assert(err == GoogleTokenVerificationError("test"))
            case Right(_)  => fail("expected google verification failure")
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
