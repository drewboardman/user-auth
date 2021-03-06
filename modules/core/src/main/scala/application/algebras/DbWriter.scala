package application.algebras

import application.algebras.DbWriterQueries.insertUser
import application.domain.Auth.{ Email, GoogleUserId, LoginResult, LoginUser, UserCreated, UserId, UserNameInUse }
import application.effects.CommonEffects.ApThrow
import application.effects.GenUUID
import application.util.sharedcodecs.loginUserCodec
import cats.effect.{ Resource, Sync }
import cats.implicits._
import skunk.implicits.toStringOps
import skunk.{ Command, Session, SqlState }

trait DbWriter[F[_]] {
  def createNewUser(googleUserId: GoogleUserId, email: Email): F[LoginResult]
}

object LiveDbWriter {
  def make[F[_]: Sync: GenUUID: ApThrow](
      sessionPool: Resource[F, Session[F]]
  ): LiveDbWriter[F] = new LiveDbWriter[F](sessionPool)
}

final class LiveDbWriter[F[_]: Sync: GenUUID: ApThrow] private (
    sessionPool: Resource[F, Session[F]]
) extends DbWriter[F] {
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
              UserNameInUse(googleUserId).raiseError[F, LoginResult]
          }
      }
    }
}

private object DbWriterQueries {
  val insertUser: Command[LoginUser] =
    sql"""
         INSERT INTO users
         VALUES ($loginUserCodec)
       """.command
}
