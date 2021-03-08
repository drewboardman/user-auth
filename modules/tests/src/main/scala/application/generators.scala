package application

import application.domain.Auth.{ Email, GoogleUserId, LoginUser, UserId }
import application.domain.GoogleTokenAuthModels.GoogleTokenString
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload
import com.google.api.client.json.webtoken.JsonWebSignature.Header
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen

import java.util.UUID

object generators {

  //------------- helpers -------------------
  val genNonEmptyString: Gen[String] =
    Gen
      .chooseNum(10, 30)
      .flatMap { n =>
        Gen.buildableOfN[String, Char](n, Gen.alphaChar)
      }

  def coerceGenUuid[A: Coercible[UUID, *]]: Gen[A]         = Gen.uuid.map(_.coerce[A])
  def coerceGenStr[A: Coercible[String, *]]: Gen[A]        = genNonEmptyString.map(_.coerce[A])
  def coerceGenInt[A: Coercible[Int, *]]: Gen[A]           = Gen.posNum[Int].map(_.coerce[A])
  def coerceGenStringyInt[A: Coercible[String, *]]: Gen[A] = genStringyInt.map(_.coerce[A])

  val genStringyInt: Gen[String] = Gen.posNum[Int].map(_.toString)

  val genLong: Gen[Long] = Gen.posNum[Int].map(_.toLong)

  def genBytes(howMany: Int): Gen[Array[Byte]] =
    Gen
      .listOfN(howMany, arbitrary[Byte])
      .map(_.toArray)

  //---------------- domain ----------------------
  val loginUserGen: Gen[LoginUser] = for {
    userId <- coerceGenUuid[UserId]
    googleUserId <- Gen.resize(25, coerceGenStringyInt[GoogleUserId])
    email <- coerceGenStr[Email]
  } yield LoginUser(userId, googleUserId, email)

  //---------------- google ----------------------
  val googleTokenStringGen: Gen[GoogleTokenString] =
    coerceGenStr[GoogleTokenString]

  val googleTokenHeaderGen: Gen[Header] = {
    for {
      jwk <- genNonEmptyString
    } yield (new Header).set("jwk", jwk.asInstanceOf[Object])
  }

  val googleTokenPayloadGen: Gen[Payload] = for {
    exp <- genLong
    nbf <- genLong
    iat <- genLong
    iss <- genNonEmptyString
    aud <- genNonEmptyString
    jti <- Gen.uuid.map(_.toString)
    typ <- genNonEmptyString
    sub <- Gen.uuid.map(_.toString)
  } yield (new Payload)
    .set("exp", exp.asInstanceOf[Object])
    .set("nbf", nbf.asInstanceOf[Object])
    .set("iat", iat.asInstanceOf[Object])
    .set("iss", iss.asInstanceOf[Object])
    .set("aud", aud.asInstanceOf[Object])
    .set("jti", jti.asInstanceOf[Object])
    .set("typ", typ.asInstanceOf[Object])
    .set("sub", sub.asInstanceOf[Object])

  val googleIdTokenGen: Gen[GoogleIdToken] = for {
    header <- googleTokenHeaderGen
    payload <- googleTokenPayloadGen
    signature <- genBytes(35)
    signedContent <- genBytes(35)
  } yield (new GoogleIdToken(header, payload, signature, signedContent))
}
