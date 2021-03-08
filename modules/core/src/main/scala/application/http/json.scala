package application.http

import application.domain.GoogleTokenAuthModels.GoogleTokenString
import cats.Applicative
import io.circe._
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

object json extends JsonCodecs {
  implicit def deriveEntityEncoder[F[_]: Applicative, A: Encoder]: EntityEncoder[F, A] = jsonEncoderOf[F, A]
}

private[http] trait JsonCodecs {
  // ----- Coercible codecs -----
  implicit def coercibleDecoder[A: Coercible[B, *], B: Decoder]: Decoder[A] = Decoder[B].map(_.coerce[A])

  implicit def coercibleEncoder[A: Coercible[B, *], B: Encoder]: Encoder[A] =
    Encoder[B].contramap(_.repr.asInstanceOf[B]) // this casting is bc the Scala compiler can't infer the type of repr

  implicit def coercibleKeyDecoder[A: Coercible[B, *], B: KeyDecoder]: KeyDecoder[A] = KeyDecoder[B].map(_.coerce[A])

  implicit def coercibleKeyEncoder[A: Coercible[B, *], B: KeyEncoder]: KeyEncoder[A] =
    KeyEncoder[B]
      .contramap[A](_.repr.asInstanceOf[B]) // this casting is bc the Scala compiler can't infer the type of repr

  implicit val googleTokenStringCodec: Codec[GoogleTokenString] =
    Codec.forProduct1(nameA0 = "google_id_token")(GoogleTokenString.apply)(_.value)
}
