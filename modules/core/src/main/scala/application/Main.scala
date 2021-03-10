package application

import application.algebras.{ LiveDbReader, LiveDbWriter, LiveGoogleVerificationWrapper, LiveLogin }
import application.domain.{ CookieService, LiveJwtWriter }
import application.http.HttpApi
import application.util.migrations
import cats.effect._
import cats.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.chrisdavenport.log4cats.{ Logger, SelfAwareStructuredLogger }
import org.http4s.Uri.Scheme
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext

object Main extends IOApp {
  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    config.loader[IO].flatMap { cfg =>
      val ex = ExecutionContext.global
      Logger[IO].info(s"Loaded config $cfg") >>
        AppResources.make[IO](cfg).use { resources =>
          val verifier  = LiveGoogleVerificationWrapper.make[IO]
          val dbReader  = LiveDbReader.make[IO](resources.psql)
          val dbWriter  = LiveDbWriter.make[IO](resources.psql)
          val jwtWriter = LiveJwtWriter.make[IO]
          val cookies   = CookieService[IO](cfg.cookieConfig.domain, cfg.cookieConfig.scheme === Scheme.https)
          val login     = LiveLogin.make[IO](verifier, dbReader, dbWriter)
          val api       = HttpApi.make[IO](login, dbReader, dbWriter, cookies, jwtWriter)

          for {
            _ <- migrations.migrateDatabase[IO](cfg.postgreSQL)
            _ <- BlazeServerBuilder[IO](ex)
                   .bindHttp(cfg.httpServerConfig.port.value, cfg.httpServerConfig.host.value)
                   .withHttpApp(api.httpApp)
                   .serve
                   .compile
                   .drain
          } yield ExitCode.Success
        }
    }
}
