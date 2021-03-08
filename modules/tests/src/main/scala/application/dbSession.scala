package application

import application.config.Data.PostgreSQLConfig
import cats.effect.{ Concurrent, ContextShift, Resource }
import natchez.Trace.Implicits.noop
import skunk.Session

object dbSession {
  def mkPostgreSQLResource[F[_]: Concurrent: ContextShift](
      c: PostgreSQLConfig
  ): Resource[F, Session[F]] =
    Session
      .single[F](
        host = c.host.value,
        port = c.port.value,
        user = c.user.value,
        database = c.database.value
      )

}
