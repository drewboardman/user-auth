package application.domain

import io.estatico.newtype.macros.newtype

import scala.util.control.NoStackTrace

object Auth {
  @newtype case class UserId(value: String)
  @newtype case class GoogleUserId(value: String) // the return of getSubject
  @newtype case class UserName(value: String)
  @newtype case class Email(value: String)

  case class LoginUser(
      userId: UserId,
      userName: UserName,
      email: Email
  )

  sealed trait LoginResult
  case class UserCreated(user: LoginUser) extends LoginResult
  case class SuccessfulLogin(user: LoginUser) extends LoginResult
  case class FailedLogin(user: LoginUser) extends LoginResult

  sealed trait LoginError extends NoStackTrace
  case class TokenVerificationError(message: String) extends LoginError
  case object TokenExpiredError extends LoginError
}
