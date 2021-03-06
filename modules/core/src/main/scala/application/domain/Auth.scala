package application.domain

import io.estatico.newtype.macros.newtype

import scala.util.control.NoStackTrace

object Auth {
  @newtype case class UserId(value: String)
  @newtype case class UserName(value: String)

  case class LoginUser(
      userId: UserId,
      userName: UserName
  )

  sealed trait LoginError extends NoStackTrace
  case class TokenVerificationError(message: String) extends LoginError
  case object TokenExpiredError extends LoginError
}
