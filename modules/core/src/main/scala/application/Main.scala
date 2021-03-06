package application

import application.algebras.{ LiveDbReader, LiveDbWriter, LiveGoogleVerificationWrapper, LiveLogin }
import cats.effect.{ ExitCode, IO, IOApp }
import cats.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.chrisdavenport.log4cats.{ Logger, SelfAwareStructuredLogger }

object Main extends IOApp {
  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    config.loader[IO].flatMap { cfg =>
      Logger[IO].info(s"Loaded config $cfg") >>
        AppResources.make[IO](cfg).use { resources =>
          for {
            verifier <- LiveGoogleVerificationWrapper.make[IO]
            dbReader <- LiveDbReader.make[IO](resources.psql)
            dbWriter <- LiveDbWriter.make[IO](resources.psql)
            login <- LiveLogin.make[IO](verifier, dbReader, dbWriter)
          } yield ExitCode.Success
        }
    }
}
