plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "com.example.MainKt"
}

kotlin {
    jvmToolchain(21)
}

dependencies {

    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.resources)

    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.0.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.0.1")
    implementation("io.ktor:ktor-server-auth-jvm:3.0.1")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:3.0.1")
    implementation("io.ktor:ktor-server-cors-jvm:3.0.1")

    implementation(libs.logback.classic)
    
    // Ktor Client for Gemini API
    implementation("io.ktor:ktor-client-cio:3.0.1")
    implementation("io.ktor:ktor-client-content-negotiation:3.0.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.1")

    implementation("org.jetbrains.exposed:exposed-core:0.55.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.55.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.55.0")
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("com.h2database:h2:2.2.224")
    implementation("at.favre.lib:bcrypt:0.10.2")

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
}