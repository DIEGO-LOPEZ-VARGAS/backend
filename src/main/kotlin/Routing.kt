package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class LoginRequest(
    val usuario: String,
    val password: String
)

@Serializable
data class Fruta(
    val nombre: String,
    val cantidad: Int
)

@Serializable
data class Receta(
    val titulo: String,
    val ingredientes: String,
    val pasos: String
)

@Serializable
data class RouteInfo(
    val method: String,
    val path: String,
    val description: String
)

@Serializable
data class Producto(
    val id: Int,
    val nombre_producto: String,
    val cantidad: Int,
    val fecha_caducidad: String,
    val tipo_almacenamiento: String,
    val disponible: Boolean
)

@Serializable
data class ProductosResponse(
    val rama: String,
    val total: Int,
    val productos: List<Producto>
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
            val frutasGuardadas = transaction {
                Frutas.selectAll().map { it[Frutas.nombre] to it[Frutas.cantidad] }
            }
            val recetasGuardadas = transaction {
                Recetas.selectAll().map { it[Recetas.titulo] }
            }

            val html = """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Dashboard Albahaca</title>
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f7f6; margin: 0; padding: 20px; color: #333; }
                        .container { max-width: 900px; margin: auto; background: white; padding: 30px; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
                        h1 { color: #2e5a39; border-bottom: 2px solid #e8f0ea; padding-bottom: 10px; }
                        .status { display: inline-block; padding: 5px 15px; border-radius: 20px; font-weight: bold; background: #e8f5e9; color: #2e7d32; }
                        .card { margin-top: 20px; padding: 15px; border: 1px solid #eee; border-radius: 8px; }
                        table { width: 100%; border-collapse: collapse; margin-top: 10px; }
                        th, td { text-align: left; padding: 12px; border-bottom: 1px solid #eee; }
                        th { background-color: #f8faf9; color: #2e5a39; }
                        .method { font-weight: bold; font-size: 0.8em; padding: 3px 8px; border-radius: 4px; color: white; }
                        .get { background: #1565c0; } .post { background: #2e7d32; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>🌿 Albahaca Proyecto - Dashboard</h1>
                        <p>Estado del Servidor: <span class="status">● ONLINE (con base de datos)</span></p>
                        
                        <div style="display: flex; gap: 20px;">
                            <div class="card" style="flex: 1;">
                                <h3>🍎 Inventario de Frutas</h3>
                                <table>
                                    <thead><tr><th>Nombre</th><th>Cantidad</th></tr></thead>
                                    <tbody>
                                        ${if (frutasGuardadas.isEmpty()) "<tr><td colspan='2'>Vacío</td></tr>" 
                                          else frutasGuardadas.joinToString("") { "<tr><td>${it.first}</td><td>${it.second}</td></tr>" }}
                                    </tbody>
                                </table>
                            </div>

                            <div class="card" style="flex: 1;">
                                <h3>🍳 Recetas Registradas</h3>
                                <table>
                                    <thead><tr><th>Título</th></tr></thead>
                                    <tbody>
                                        ${if (recetasGuardadas.isEmpty()) "<tr><td>Vacío</td></tr>" 
                                          else recetasGuardadas.joinToString("") { "<tr><td>$it</td></tr>" }}
                                    </tbody>
                                </table>
                            </div>
                        </div>

                        <div class="card">
                            <h3>🔌 Endpoints Disponibles</h3>
                            <table>
                                <thead><tr><th>Método</th><th>Ruta</th><th>Descripción</th></tr></thead>
                                <tbody>
                                    <tr><td><span class="method get">GET</span></td><td>/api/frutas</td><td>Lista de inventario</td></tr>
                                    <tr><td><span class="method post">POST</span></td><td>/api/frutas</td><td>Guardar fruta</td></tr>
                                    <tr><td><span class="method get">GET</span></td><td>/api/recetas</td><td>Lista de recetas</td></tr>
                                    <tr><td><span class="method post">POST</span></td><td>/api/recetas</td><td>Guardar receta</td></tr>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </body>
                </html>
            """.trimIndent()
            call.respondText(html, ContentType.Text.Html)
        }

        // --- ENDPOINTS DE FRUTAS ---
        get("/api/frutas") {
            val frutas = transaction {
                Frutas.selectAll().map { "${it[Frutas.nombre]}: ${it[Frutas.cantidad]}" }
            }
            if (frutas.isEmpty()) {
                call.respondText("No hay registros.")
            } else {
                call.respondText(frutas.joinToString("\n"))
            }
        }

        post("/api/frutas") {
            val fruta = call.receive<Fruta>()
            transaction {
                Frutas.insert {
                    it[nombre] = fruta.nombre
                    it[cantidad] = fruta.cantidad
                }
            }
            call.respondText("Fruta guardada")
        }

        // --- ENDPOINTS DE RECETAS ---
        get("/api/recetas") {
            val recetas = transaction {
                Recetas.selectAll().map {
                    "TÍTULO: ${it[Recetas.titulo]}\nIngredientes: ${it[Recetas.ingredientes]}"
                }
            }
            if (recetas.isEmpty()) {
                call.respondText("No hay recetas aún.")
            } else {
                call.respondText(recetas.joinToString("\n---\n"))
            }
        }

        post("/api/recetas") {
            try {
                val receta = call.receive<Receta>()
                transaction {
                    Recetas.insert {
                        it[titulo] = receta.titulo
                        it[ingredientes] = receta.ingredientes
                        it[pasos] = receta.pasos
                    }
                }
                call.respondText("¡Receta guardada en el servidor!")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Error en el formato de receta")
            }
        }

        // --- ENDPOINTS (COMPRAS) ---
        get("/api/rama2/productos") {
            val lista = listOf(
                Producto(1, "Leche", 2, "2025-12-01", "refrigerador", true),
                Producto(2, "Arroz", 1, "2026-01-01", "despensa", true),
                Producto(3, "Manzanas", 5, "2025-11-15", "refrigerador", true)
            )
            call.respond(ProductosResponse("Rama 2 - Inventario", lista.size, lista))
        }

        get("/api/rama2/compras") {
            val lista = listOf(
                Producto(101, "Jabón", 3, "N/A", "despensa", true),
                Producto(102, "Pan", 2, "2025-05-25", "despensa", true),
                Producto(103, "Queso", 1, "2025-06-01", "refrigerador", true)
            )
            call.respond(ProductosResponse("Rama 2 - Compras", lista.size, lista))
        }

        // --- ENDPOINTS DE SISTEMA ---
        post("/login") {
            try {
                val request = call.receive<LoginRequest>()
                if (request.usuario == "admin" && request.password == "1234") {
                    call.respond(HttpStatusCode.OK, "Login exitoso")
                } else {
                    call.respond(HttpStatusCode.Unauthorized, "Credenciales incorrectas")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Error en el formato de login")
            }
        }

        get("/api/railway/status") {
            call.respond(
                RailwayStatusResponse(
                    online = true,
                    serverUrl = "https://backend-production-523ba.up.railway.app",
                    latencyMs = 1,
                    routes = listOf(
                        RouteInfo("POST", "/api/frutas", "Guardar Fruta"),
                        RouteInfo("POST", "/api/recetas", "Guardar Receta"),
                        RouteInfo("GET", "/api/railway/status", "Estado"),
                        RouteInfo("GET", "/api/railway/ping", "Ping")
                    )
                )
            )
        }

        get("/api/railway/ping") {
            call.respondText("pong")
        }
    }
}
