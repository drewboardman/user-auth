package application.algebras

import application.domain.Auth._
import application.domain.GoogleTokenAuthModels.GoogleTokenString
import application.effects.CommonEffects.MonadThrow
import cats.Functor
import cats.implicits._

trait Login[F[_]] {
  def login(rawToken: GoogleTokenString): F[LoginResult]
  def create(rawToken: GoogleTokenString): F[LoginResult]
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
    for {
      (id, _) <- checkToken(rawToken)
      maybeUser <- dbReader.getUserByGoogleUserId(id)
    } yield (id, maybeUser) match {
      case (_, Some(user)) => SuccessfulLogin(user)
      case (id, None)      => UserDoesNotExist(id)
    }

  override def create(rawToken: GoogleTokenString): F[LoginResult] =
    for {
      (id, email) <- checkToken(rawToken)
      user <- dbWriter.createNewUser(id, email)
    } yield user

  private def checkToken(rawToken: GoogleTokenString): F[(GoogleUserId, Email)] =
    for {
      token <- tokenVerifier.verify(rawToken)
      payload = token.getPayload
      googleUserId <- tokenVerifier.getGoogleUserId(payload)
      email <- tokenVerifier.getEmail(payload)
      // TODO: check the header for jwk also
      // TODO: check the header for iss also
    } yield (googleUserId, email)
}
