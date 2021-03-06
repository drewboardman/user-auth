package application.util

import application.domain.Auth.{ Email, GoogleUserId, LoginUser, UserId }
import skunk.codec.all.{ uuid, varchar }
import skunk.implicits.toIdOps
import skunk.{ ~, Codec }

object sharedcodecs {
  val loginUserCodec: Codec[LoginUser] =
    (uuid ~ varchar ~ varchar).imap {
      case id ~ googleUserId ~ email =>
        LoginUser(UserId(id), GoogleUserId(googleUserId), Email(email))
    }(loginUser => loginUser.userId.value ~ loginUser.googleUserId.value ~ loginUser.email.value)
}
