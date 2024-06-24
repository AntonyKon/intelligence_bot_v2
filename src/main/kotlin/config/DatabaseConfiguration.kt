package config

import dev.inmo.kslog.common.logger
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.vendors.SQLiteDialect
import org.koin.core.annotation.Single
import java.sql.Connection.TRANSACTION_READ_UNCOMMITTED

@Single
class DatabaseConfiguration(
    private val dbCredentials: DatabaseCredentials
) {

    fun start() = DatabaseConfig.invoke {
        this.sqlLogger = StdOutSqlLogger
    }.let {
        Database.connect(
            url = dbCredentials.url,
            driver = dbCredentials.driver,
            user = dbCredentials.user,
            password = dbCredentials.password,
            databaseConfig = it
        )
    }
        .also {
            Flyway.configure().dataSource(
                dbCredentials.url, dbCredentials.user, dbCredentials.password
            ).load().migrate()
        }
}