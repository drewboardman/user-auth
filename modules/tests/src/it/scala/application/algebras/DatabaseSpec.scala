package application.algebras

import application.arbitrary._
import application.config.Data.PostgreSQLConfig
import application.dbSession.mkPostgreSQLResource
import application.domain.Auth.{Email, GoogleUserId, UserCreated}
import application.{CleanupIntegrationTests, RunMigrations}
import cats.effect.{IO, Resource}
import eu.timepit.refined.types.net.UserPortNumber
import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString
import skunk.Session
import suite.IOAssertion

class DatabaseSpec extends CleanupIntegrationTests with RunMigrations {
  val MaxTests: PropertyCheckConfigParam = MinSuccessful(1)

  override def dbConfig: PostgreSQLConfig = PostgreSQLConfig(
    NonEmptyString.unsafeFrom("localhost"),
    UserPortNumber.unsafeFrom(5432),
    NonEmptyString.unsafeFrom("postgres"),
    NonEmptyString.unsafeFrom("postgres"),
    NonEmptyString.unsafeFrom("user-auth"),
    PosInt.unsafeFrom(10)
    )
  override def sessionPool: Resource[IO, Session[IO]] = mkPostgreSQLResource[IO](dbConfig)
  override def tablesToCleanup = List("users")

  test("creates a user and logs them in") {
    forAll(MaxTests) { ( googleUserId: GoogleUserId, email: Email ) =>
      IOAssertion {
        val reader = LiveDbReader.make[IO](sessionPool)
        val writer = LiveDbWriter.make[IO](sessionPool)
        for {
          created <- writer.createNewUser(googleUserId, email)
          login <- reader.getUserByGoogleUserId(googleUserId)
        } yield {
          assert(created == UserCreated(created.user))
          assert(login.contains(created.user))
        }
      }
    }
  }

  test("creates and reads a session") {
    forAll(MaxTests) { ( googleUserId: GoogleUserId, email: Email ) =>
      IOAssertion {
        val reader = LiveDbReader.make[IO](sessionPool)
        val writer = LiveDbWriter.make[IO](sessionPool)
        for {
          created <- writer.createNewUser(googleUserId, email)
          token <- writer.createSession(created.user)
          usr <- reader.getUserByRefreshToken(token)
        } yield {
          assert(usr.contains(created.user))
        }
      }
    }
  }
}
