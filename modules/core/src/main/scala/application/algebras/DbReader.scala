package application.algebras

import application.domain.Auth.{ GoogleUserId, LoginUser, RefreshToken }
import application.util.sharedcodecs.loginUserCodec
import application.util.skunkx._
import cats.effect.{ Resource, Sync }
import skunk._
import skunk.codec.all._
import skunk.implicits._

trait DbReader[F[_]] {
  def getUserByGoogleUserId(googleUserId: GoogleUserId): F[Option[LoginUser]]
  def getUserByRefreshToken(refreshToken: RefreshToken): F[Option[LoginUser]]
}

object LiveDbReader {
  def make[F[_]: Sync](sessionPool: Resource[F, Session[F]]) = new LiveDbReader[F](sessionPool)
}

final class LiveDbReader[F[_]: Sync] private (
    sessionPool: Resource[F, Session[F]]
) extends DbReader[F] {
  import DbReaderQueries._
  override def getUserByGoogleUserId(googleUserId: GoogleUserId): F[Option[LoginUser]] =
    sessionPool.use { session =>
      session.prepare(selectUserByGoogleUserId).use { prepared =>
        prepared.option(googleUserId)
      }
    }

  override def getUserByRefreshToken(refreshToken: RefreshToken): F[Option[LoginUser]] =
    sessionPool.use { session =>
      session.prepare(selectUserByRefreshToken).use { prepared =>
        prepared.option(refreshToken)
      }
    }
}

private object DbReaderQueries {
  val selectUserByRefreshToken: Query[RefreshToken, LoginUser] = sql"""
         SELECT
         u.user_id,
         u.google_user_id,
         u.email
         FROM sessions s
         JOIN users u on u.user_id = s.user_id
         WHERE refresh_token = ${uuid.cimap[RefreshToken]};
       """.query(loginUserCodec)

  val selectUserByGoogleUserId: Query[GoogleUserId, LoginUser] = sql"""
         SELECT * FROM users
         WHERE google_user_id = ${varchar.cimap[GoogleUserId]};
       """.query(loginUserCodec)
}
