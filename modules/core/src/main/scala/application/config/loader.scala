package application.config

import application.config.Data.{ AppConfig, PostgreSQLConfig }
import cats.effect._
import cats.syntax.all._
import ciris._
import ciris.refined._
import eu.timepit.refined.types.net.UserPortNumber
import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString

object loader {
  // this allows you to call `config.loader`
  def apply[F[_]: Async: ContextShift]: F[AppConfig] = default.load[F]

  // TODO: use env vars here for client secret
  private val default: ConfigValue[AppConfig] = (
    env("PSQL_HOST").as[NonEmptyString].default(NonEmptyString.unsafeFrom("localhost")),
    env("PSQL_PORT").as[UserPortNumber].default(UserPortNumber.unsafeFrom(5432)),
    env("PSQL_USER").as[NonEmptyString].default(NonEmptyString.unsafeFrom("postgres")),
    env("PSQL_PASSWORD").as[NonEmptyString].default(NonEmptyString.unsafeFrom("postgres")),
    env("PSQL_DATABASE").as[NonEmptyString].default(NonEmptyString.unsafeFrom("user-auth")),
    env("PSQL_MAX_CONNECTIONS").as[PosInt].default(PosInt.unsafeFrom(10))
  ).parMapN { (host, port, user, pass, db, max) =>
    AppConfig(
      PostgreSQLConfig(host, port, user, pass, db, max)
    )
  }
}
