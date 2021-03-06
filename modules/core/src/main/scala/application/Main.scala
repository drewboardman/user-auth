package application

import application.algebras.{LiveAuthVerifier, LiveGoogleVerificationWrapper}
import cats.effect.{ExitCode, IO, IOApp}
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger


object Main extends IOApp {
  implicit val logger:  SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] = {
    val authVerifier = LiveAuthVerifier.make[IO](new LiveGoogleVerificationWrapper)
    IO.pure(ExitCode.Success)
  }

}
