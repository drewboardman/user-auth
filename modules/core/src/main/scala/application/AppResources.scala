package application

import application.config.Data.{ AppConfig, PostgreSQLConfig }
import natchez.Trace.Implicits.noop
import cats.effect.{ ConcurrentEffect, ContextShift, Resource }
import io.chrisdavenport.log4cats.Logger
import skunk.{ Session, SessionPool }

final case class AppResources[F[_]](
    psql: Resource[F, Session[F]]
)

object AppResources {
  def make[F[_]: ConcurrentEffect: ContextShift: Logger](
      cfg: AppConfig
//    ex: ExecutionContext
  ): Resource[F, AppResources[F]] =
    for {
      psql <- mkPostgreSQLResource(cfg.postgreSQL)
    } yield AppResources.apply[F](psql)

  private def mkPostgreSQLResource[F[_]: ConcurrentEffect: ContextShift](
      c: PostgreSQLConfig
  ): SessionPool[F] =
    Session
      .pooled[F](
        host = c.host.value,
        port = c.port.value,
        user = c.user.value,
        database = c.database.value,
        max = c.max.value
      )
}
