package application.effects

import application.effects.CommonEffects.ApThrow

import java.util.UUID
import cats.effect.Sync
import cats.syntax.all._
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._

/** This is a typeclass that will generate UUIDs. We use it to create
  * primary keys for the psql table elements.
  * @tparam F
  */
trait GenUUID[F[_]] {
  def make: F[UUID]
  def make[A: Coercible[UUID, *]]: F[A]
  def read[A: Coercible[UUID, *]](str: String): F[A]
}

object GenUUID {
  def apply[F[_]](implicit ev: GenUUID[F]): GenUUID[F] = ev

  implicit def syncGenUUID[F[_]: Sync]: GenUUID[F] =
    new GenUUID[F] {
      def make: F[UUID] = Sync[F].delay(UUID.randomUUID())

      def make[A: Coercible[UUID, *]]: F[A] = make.map(_.coerce[A])

      def read[A: Coercible[UUID, *]](str: String): F[A] = ApThrow[F].catchNonFatal(UUID.fromString(str).coerce[A])
    }
}
