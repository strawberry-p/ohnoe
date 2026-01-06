package tech.tarakoshka.ohnoe_desktop

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

class DatabaseDriverFactory {
    fun createDriver(): SqlDriver {
        val databasePath = "ohnoe.db"
        val driver = JdbcSqliteDriver("jdbc:sqlite:$databasePath")

        if (!File(databasePath).exists()) {
            AppDatabase.Schema.create(driver)
        }
        return driver
    }
}