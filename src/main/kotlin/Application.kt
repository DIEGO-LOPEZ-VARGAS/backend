package com.example

import com.example.config.DatabaseFactory
import com.example.plugins.configureSecurity
import com.example.repository.ProductoRepository
import com.example.repository.RecetaRepository
import com.example.repository.UsuarioRepository
import com.example.routes.authRoutes
import com.example.routes.productRoutes
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.respondText
import kotlinx.coroutines.launch

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    // Inicialización asíncrona para evitar errores 502 por bloqueos en el arranque
    launch {
        try {
            DatabaseFactory.init()
        } catch (e: Exception) {
            println("ADVERTENCIA: Reintentando conexión a la base de datos en segundo plano...")
        }
    }
    
    install(CORS) {
        anyHost()
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
    }

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
        })
    }
    
    configureSecurity()

    val userRepo = UsuarioRepository()
    val prodRepo = ProductoRepository()
    val recRepo = RecetaRepository()

    routing {
        get("/") {
            call.respondText("Albahaca Server Online")
        }
        authRoutes(userRepo)
        productRoutes(prodRepo, recRepo)
    }
}
