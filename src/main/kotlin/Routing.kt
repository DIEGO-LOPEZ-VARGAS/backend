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
data class RailwayStatusResponse(
    val online: Boolean,
    val serverUrl: String,
    val latencyMs: Long,
    val routes: List<RouteInfo>
)

// Listas en memoria para guardar datos temporalmente
val listaFrutas = mutableListOf<Fruta>()
val listaRecetas = mutableListOf<Receta>()

fun Application.configureRouting() {

    routing {

        get("/") {
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
                        <p>Estado del Servidor: <span class="status">● ONLINE</span></p>
                        
                        <div style="display: flex; gap: 20px;">
                            <div class="card" style="flex: 1;">
                                <h3>🍎 Inventario de Frutas</h3>
                                <table>
                                    <thead><tr><th>Nombre</th><th>Cantidad</th></tr></thead>
                                    <tbody>
                                        ${if (listaFrutas.isEmpty()) "<tr><td colspan='2'>Vacío</td></tr>" 
                                          else listaFrutas.joinToString("") { "<tr><td>${it.nombre}</td><td>${it.cantidad}</td></tr>" }}
                                    </tbody>
                                </table>
                            </div>

                            <div class="card" style="flex: 1;">
                                <h3>🍳 Recetas Registradas</h3>
                                <table>
                                    <thead><tr><th>Título</th></tr></thead>
                                    <tbody>
                                        ${if (listaRecetas.isEmpty()) "<tr><td>Vacío</td></tr>" 
                                          else listaRecetas.joinToString("") { "<tr><td>${it.titulo}</td></tr>" }}
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
            if (listaFrutas.isEmpty()) {
                call.respondText("No hay registros.")
            } else {
                call.respondText(listaFrutas.joinToString("\n") { "${it.nombre}: ${it.cantidad}" })
            }
        }

        post("/api/frutas") {
            val fruta = call.receive<Fruta>()
            listaFrutas.add(fruta)
            call.respondText("Fruta guardada")
        }

        // --- ENDPOINTS DE RECETAS (EXAMEN) ---
        get("/api/recetas") {
            if (listaRecetas.isEmpty()) {
                call.respondText("No hay recetas aún.")
            } else {
                call.respondText(listaRecetas.joinToString("\n---\n") { "TÍTULO: ${it.titulo}\nIngredientes: ${it.ingredientes}" })
            }
        }

        post("/api/recetas") {
            try {
                val receta = call.receive<Receta>()
                listaRecetas.add(receta)
                call.respondText("¡Receta guardada en el servidor!")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Error en el formato de receta")
            }
        }

        // --- ENDPOINTS DE SISTEMA ---
        get("/api/railway/status") {
            call.respond(
                RailwayStatusResponse(
                    online = true,
                    serverUrl = "http://localhost:8080",
                    latencyMs = 1,
                    routes = listOf(
                        RouteInfo("POST", "/api/frutas", "Guardar Fruta"),
                        RouteInfo("POST", "/api/recetas", "Guardar Receta"),
                        RouteInfo("GET", "/api/railway/status", "Estado")
                    )
                )
            )
        }
    }
}