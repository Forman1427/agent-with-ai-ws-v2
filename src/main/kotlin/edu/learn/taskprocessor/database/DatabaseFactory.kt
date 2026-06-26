package edu.learn.taskprocessor.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.typesafe.config.ConfigFactory
import edu.learn.taskprocessor.repository.tables.TasksTable
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

private val log = logger {}

object DatabaseFactory {
    private lateinit var dataSource: HikariDataSource

    @Synchronized
    fun init() {
        if (this::dataSource.isInitialized) {
            log.info { "Database already initialized" }
            return
        }

        val config = ConfigFactory.load().getConfig("database")

        val jdbcUrl = config.getString("url")
        val driver = config.getString("driver")
        val user = config.getString("user")
        val password = config.getString("password")
        val maximumPoolSize = config.getInt("maximumPoolSize")

        val hikariConfig = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.driverClassName = driver
            this.username = user
            this.password = password
            this.maximumPoolSize = maximumPoolSize
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        dataSource = try {
            HikariDataSource(hikariConfig)
        } catch (e: Exception) {
            log.error(e) { "Failed to create connection pool: $jdbcUrl" }
            throw e
        }
        Database.connect(dataSource)

        try {
            transaction { exec("SELECT 1") }
            log.info { "Database connection verified: $jdbcUrl" }
        } catch (e: Exception) {
            log.error(e) { "Database is not available: $jdbcUrl" }
        }

        transaction {
            SchemaUtils.create(TasksTable)
        }
    }

    fun close() {
        if (this::dataSource.isInitialized) {
            dataSource.close()
            log.info { "Database connection pool closed" }
        }
    }
}