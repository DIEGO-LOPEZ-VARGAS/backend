package com.example.routes

import com.example.dtos.*
import com.example.repository.ProductoRepository
import com.example.repository.RecetaRepository
import com.example.models.Actividades
import com.example.models.Frutas
import com.example.models.Recetas
import com.example.models.Compras
import com.example.config.DatabaseFactory.dbQuery
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDateTime

@Serializable
data class ActividadDto(
    val id: Int,
    val accion: String,
    val detalle: String,
    val fecha: String
)

fun Route.productRoutes(
    prodRepo: ProductoRepository,
    recRepo: RecetaRepository
) {
    val geminiClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            json(jsonConfig)
        }
    }
    val geminiApiKey = System.getenv("GEMINI_API_KEY")?.trim() ?: ""

    authenticate("auth-jwt") {

        // --- ACTIVIDADES ---
        get("/api/actividades") {
            val list = dbQuery {
                Actividades.selectAll().map {
                    ActividadDto(
                        id = it[Actividades.id],
                        accion = it[Actividades.accion],
                        detalle = it[Actividades.detalle],
                        fecha = it[Actividades.fecha].toString()
                    )
                }
            }
            call.respond(list)
        }

        // --- FRUTAS (ADMIN ONLY) ---
        get("/api/frutas") {
            call.respond(prodRepo.allFrutas())
        }

        post("/api/frutas") {
            val f = call.receive<FrutaDto>()
            prodRepo.addFruta(f)

            // Registrar actividad
            val userEmail = call.userEmail()
            dbQuery {
                Actividades.insert {
                    it[accion] = "Añadir Fruta"
                    it[detalle] = "Añadió ${f.nombre} (${f.cantidad}kg) - Por $userEmail"
                }
            }

            call.respond(HttpStatusCode.Created, "Guardado")
        }

        // --- RECETAS (USER/ADMIN) ---
        get("/api/recetas") {
            call.respond(recRepo.allRecetas())
        }

        post("/api/recetas") {
            val r = call.receive<RecetaDto>()
            recRepo.addReceta(r)
            call.respond(HttpStatusCode.Created, "Guardada")
        }

        // --- IA ---
        post("/api/recetas/ia") {
            try {
                if (geminiApiKey.isBlank()) {
                    println("DEPURACION_IA: ERROR - La variable GEMINI_API_KEY está vacía en Railway")
                    call.respond(HttpStatusCode.InternalServerError, "Error: Configuración de IA incompleta (falta API Key)")
                    return@post
                }

                val request = call.receive<IngredientsRequest>()
                println("DEPURACION_IA: Recibidos ingredientes: ${request.ingredientes}")

                val prompt = "Genera una receta JSON con { \"titulo\": \"...\", \"ingredientes\": \"...\", \"pasos\": \"...\" } usando: ${request.ingredientes.joinToString()}"

                // URL base sin la llave expuesta
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"
                println("DEPURACION_IA: Llamando a Gemini API v1beta con cabecera de seguridad...")

                val res = geminiClient.post(url) {
                    contentType(ContentType.Application.Json)
                    // Enviamos la llave en la cabecera, que es más seguro y robusto
                    header("x-goog-api-key", geminiApiKey)
                    setBody(buildJsonObject {
                        putJsonArray("contents") {
                            addJsonObject {
                                putJsonArray("parts") {
                                    addJsonObject { put("text", prompt) }
                                }
                            }
                        }
                    })
                }

                if (res.status == HttpStatusCode.OK) {
                    val body = res.body<JsonObject>()
                    val text = body["candidates"]?.jsonArray?.get(0)?.jsonObject
                        ?.get("content")?.jsonObject
                        ?.get("parts")?.jsonArray?.get(0)?.jsonObject
                        ?.get("text")?.jsonPrimitive?.content ?: ""

                    val json = text.replace("```json", "").replace("```", "").trim()
                    try {
                        val receta = Json.decodeFromString<RecetaDto>(json)
                        call.respond(receta)
                    } catch (e: Exception) {
                        println("DEPURACION_IA: Error al parsear JSON: ${e.message}")
                        call.respond(HttpStatusCode.InternalServerError, "Error al procesar la respuesta de la IA")
                    }
                } else {
                    val errorBody = res.bodyAsText()
                    println("DEPURACION_IA: Error de Gemini (${res.status}): $errorBody")
                    call.respond(HttpStatusCode.InternalServerError, "Google Gemini Error (${res.status}): $errorBody")
                }
            } catch (e: Exception) {
                println("DEPURACION_IA: Excepción en el endpoint: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Error desconocido")
            }
        }

        // --- COMPRAS (USER) ---
        get("/api/compras") {
            val list = prodRepo.allCompras()
            call.respond(ProductosResponse("Lista Personal", list.size, list))
        }

        post("/api/compras") {
            val p = call.receive<ProductoDto>()
            prodRepo.addCompra(p)
            call.respond(HttpStatusCode.Created, "Agregado")
        }
    }
}

fun ApplicationCall.userEmail(): String? {
    val principal = principal<JWTPrincipal>()
    return principal?.payload?.getClaim("email")?.asString()
}
