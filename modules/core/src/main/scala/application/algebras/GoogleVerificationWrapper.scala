package application.algebras

import application.domain.Auth.{ LoginError, TokenVerificationError }
import application.domain.GoogleTokenAuthModels.GoogleTokenString
import com.google.api.client.googleapis.auth.oauth2.{ GoogleIdToken, GoogleIdTokenVerifier }
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory

import scala.jdk.CollectionConverters._
import scala.util.{ Failure, Success, Try }

/** This exists so I can test things easier
  */
trait GoogleVerificationWrapper {
  def verify(idToken: GoogleTokenString): Either[LoginError, GoogleIdToken]
}

// TODO: add the audience list (as secret) to config and use that to verify
final class LiveGoogleVerificationWrapper extends GoogleVerificationWrapper {
  private val builder = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance)

  private val tokenVerifier: GoogleIdTokenVerifier =
    builder
      .setAudience(List("foo").asJava) // this should be encrypted
      .build()

  override def verify(idToken: GoogleTokenString): Either[LoginError, GoogleIdToken] =
    Try {
      tokenVerifier.verify(idToken.value)
    } match {
      case Success(value) =>
        if (value == null) {
          Left(TokenVerificationError("Token verification failed."))
        } else {
          Right(value)
        }
      case Failure(err)   =>
        Left(TokenVerificationError(s"Exception thrown during token verification process: ${err}"))
    }
}
