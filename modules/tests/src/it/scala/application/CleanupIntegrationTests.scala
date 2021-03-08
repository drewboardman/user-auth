package application

import cats.effect.{IO, Resource}
import cats.implicits.toFoldableOps
import org.scalatest.BeforeAndAfterEach
import skunk._
import skunk.implicits._
import suite.PureTestSuite


trait CleanupIntegrationTests extends PureTestSuite with BeforeAndAfterEach {
  def sessionPool: Resource[IO, Session[IO]]
  def tablesToCleanup: List[String]

  override def afterEach(): Unit = {
    super.afterEach()

    sessionPool.use { sn =>
      tablesToCleanup.traverse_ { table =>
        sn.execute(truncate(table))
      }
    }.unsafeRunSync()
  }

  def truncate(table: String): Command[Void] = sql"TRUNCATE TABLE #$table".command
}