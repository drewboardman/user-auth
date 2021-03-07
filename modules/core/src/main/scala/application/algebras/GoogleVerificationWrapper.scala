package application.algebras

import application.domain.Auth.{ Email, GoogleTokenVerificationError, GoogleUserId }
import application.domain.GoogleTokenAuthModels.GoogleTokenString
import application.effects.CommonEffects.MonadThrow
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload
import com.google.api.client.googleapis.auth.oauth2.{ GoogleIdToken, GoogleIdTokenVerifier }
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory

import scala.jdk.CollectionConverters._
import scala.util.{ Failure, Success, Try }

trait GoogleVerificationWrapper[F[_]] {
  def verify(idToken: GoogleTokenString): F[GoogleIdToken]
  def getGoogleUserId(payload: Payload): F[GoogleUserId]
  def getEmail(payload: Payload): F[Email]
}

object LiveGoogleVerificationWrapper {
  def make[F[_]: MonadThrow] = new LiveGoogleVerificationWrapper[F]
}

// TODO: add the audience list (as secret) to config and use that to verify
final class LiveGoogleVerificationWrapper[F[_]: MonadThrow] extends GoogleVerificationWrapper[F] {
  private val builder = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance)

  private val tokenVerifier: GoogleIdTokenVerifier =
    builder
      .setAudience(List("foo").asJava) // this should be encrypted
      .build()

  override def verify(idToken: GoogleTokenString): F[GoogleIdToken] = {
    val res = Try {
      tokenVerifier.verify(idToken.value)
    } match {
      case Success(value) =>
        if (value == null) {
          Left(GoogleTokenVerificationError("Token verification failed."))
        } else {
          Right(value)
        }
      case Failure(err)   =>
        Left(GoogleTokenVerificationError(s"Exception thrown during token verification process: ${err}"))
    }

    MonadThrow[F].fromEither(res)
  }

  override def getGoogleUserId(payload: Payload): F[GoogleUserId] = {
    val subject = payload.getSubject
    val res     = if (subject == null) {
      Right(GoogleUserId(subject))
    } else {
      Left(
        GoogleTokenVerificationError("payload.getSubject failed. Google token payload contained null google userId.")
      )
    }

    MonadThrow[F].fromEither(res)
  }

  override def getEmail(payload: Payload): F[Email] = {
    val email = payload.getEmail
    val res   = if (email == null) {
      Right(Email(email))
    } else {
      Left(GoogleTokenVerificationError("payload.getEmail failed. Google token payload contained null email."))
    }

    MonadThrow[F].fromEither(res)
  }
}
