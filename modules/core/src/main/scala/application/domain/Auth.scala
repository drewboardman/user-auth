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
  case object TokenVerificationError extends LoginError
  case object TokenExpiredError extends LoginError
}
