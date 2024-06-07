package config

import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.koin.core.annotation.Single

@Single
class DatabaseConfig(
    private val dbCredentials: DatabaseCredentials
) {

    fun start() = Database.connect(
        url = dbCredentials.url,
        driver = dbCredentials.driver,
        user = dbCredentials.user,
        password = dbCredentials.password
    )
        .also {
            Flyway.configure().dataSource(
                dbCredentials.url, dbCredentials.user, dbCredentials.password
            ).load().migrate()
        }
}