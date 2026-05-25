plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    id("io.ktor.plugin") version "3.4.3"
    application
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    mainClass.set("com.learnapp.ApplicationKt")
}

dependencies {
    implementation("io.ktor:ktor-server-core:3.4.3")
    implementation("io.ktor:ktor-server-netty:3.4.3")
    implementation("io.ktor:ktor-server-content-negotiation:3.4.3")
    implementation("io.ktor:ktor-server-auth:3.4.3")
    implementation("io.ktor:ktor-server-auth-jwt:3.4.3")
    implementation("io.ktor:ktor-server-status-pages:3.4.3")
    implementation("io.ktor:ktor-server-cors:3.4.3")
    implementation("io.ktor:ktor-server-call-logging:3.4.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.4.3")

    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:5.5.1")
    implementation("org.mongodb:bson-kotlinx:5.5.1")

    implementation("com.google.firebase:firebase-admin:8.1.0")

    implementation("ch.qos.logback:logback-classic:1.5.32")

    testImplementation("io.ktor:ktor-server-test-host:3.4.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.2.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(24)
}