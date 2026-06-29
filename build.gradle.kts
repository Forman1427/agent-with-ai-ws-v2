plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.0"
    application
    id("com.gradleup.shadow") version "9.3.1"
}

group = "edu.learn.taskprocessor"
version = "0.0.1"

application {
    mainClass.set("edu.learn.taskprocessor.MainKt")
}

repositories {
    mavenCentral()
}

dependencies {
    val ktorVersion = "3.0.0"
    val logbackVersion = "1.5.7"
    val exposedVersion = "0.60.0"
    val hikariVersion = "5.1.0"
    val postgresVersion = "42.7.4"

    // ============ Ktor Server (HTTP сервер + плагины) ============
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-cors-jvm:${ktorVersion}")

    // ============ Ktor Client (HTTP-клиент для AI) ============
    implementation("io.ktor:ktor-client-core-jvm:${ktorVersion}")
    implementation("io.ktor:ktor-client-cio-jvm:${ktorVersion}")
    implementation("io.ktor:ktor-client-content-negotiation-jvm:${ktorVersion}")

    // ============ Kotlinx (корутины, сериализация, даты) ============
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

    // ============ База данных (PostgreSQL + Exposed ORM) ============
    implementation("org.postgresql:postgresql:${postgresVersion}")
    implementation("com.zaxxer:HikariCP:${hikariVersion}")
    implementation("org.jetbrains.exposed:exposed-core:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-jdbc:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-dao:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:${exposedVersion}")

    // ============ Логирование ============
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")

    // ============ Конфигурация (.env) ============
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

    // ============ Test ============

    // JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")

    // Ktor Test (in-memory сервер для интеграционных тестов)
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testImplementation("io.ktor:ktor-client-core-jvm:${ktorVersion}")
    testImplementation("io.ktor:ktor-client-content-negotiation-jvm:${ktorVersion}")

    // Моки
    testImplementation("io.mockk:mockk:1.14.6")

    // Coroutines test
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

    // In-memory БД для тестов PostgresTaskRepository
    testImplementation("com.h2database:h2:2.2.224")

    // Kotlin test assertions
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveBaseName.set("taskprocessor")
    archiveClassifier.set("")
    archiveVersion.set("")
    mergeServiceFiles()

    // Защита от дублей ресурсов при мердже
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}