package dev.shalaga44.storage

import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import java.io.File

object DatabaseFactory {

    fun initialize(databaseUrl: String) {
        File("data").mkdirs()

        Database.connect(
            url = databaseUrl,
            driver = "org.sqlite.JDBC"
        )

        Flyway.configure()
            .dataSource(databaseUrl, null, null)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .load()
            .migrate()
    }
}