package application.domain

import application.domain.Auth.{ JwtSecretKeyConfig, LoginUser, TokenExpiration }
import cats.effect.Sync
import cats.implicits._
import dev.profunktor.auth.jwt.{ jwtEncode, JwtSecretKey, JwtToken }
import io.circe.syntax._
import pdi.jwt.{ JwtAlgorithm, JwtClaim }
import application.http.json._

import scala.concurrent.duration.FiniteDuration

trait JwtWriter[F[_]] {
  def create(loginUser: LoginUser): F[JwtToken]
}

object LiveJwtWriter {
  def make[F[_]: Sync](
      tokenConfig: JwtSecretKeyConfig,
      tokenExpiration: TokenExpiration
  ): F[JwtWriter[F]] =
    Sync[F].delay(java.time.Clock.systemUTC).map { implicit jClock =>
      new LiveJwtWriter[F](tokenConfig, tokenExpiration.value)
    }
}

final class LiveJwtWriter[F[_]: Sync] private (
    config: JwtSecretKeyConfig,
    exp: FiniteDuration
)(implicit val ev: java.time.Clock)
    extends JwtWriter[F] {
  override def create(loginUser: LoginUser): F[JwtToken] =
    for {
      claim <- Sync[F].delay(JwtClaim(loginUser.asJson.noSpaces).issuedNow.expiresIn(exp.toMillis))
      secretKey = JwtSecretKey(config.value.value.value)
      token <- jwtEncode[F](claim, secretKey, JwtAlgorithm.HS256)
    } yield token
}
