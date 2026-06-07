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

@Serializable
data class UsuarioRama1(
    val id: Int,
    val nombre: String,
    val rol: String,
    val status: String
)

@Serializable
data class AlmacenamientoDiego(
    val id: Int,
    val producto: String,
    val cantidad: Int,
    val ubicacion: String
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
                        ),
                        RouteInfo(
                            "GET",
                            "/api/rama1/usuarios",
                            "Consulta de usuarios Rama 1"
                        ),
                        RouteInfo(
                            "GET",
                            "/api/diego/almacenamiento",
                            "Inventario de almacenamiento Rama Diego"
                        )
                    )
                )
            )
        }

        get("/api/rama1/usuarios") {

            val listaUsuarios = listOf(
                UsuarioRama1(1, "Ambar Jezabel", "Rama 1", "Activo"),
                UsuarioRama1(2, "Diego López", "Rama Administrador", "Offline"),
                UsuarioRama1(3, "Gabi", "Rama 2", "Activo")
            )

            call.respond(
                HttpStatusCode.OK,
                listaUsuarios
            )
        }

        // ===== ENDPOINT DE DIEGO =====

        get("/api/diego/almacenamiento") {

            val inventario = listOf(
                AlmacenamientoDiego(
                    id = 1,
                    producto = "Albahaca",
                    cantidad = 25,
                    ubicacion = "Estante A"
                ),
                AlmacenamientoDiego(
                    id = 2,
                    producto = "Tomate",
                    cantidad = 40,
                    ubicacion = "Estante B"
                ),
                AlmacenamientoDiego(
                    id = 3,
                    producto = "Lechuga",
                    cantidad = 18,
                    ubicacion = "Refrigerador"
                ),
                AlmacenamientoDiego(
                    id = 4,
                    producto = "Cilantro",
                    cantidad = 30,
                    ubicacion = "Estante C"
                )
            )

            call.respond(
                HttpStatusCode.OK,
                inventario
            )
        }
    }
}