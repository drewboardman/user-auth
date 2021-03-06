package application.algebras

import application.domain.Auth.{ Email, GoogleUserId, LoginResult }

trait DbWriter[F[_]] {
  def createNewUser(googleUserId: GoogleUserId, email: Email): F[LoginResult]
}
