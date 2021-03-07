package application.domain

import io.estatico.newtype.macros.newtype

import java.util.UUID
import scala.util.control.NoStackTrace

object Auth {
  @newtype case class UserId(value: UUID)
  @newtype case class GoogleUserId(value: String) // the return of getSubject
  @newtype case class UserName(value: String)
  @newtype case class Email(value: String)

  case class LoginUser(
      userId: UserId,
      googleUserId: GoogleUserId,
      email: Email
  )

  sealed trait LoginResult
  case class UserCreated(user: LoginUser) extends LoginResult
  case class SuccessfulLogin(user: LoginUser) extends LoginResult
  case class UserDoesNotExist(googleUserId: GoogleUserId) extends LoginResult

  sealed trait LoginError extends NoStackTrace
  case class TokenVerificationError(message: String) extends LoginError
  case object TokenExpiredError extends LoginError
  case class UserNameInUse(googleUserId: GoogleUserId) extends LoginError
}
