package com.example

import com.example.config.DatabaseFactory
import com.example.plugins.configureSecurity
import com.example.repository.ProductoRepository
import com.example.repository.RecetaRepository
import com.example.repository.UsuarioRepository
import com.example.routes.authRoutes
import com.example.routes.productRoutes
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.http.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    DatabaseFactory.init()
    
    install(CORS) {
        anyHost()
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
    }

    install(ContentNegotiation) {
        json()
    }
    
    configureSecurity()

    val userRepo = UsuarioRepository()
    val prodRepo = ProductoRepository()
    val recRepo = RecetaRepository()

    routing {
        authRoutes(userRepo)
        productRoutes(prodRepo, recRepo)
    }
}
