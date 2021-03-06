package application.algebras

import application.domain.Auth.{ LoginUser, UserId, UserName }
import application.domain.GoogleTokenAuthModels.GoogleTokenString
import application.effects.CommonEffects.MonadThrow
import cats.Functor
import cats.implicits._
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload

trait Login[F[_]] {
  def verifyIdToken(rawToken: GoogleTokenString): F[LoginUser]
}

object LiveLogin {
  def make[F[_]: Functor: MonadThrow](tokenVerifier: GoogleVerificationWrapper) = new LiveLogin[F](tokenVerifier)
}

final class LiveLogin[F[_]: Functor: MonadThrow] private (
    tokenVerifier: GoogleVerificationWrapper
) extends Login[F] {
  override def verifyIdToken(rawToken: GoogleTokenString): F[LoginUser] =
    MonadThrow[F]
      .fromEither(tokenVerifier.verify(rawToken))
      .map { googleIdToken =>
        val payload: Payload = googleIdToken.getPayload
        // do i care about the expiration at all?
        // need to use the Header.jwk stuff I think
        LoginUser(
          UserId(payload.getSubject),
          UserName(payload.get("name").asInstanceOf[String])
        )
      }
}
