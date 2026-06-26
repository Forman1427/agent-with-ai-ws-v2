package edu.learn.taskprocessor.plugins

import com.typesafe.config.ConfigFactory
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

private val log = logger {}

fun Application.configureCors() {
    val config = ConfigFactory.load().getConfig("cors")
    val allowedHosts = config.getStringList("allowedHosts")

    install(CORS) {
        if (config.getBoolean("development")) {
            anyHost()
            log.warn { "CORS: anyHost() — only for local development" }
        } else {
            allowedHosts.forEach { allowHost(it, schemes = listOf("http", "https")) }
        }
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
    }
}