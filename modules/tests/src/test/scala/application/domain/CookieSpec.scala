package application.domain

import application.config.Data.CookieDomain
import cats.effect.IO
import eu.timepit.refined.types.string.NonEmptyString
import org.http4s.Method.GET
import org.http4s.{ Request, RequestCookie }
import org.http4s.implicits.http4sLiteralsSyntax
import suite.PureTestSuite

class CookieSpec extends PureTestSuite {
  val testCookies: CookieService[IO] =
    CookieService[IO](CookieDomain(NonEmptyString.unsafeFrom("drew-domain")), secure = false)

  test("can get the cookie off of a request") {
    val cookie = RequestCookie("refresh-token", "hello world")
    val req    = Request[IO](GET, uri = uri"foo/").addCookie(cookie)
    testCookies
      .findCookie(req)
      .map(c => assert(c.contains(cookie)))
  }
}
