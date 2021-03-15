package application.domain

import application.domain.Auth.GoogleUserId
import dev.profunktor.auth.jwt.JwtToken

object Security {
  case class GoogleSecurityAlert(
    googleUserId: GoogleUserId,
    event: GoogleSecurityEvent,
    token: JwtToken
  )

  sealed trait GoogleSecurityEvent // use enumeratum maybe
}
