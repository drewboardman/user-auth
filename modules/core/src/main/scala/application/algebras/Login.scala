package application.algebras

import application.domain.Auth._
import application.domain.GoogleTokenAuthModels.GoogleTokenString
import application.effects.CommonEffects.MonadThrow
import cats.Functor
import cats.implicits._

trait Login[F[_]] {
  def login(rawToken: GoogleTokenString): F[LoginResult]
}

object LiveLogin {
  def make[F[_]: Functor: MonadThrow](
      tokenVerifier: GoogleVerificationWrapper[F],
      dbReader: DbReader[F],
      dbWriter: DbWriter[F]
  ) = new LiveLogin[F](tokenVerifier, dbReader, dbWriter)
}

final class LiveLogin[F[_]: Functor: MonadThrow] private (
    tokenVerifier: GoogleVerificationWrapper[F],
    dbReader: DbReader[F],
    dbWriter: DbWriter[F]
) extends Login[F] {
  override def login(rawToken: GoogleTokenString): F[LoginResult] =
    // TODO: figure out a way to ask new users for more login information like username
    for {
      token <- tokenVerifier.verify(rawToken)
      payload = token.getPayload
      googleUserId <- tokenVerifier.getGoogleUserId(payload)
      email <- tokenVerifier.getEmail(payload)
      // TODO: check the header for jwk also
      result <- createOrReturnCurrentUser(googleUserId, email)
    } yield result

  private def createOrReturnCurrentUser(googleUserId: GoogleUserId, email: Email): F[LoginResult] =
    dbReader.getUserByGoogleUserId(googleUserId).flatMap {
      case Some(user) => (SuccessfulLogin(user): LoginResult).pure[F]
      case None       => dbWriter.createNewUser(googleUserId, email)
    }
}
