package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
@Serializable
data class LoginRequest(
    val usuario: String,
    val password: String
)

@Serializable
data class RouteInfo(
    val method: String,
    val path: String,
    val description: String
)

@Serializable
data class RailwayStatusResponse(
    val online: Boolean,
    val serverUrl: String,
    val latencyMs: Long,
    val routes: List<RouteInfo>
)

fun Application.configureRouting() {

    routing {

        get("/") {
            call.respondText("Servidor Albahaca activo")
        }

        post("/login") {

            val login = call.receive<LoginRequest>()

            if (
                login.usuario == "admin" &&
                login.password == "1234"
            ) {

                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "mensaje" to "Login correcto"
                    )
                )

            } else {

                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf(
                        "mensaje" to "Credenciales incorrectas"
                    )
                )
            }
        }

        get("/api/railway/ping") {

            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "status" to "pong"
                )
            )
        }

        get("/api/railway/status") {

            call.respond(
                RailwayStatusResponse(
                    online = true,
                    serverUrl = "http://localhost:8080",
                    latencyMs = 1,
                    routes = listOf(
                        RouteInfo(
                            "POST",
                            "/login",
                            "Inicio de sesión"
                        ),
                        RouteInfo(
                            "GET",
                            "/api/railway/ping",
                            "Prueba de conexión"
                        ),
                        RouteInfo(
                            "GET",
                            "/api/railway/status",
                            "Estado del servidor"
                        )
                    )
                )
            )
        }
    }
}