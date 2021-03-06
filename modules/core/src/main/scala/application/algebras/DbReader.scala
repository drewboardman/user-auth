package application.algebras

import application.domain.Auth.{ GoogleUserId, LoginUser }

trait DbReader[F[_]] {
  def getUserByGoogleUserId(googleUserId: GoogleUserId): F[Option[LoginUser]]
}
