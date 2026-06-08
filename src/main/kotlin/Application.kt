package com.example

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*

fun Application.module(configureProductos: () -> Unit) {

    install(ContentNegotiation) {
        json()
    }

    configureRouting()
    configureProductos()
}