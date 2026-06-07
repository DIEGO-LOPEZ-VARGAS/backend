plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "com.example.ApplicationKt"
}

kotlin {
    jvmToolchain(21)
}

dependencies {

    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.resources)

    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.5.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.5.0")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.5.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.5.0")

    implementation(libs.logback.classic)

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
}