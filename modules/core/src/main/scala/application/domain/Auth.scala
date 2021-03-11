package application.domain

import ciris.Secret
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype

import java.util.UUID
import scala.concurrent.duration.FiniteDuration
import scala.util.control.NoStackTrace

object Auth {
  @newtype case class RefreshToken(value: UUID)
  @newtype case class UserId(value: UUID)
  @newtype case class GoogleUserId(value: String) // the return of getSubject
  @newtype case class UserName(value: String)
  @newtype case class Email(value: String)
  @newtype case class TokenExpiration(value: FiniteDuration)
  @newtype case class JwtSecretKeyConfig(value: Secret[NonEmptyString])

  case class LoginUser(
      userId: UserId,
      googleUserId: GoogleUserId,
      email: Email
  )

  sealed trait LoginResult { def user: LoginUser }
  case class UserCreated(user: LoginUser) extends LoginResult
  case class SuccessfulLogin(user: LoginUser) extends LoginResult

  sealed trait LoginError extends NoStackTrace
  case class GoogleTokenVerificationError(message: String) extends LoginError
  case class GoogleUserIdAlreadyExists(googleUserId: GoogleUserId) extends LoginError
  case class UserDoesNotExist(googleUserId: GoogleUserId) extends LoginError
}
