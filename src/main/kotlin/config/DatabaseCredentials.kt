package config

import org.koin.core.annotation.Property
import org.koin.core.annotation.Single

@Single
data class DatabaseCredentials(
    @Property("database.url")
    val url: String,
    @Property("database.driver")
    val driver: String,
    @Property("database.user")
    val user: String,
    @Property("database.password")
    val password: String
)