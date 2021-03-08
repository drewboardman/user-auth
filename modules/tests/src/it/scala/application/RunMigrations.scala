package application

import application.config.Data.PostgreSQLConfig
import application.util.migrations
import cats.effect.IO
import org.scalatest.BeforeAndAfterAll
import suite.PureTestSuite

trait RunMigrations extends PureTestSuite with BeforeAndAfterAll {
  def dbConfig: PostgreSQLConfig

  override def beforeAll(): Unit =
    migrations
      .migrateDatabase[IO](dbConfig)
      .void
      .unsafeRunSync()

  override protected def afterAll(): Unit = super.afterAll()
}
