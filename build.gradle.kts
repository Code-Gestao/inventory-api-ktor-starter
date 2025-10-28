
plugins {
    application
    kotlin("jvm") version "1.9.24"
    id("io.ktor.plugin") version "2.3.9"
    id("org.flywaydb.flyway") version "10.18.2"
}

application {
    mainClass.set("com.codegestao.inventory.ServerKt")
}

repositories {
    mavenCentral()
}

val ktorVersion = "2.3.9"
val logbackVersion = "1.5.6"
val exposedVersion = "0.51.0"
val hikariVersion = "5.1.0"
val flywayVersion = "10.18.2"
val postgresDriver = "42.7.4"

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.postgresql:postgresql:$postgresDriver")

    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("io.ktor:ktor-client-java:$ktorVersion")
}

tasks.test {
    useJUnitPlatform()
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
