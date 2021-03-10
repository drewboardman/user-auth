package application.config

import eu.timepit.refined.types.all.{ PosInt, UserPortNumber }
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype
import org.http4s.Uri.Scheme

object Data {
  @newtype case class RedisURI(value: NonEmptyString)
  @newtype case class RedisConfig(uri: RedisURI)
  @newtype case class CookieDomain(value: NonEmptyString)

  case class AppConfig(
      postgreSQL: PostgreSQLConfig,
      httpServerConfig: HttpServerConfig,
      cookieConfig: CookieConfig
  )

  case class CookieConfig(
      domain: CookieDomain,
      scheme: Scheme
  )

  case class HttpServerConfig(
      host: NonEmptyString,
      port: UserPortNumber
  )

  case class PostgreSQLConfig(
      host: NonEmptyString,
      port: UserPortNumber,
      user: NonEmptyString,
      password: NonEmptyString,
      database: NonEmptyString,
      max: PosInt
  ) {
    def jdbcUrl: String = s"jdbc:postgresql://${host}:${port}/${database}"
  }
}
