package application.http.routes.auth

import application.algebras.{ DbReader, DbWriter, Login }
import application.domain.Auth._
import application.domain.GoogleTokenAuthModels.GoogleTokenString
import application.domain.{ CookieService, JwtWriter }
import application.effects.CommonEffects.MonadThrow
import application.http.json._
import cats.Defer
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.circe.{ toMessageSynax, JsonDecoder }
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

final class LoginRoutes[F[_]: Defer: JsonDecoder: MonadThrow](
    login: Login[F],
    dbReader: DbReader[F],
    dbWriter: DbWriter[F],
    cookies: CookieService[F],
    jwtWriter: JwtWriter[F]
) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/auth"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    // If the user has a refresh token, return a new JWT. Otherwise 403.
    case req @ POST -> Root / "refresh-token" =>
      cookies.findSessionToken(req).flatMap {
        case None      => Forbidden("Not logged in.")
        case Some(tok) =>
          dbReader.getUserByRefreshToken(tok).flatMap {
            case None    => Forbidden("Invalid session token.")
            case Some(u) => jwtWriter.create(u).flatMap(Ok(_))
          }
      }

    case req @ POST -> Root / "login" =>
      (for {
        token <- req.asJsonDecode[GoogleTokenString]
        loginResult <- login.login(token)
      } yield loginResult)
        .flatMap {
          case SuccessfulLogin(usr) =>
            for {
              refreshToken <- dbWriter.createSession(usr)
              cookie <- cookies.sessionCookie(refreshToken)
              jwt <- jwtWriter.create(usr)
              res <- Ok(jwt)
            } yield res.addCookie(cookie)
          case _                    => BadRequest()
        }
        .recoverWith {
          case _: LoginError => Forbidden()
        }

    case req @ POST -> Root / "createUser" =>
      (for {
        token <- req.asJsonDecode[GoogleTokenString]
        loginResult <- login.create(token)
      } yield loginResult)
        .flatMap {
          case UserCreated(usr) =>
            for {
              refreshToken <- dbWriter.createSession(usr)
              cookie <- cookies.sessionCookie(refreshToken)
              jwt <- jwtWriter.create(usr)
              res <- Created(jwt)
            } yield res.addCookie(cookie)
          case _                => BadRequest()
        }
        .recoverWith {
          case GoogleTokenVerificationError(_) => Forbidden()
          case GoogleUserIdAlreadyExists(_)    => Conflict()
        }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes // no middleware?
  )
}
