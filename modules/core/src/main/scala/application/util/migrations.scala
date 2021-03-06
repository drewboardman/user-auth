package application.util

import application.config.Data.PostgreSQLConfig
import cats.effect.Sync
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult

object migrations {

  def migrateDatabase[F[_]: Sync](config: PostgreSQLConfig): F[MigrateResult] =
    Sync[F].delay {
      Flyway
        .configure()
        .locations("classpath:migrations")
        .dataSource(
          config.jdbcUrl,
          config.user.value,
          config.password.value
        )
        .baselineOnMigrate(true)
        .load()
        .migrate()
    }
}
