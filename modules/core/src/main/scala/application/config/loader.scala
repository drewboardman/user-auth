package application.config

import application.config.Data.{ AppConfig, CookieConfig, CookieDomain, HttpServerConfig, PostgreSQLConfig }
import application.domain.Auth.{ JwtSecretKeyConfig, TokenExpiration }
import cats.effect._
import cats.syntax.all._
import ciris._
import ciris.refined._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.net.UserPortNumber
import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString
import org.http4s.Uri.Scheme

import scala.concurrent.duration.DurationInt

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
    env("PSQL_MAX_CONNECTIONS").as[PosInt].default(PosInt.unsafeFrom(10)),
    env("HTTP_SERVER_HOST").as[NonEmptyString].default(NonEmptyString.unsafeFrom("0.0.0.0")),
    env("HTTP_SERVER_PORT").as[UserPortNumber].default(UserPortNumber.unsafeFrom(8080)),
    env("COOKIE_DOMAIN").as[NonEmptyString].default(NonEmptyString.unsafeFrom("local.user-auth")),
    env("SCHEME").as[String].default("http"),
    env("JWT_SECRET_KEY")
      .as[NonEmptyString]
      .default(
        NonEmptyString.unsafeFrom("Mj8%fJAJ&jMRHUa&%#ew9yym32tX!klqamprF8oBfHBerQe7x6qeJL%zxfDU9vnO")
      )
      .secret,
    env("JWT_EXPIRATION").as[Int].default(15)
  ).parMapN { (psqlHost, psqlPort, user, pass, db, max, serverHost, serverPort, domain, scheme, secret, exp) =>
    AppConfig(
      PostgreSQLConfig(psqlHost, psqlPort, user, pass, db, max),
      HttpServerConfig(serverHost, serverPort),
      CookieConfig(CookieDomain(domain), Scheme.unsafeFromString(scheme)), // TODO: fix scheme stuff
      JwtSecretKeyConfig(secret),
      TokenExpiration(exp.minutes)
    )
  }
}
