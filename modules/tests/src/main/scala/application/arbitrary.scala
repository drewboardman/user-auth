package application

import application.domain.Auth.{ Email, GoogleUserId, LoginUser }
import application.domain.GoogleTokenAuthModels.GoogleTokenString
import application.generators._
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import org.scalacheck.{ Arbitrary, Gen }

import java.util.UUID

object arbitrary {
  implicit def arbCoercibleInt[A: Coercible[Int, *]]: Arbitrary[A]    = Arbitrary(Gen.posNum[Int].map(_.coerce[A]))
  implicit def arbCoercibleStr[A: Coercible[String, *]]: Arbitrary[A] = Arbitrary(coerceGenStr[A])
  implicit def arbCoercibleUUID[A: Coercible[UUID, *]]: Arbitrary[A]  = Arbitrary(coerceGenUuid[A])

  implicit val arbGoogleIdToken: Arbitrary[GoogleIdToken]         = Arbitrary(googleIdTokenGen)
  implicit val arbGoogleTokenString: Arbitrary[GoogleTokenString] = Arbitrary(googleTokenStringGen)
  implicit val arbLoginUser: Arbitrary[LoginUser]                 = Arbitrary(loginUserGen)
  implicit val arbEmail: Arbitrary[Email]                         = Arbitrary(coerceGenStr[Email])
  implicit val arbGoogleUserId: Arbitrary[GoogleUserId]           = Arbitrary(coerceGenStringyInt[GoogleUserId])
}
