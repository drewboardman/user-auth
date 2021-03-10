package application.algebras

import application.domain.Auth.{
  Email,
  GoogleUserId,
  GoogleUserIdAlreadyExists,
  LoginResult,
  LoginUser,
  RefreshToken,
  UserCreated,
  UserId
}
import application.effects.CommonEffects.ApThrow
import application.effects.GenUUID
import application.util.sharedcodecs.loginUserCodec
import application.util.skunkx.CodecOps
import cats.effect.{ Resource, Sync }
import cats.implicits._
import skunk._
import skunk.implicits._
import skunk.codec.all._

import java.util.UUID

trait DbWriter[F[_]] {
  def createNewUser(googleUserId: GoogleUserId, email: Email): F[LoginResult]
  def createSession(loginUser: LoginUser): F[RefreshToken]
}

object LiveDbWriter {
  def make[F[_]: Sync: GenUUID: ApThrow](
      sessionPool: Resource[F, Session[F]]
  ): LiveDbWriter[F] = new LiveDbWriter[F](sessionPool)
}

final class LiveDbWriter[F[_]: Sync: GenUUID: ApThrow] private (
    sessionPool: Resource[F, Session[F]]
) extends DbWriter[F] {
  import application.algebras.DbWriterQueries._

  override def createNewUser(googleUserId: GoogleUserId, email: Email): F[LoginResult] =
    sessionPool.use { sn =>
      sn.prepare(insertUser).use { cmd =>
        val res: F[LoginResult] = for {
          userId <- GenUUID[F].make[UserId]
          loginUser = LoginUser(userId, googleUserId, email)
          user <- cmd.execute(loginUser).as(loginUser)
        } yield UserCreated(user)

        res
          .handleErrorWith {
            case SqlState.UniqueViolation(_) =>
              GoogleUserIdAlreadyExists(googleUserId).raiseError[F, LoginResult]
          }
      }
    }

  override def createSession(loginUser: LoginUser): F[RefreshToken] =
    sessionPool.use { sn =>
      sn.prepare(insertSession).use { pq =>
        for {
          refreshToken <- GenUUID[F].make[RefreshToken]
          token <- pq.unique(refreshToken.value ~ loginUser.userId.value)
        } yield token
      }
    }
}

private object DbWriterQueries {
  val insertUser: Command[LoginUser] =
    sql"""
         INSERT INTO users
         VALUES ($loginUserCodec)
       """.command

  val insertSession: Query[UUID ~ UUID, RefreshToken] =
    sql"""
         INSERT INTO sessions (refresh_token, user_id)
         VALUES ($uuid, $uuid)
         RETURNING refresh_token
       """.query(uuid.cimap[RefreshToken])
}
