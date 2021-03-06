package application.domain

import io.estatico.newtype.macros.newtype

object GoogleTokenAuthModels {
  @newtype case class GoogleTokenString(value: String)
}
