// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package application.domain

import application.config.Data.CookieDomain
import application.domain.Auth.RefreshToken
import cats.data.OptionT
import cats.syntax.all._
import cats.{ Applicative, MonadError }
import org.http4s._

import java.util.UUID

trait CookieReader[F[_]] {

  /** Find the session cookie in `req`, if any. */
  def findCookie(req: Request[F]): F[Option[RequestCookie]]

  /** Find the session cookie in `req`, if any, and decode it as a SessionToken. If the cookie's content is
    * not a valid SessionToken an error will be raised in `F`.
    */
  def findSessionToken(req: Request[F]): F[Option[RefreshToken]]

  /** Find the session cookie in `req`, if any. */
  def findCookie(res: Response[F]): F[Option[ResponseCookie]]

  def getCookie(res: Response[F]): F[ResponseCookie]

  /** Find the session cookie in `res`, if any, and decode it as a SessionToken. If the cookie's content is
    * not a valid SessionToken an error will be raised in `F`.
    */
  def findSessionToken(res: Response[F]): F[Option[RefreshToken]]

  def getSessionToken(res: Response[F]): F[RefreshToken]

}

object CookieReader {

  private[domain] val CookieName = "refresh-token"

  def apply[F[_]: MonadError[*[_], Throwable]]: CookieReader[F] =
    new CookieReader[F] {

      def findCookie(req: Request[F]): F[Option[RequestCookie]] = req.cookies.find(_.name == CookieName).pure[F]

      def getCookie(res: Response[F]): F[ResponseCookie] =
        findCookie(res).flatMap(_.toRight(new RuntimeException(s"Missing cookie.")).liftTo[F])

      def findSessionToken(req: Request[F]): F[Option[RefreshToken]] =
        OptionT(findCookie(req)).semiflatMap { c =>
          Either.catchOnly[IllegalArgumentException](RefreshToken(UUID.fromString(c.content))).liftTo[F]
        }.value

      def findCookie(res: Response[F]): F[Option[ResponseCookie]] = res.cookies.find(_.name == CookieName).pure[F]

      def findSessionToken(res: Response[F]): F[Option[RefreshToken]] =
        OptionT(findCookie(res)).semiflatMap { c =>
          Either.catchOnly[IllegalArgumentException](RefreshToken(UUID.fromString(c.content))).liftTo[F]
        }.value

      def getSessionToken(res: Response[F]): F[RefreshToken] =
        findSessionToken(res).flatMap(_.toRight(new RuntimeException(s"Missing or invalid session token.")).liftTo[F])

    }
}

trait CookieWriter[F[_]] {

  /** Construct a non-expiring session cookie containing the specified token. */
  def sessionCookie(token: RefreshToken): F[ResponseCookie]

  /** Construct an empty and expired session cookie. */
  def removeCookie(res: Response[F]): F[Response[F]]

}

object CookieWriter {

  private[domain] val CookieName = CookieReader.CookieName

  def apply[F[_]: Applicative](
      domain: CookieDomain,
      secure: Boolean
  ): CookieWriter[F] =
    new CookieWriter[F] {

      val emptyCookie: ResponseCookie =
        ResponseCookie(
          name = CookieName,
          domain = Some(domain.value.value),
          content = "",
          sameSite = SameSite.Strict,
          secure = secure,
          httpOnly = secure,
          path = Some("/")
        )

      def sessionCookie(token: RefreshToken): F[ResponseCookie] =
        emptyCookie
          .copy(
            content = token.value.toString(),
            expires = Some(HttpDate.MaxValue)
          )
          .pure[F]

      def removeCookie(res: Response[F]): F[Response[F]] = res.putHeaders(emptyCookie.clearCookie).pure[F]

    }

}

trait CookieService[F[_]] extends CookieReader[F] with CookieWriter[F]

object CookieService {
  def apply[F[_]: MonadError[*[_], Throwable]](
      domain: CookieDomain,
      secure: Boolean
  ): CookieService[F] =
    new CookieService[F] {
      val reader                                                      = CookieReader[F]
      val writer                                                      = CookieWriter[F](domain, secure)
      def findCookie(req: Request[F]): F[Option[RequestCookie]]       = reader.findCookie(req)
      def getCookie(res: Response[F]): F[ResponseCookie]              = reader.getCookie(res)
      def findSessionToken(req: Request[F]): F[Option[RefreshToken]]  = reader.findSessionToken(req)
      def findCookie(res: Response[F]): F[Option[ResponseCookie]]     = reader.findCookie(res)
      def findSessionToken(res: Response[F]): F[Option[RefreshToken]] = reader.findSessionToken(res)
      def sessionCookie(token: RefreshToken): F[ResponseCookie]       = writer.sessionCookie(token)
      def getSessionToken(res: Response[F]): F[RefreshToken]          = reader.getSessionToken(res)
      def removeCookie(res: Response[F]): F[Response[F]]              = writer.removeCookie(res)
    }
}
