package edu.learn.taskprocessor.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.cdimascio.dotenv.dotenv

object AppConfig {
    private val dotenv by lazy {
        dotenv {
            directory = "."
            ignoreIfMalformed = true
            ignoreIfMissing = true
        }
    }

    val instance: Config by lazy {
        ConfigFactory.systemProperties()
            .withFallback(ConfigFactory.systemEnvironment())
            .withFallback(dotEnvConfig())
            .withFallback(ConfigFactory.load())
            .resolve()
    }

    fun booleanFromEnvOrConfig(envKey: String, config: Config, configPath: String): Boolean {
        return System.getProperty(envKey)?.toBoolean()
            ?: System.getenv(envKey)?.toBoolean()
            ?: dotenv[envKey]?.toBoolean()
            ?: config.getBoolean(configPath)
    }

    private fun dotEnvConfig(): Config = try {
        ConfigFactory.parseMap(
            dotenv.entries().associate { it.key to it.value }
        )
    } catch (_: Exception) {
        ConfigFactory.empty()
    }
}
