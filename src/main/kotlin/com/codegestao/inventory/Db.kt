
package com.codegestao.inventory

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

object Db {
    fun connectAndMigrate(): Database {
        val host = System.getenv("DB_HOST") ?: "localhost"
        val port = System.getenv("DB_PORT") ?: "5432"
        val name = System.getenv("DB_NAME") ?: "inventory"
        val user = System.getenv("DB_USER") ?: "postgres"
        val pass = System.getenv("DB_PASSWORD") ?: "postgres"
        val jdbcUrl = "jdbc:postgresql://$host:$port/$name"

        Flyway.configure()
            .dataSource(jdbcUrl, user, pass)
            .locations("classpath:db/migration")
            .load()
            .migrate()

        val cfg = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = user
            this.password = pass
            this.driverClassName = "org.postgresql.Driver"
            this.maximumPoolSize = 5
        }
        val ds = HikariDataSource(cfg)
        return Database.connect(ds)
    }
}
