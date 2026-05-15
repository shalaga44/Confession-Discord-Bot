package dev.shalaga44.storage

import dev.shalaga44.storage.tables.ConfessionsTable
import dev.shalaga44.storage.tables.GuildSettingsTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object DatabaseFactory {

    fun initialize(databaseUrl: String) {
        File("data").mkdirs()

        Database.connect(
            url = databaseUrl,
            driver = "org.sqlite.JDBC"
        )

        transaction {



            SchemaUtils.create(
                ConfessionsTable,
                GuildSettingsTable
            )
            exec( SchemaUtils.addMissingColumnsStatements(ConfessionsTable,
                GuildSettingsTable).joinToString(";")
            )
        }
    }
}