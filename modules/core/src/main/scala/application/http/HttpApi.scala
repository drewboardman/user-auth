package application.http

import application.algebras.Login
import application.http.routes.auth.LoginRoutes
import application.http.routes.version
import cats.effect.{ Concurrent, Timer }
import org.http4s.implicits._
import org.http4s.{ HttpApp, HttpRoutes }
import org.http4s.server.Router
import org.http4s.server.middleware.{ AutoSlash, CORS, RequestLogger, ResponseLogger, Timeout }

import scala.concurrent.duration.DurationInt

object HttpApi {
  def make[F[_]: Concurrent: Timer](login: Login[F]) = new HttpApi[F](login)
}

final class HttpApi[F[_]: Concurrent: Timer] private (
    login: Login[F]
) {
  private val loginRoutes = new LoginRoutes[F](login).routes

  private val openRoutes = loginRoutes

  private val routes = Router(
    version.v1 -> openRoutes
  )

  private val middleware: HttpRoutes[F] => HttpRoutes[F] = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    } andThen { http: HttpRoutes[F] =>
      CORS(http, CORS.DefaultCORSConfig)
    } andThen { http: HttpRoutes[F] =>
      Timeout(60.seconds)(http)
    }
  }

  private val loggers: HttpApp[F] => HttpApp[F] = {
    { http: HttpApp[F] =>
      RequestLogger.httpApp(true, true)(http)
    } andThen { http: HttpApp[F] =>
      ResponseLogger.httpApp(true, true)(http)
    }
  }

  val httpApp: HttpApp[F] = loggers(middleware(routes).orNotFound)
}
