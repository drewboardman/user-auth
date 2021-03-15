package application.algebras

import application.domain.Security.GoogleSecurityAlert
import cats.effect.Sync
import dev.profunktor.auth.jwt.JwtToken
import org.http4s.client.Client

trait GoogleSecurity[F[_]] {
  def verifyToken(jwtToken: JwtToken): F[GoogleSecurityAlert]
}

object LiveGoogleSecurity {
  def make[F[_]: Sync](client: Client[F]) = new LiveGoogleSecurity[F](client)
}

final class LiveGoogleSecurity[F[_]: Sync] private (
  client: Client[F]
) extends GoogleSecurity[F] {
  override def verifyToken(jwtToken: JwtToken): F[GoogleSecurityAlert] = ???
}
