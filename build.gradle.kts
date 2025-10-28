
plugins {
    application
    kotlin("jvm") version "1.9.24"
    id("io.ktor.plugin") version "2.3.9"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.flywaydb.flyway") version "10.18.2"
}

application {
    mainClass.set("com.codegestao.inventory.ServerKt")
}

repositories { mavenCentral() }

val ktorVersion = "2.3.9"
val logbackVersion = "1.5.6"
val exposedVersion = "0.51.0"
val hikariVersion = "5.1.0"
val flywayVersion = "10.18.2"
val postgresDriver = "42.7.4"

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:2.3.9")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.9")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.9")
    implementation("io.ktor:ktor-serialization-jackson-jvm:2.3.9")
    implementation("io.ktor:ktor-server-call-logging-jvm:2.3.9")
    implementation("ch.qos.logback:logback-classic:1.5.6")

    // EXPOSED — todas as partes e **java-time**
    implementation("org.jetbrains.exposed:exposed-core:0.51.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.51.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.51.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.51.0")

    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.postgresql:postgresql:42.7.4")

    implementation("org.flywaydb:flyway-core:10.18.2")
    implementation("org.flywaydb:flyway-database-postgresql:10.18.2")
    implementation("org.jetbrains.exposed:exposed-core:0.51.0")

}


tasks.test { useJUnitPlatform() }

tasks.shadowJar {
    archiveClassifier.set("all")
    mergeServiceFiles()
}

flyway {
    val host = System.getenv("DB_HOST") ?: "localhost"
    val port = System.getenv("DB_PORT") ?: "5432"
    val name = System.getenv("DB_NAME") ?: "inventory"
    val user = System.getenv("DB_USER") ?: "postgres"
    val pass = System.getenv("DB_PASSWORD") ?: "postgres"
    url = "jdbc:postgresql://$host:$port/$name"
    this.user = user
    this.password = pass
    locations = arrayOf("filesystem:src/main/resources/db/migration")
}
