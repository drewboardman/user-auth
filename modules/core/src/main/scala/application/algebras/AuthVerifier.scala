package application.algebras

import application.domain.Auth.{LoginUser, TokenVerificationError, UserId, UserName}
import cats.MonadError
import cats.effect.Sync
import cats.implicits.catsSyntaxApplicativeId
import com.google.api.client.auth.openidconnect.{IdToken, IdTokenVerifier}

trait AuthVerifier[F[_]] {
  def verifyIdToken(idToken: IdToken): F[LoginUser]
}

object LiveAuthVerifier {
  def make[F[_]: Sync](tokenVerifier: GoogleVerificationWrapper) = new LiveAuthVerifier[F](tokenVerifier)
}

final class LiveAuthVerifier[F[_]: Sync] private (
  tokenVerifier: GoogleVerificationWrapper
) extends AuthVerifier[F] {
  override def verifyIdToken(idToken: IdToken): F[LoginUser] =
    if(tokenVerifier.verify(idToken)) {
      val payload: IdToken.Payload = idToken.getPayload
      // do i care about the expiration at all?
      // need to use the Header.jwk stuff I think
      LoginUser(
        UserId(payload.getSubject),
        UserName(payload.get("name").asInstanceOf[String])
      ).pure[F]
    } else
      MonadError[F, Throwable].raiseError(TokenVerificationError)
}
