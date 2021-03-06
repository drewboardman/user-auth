package application.algebras

import application.domain.Auth.{ GoogleUserId, LoginUser }
import application.util.sharedcodecs.loginUserCodec
import application.util.skunkx._
import cats.effect.{ Resource, Sync }
import skunk._
import skunk.codec.all._
import skunk.implicits._

trait DbReader[F[_]] {
  def getUserByGoogleUserId(googleUserId: GoogleUserId): F[Option[LoginUser]]
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
}

private object DbReaderQueries {
  val selectUserByGoogleUserId: Query[GoogleUserId, LoginUser] = sql"""
         SELECT * FROM users_table
         WHERE google_user_id = ${varchar.cimap[GoogleUserId]};
       """.query(loginUserCodec)
}
