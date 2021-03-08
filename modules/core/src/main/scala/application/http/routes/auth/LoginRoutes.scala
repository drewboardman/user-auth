package application.http.routes.auth

import application.algebras.Login
import application.domain.Auth.{
  GoogleTokenVerificationError,
  GoogleUserIdAlreadyExists,
  LoginError,
  SuccessfulLogin,
  UserCreated
}
import application.domain.GoogleTokenAuthModels.GoogleTokenString
import application.effects.CommonEffects.MonadThrow
import cats.Defer
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.circe.{ toMessageSynax, JsonDecoder }
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import application.http.json._

final class LoginRoutes[F[_]: Defer: JsonDecoder: MonadThrow](
    login: Login[F]
) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/auth"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "login" =>
      (for {
        token <- req.asJsonDecode[GoogleTokenString]
        loginResult <- login.login(token)
      } yield loginResult)
        .flatMap {
          case SuccessfulLogin(_) => Ok() // add a jwt
          case _                  => BadRequest()
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
          case UserCreated(_) => Created() // add a jwt
          case _              => BadRequest()
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
