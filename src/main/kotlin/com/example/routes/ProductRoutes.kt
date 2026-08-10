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
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import io.ktor.utils.io.*
import kotlinx.io.readByteArray
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

// Cliente global para IA para evitar crear uno nuevo por cada petición
private val geminiClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        val jsonConfig = Json {
            ignoreUnknownKeys = true
        }
        json(jsonConfig)
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 90000 // 90 segundos para la IA
        connectTimeoutMillis = 90000
        socketTimeoutMillis = 90000
    }
}

fun Route.productRoutes(
    prodRepo: ProductoRepository,
    recRepo: RecetaRepository
) {
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
            val userId = call.userId()
            call.respond(prodRepo.allFrutas(userId))
        }

        post("/api/frutas") {
            val userId = call.userId()
            val f = call.receive<FrutaDto>()
            prodRepo.addFruta(f, userId)

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

        delete("/api/frutas/{id}") {
            val userId = call.userId()
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            if (prodRepo.deleteFruta(id, userId)) call.respond(HttpStatusCode.OK)
            else call.respond(HttpStatusCode.NotFound)
        }

        put("/api/frutas/{id}") {
            val userId = call.userId()
            val id = call.parameters["id"]?.toIntOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest)
            val f = call.receive<FrutaDto>()
            if (prodRepo.updateFruta(id, f, userId)) call.respond(HttpStatusCode.OK)
            else call.respond(HttpStatusCode.NotFound)
        }

        // --- RECETAS (USER/ADMIN) ---
        get("/api/recetas") {
            val userId = call.userId()
            call.respond(recRepo.allRecetas(userId))
        }

        post("/api/recetas") {
            val userId = call.userId()
            val r = call.receive<RecetaDto>()
            recRepo.addReceta(r, userId)
            call.respond(HttpStatusCode.Created, "Guardada")
        }

        delete("/api/recetas/{id}") {
            val userId = call.userId()
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            if (recRepo.deleteReceta(id, userId)) call.respond(HttpStatusCode.OK)
            else call.respond(HttpStatusCode.NotFound)
        }

        put("/api/recetas/{id}") {
            val userId = call.userId()
            val id = call.parameters["id"]?.toIntOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest)
            val r = call.receive<RecetaDto>()
            if (recRepo.updateReceta(id, r, userId)) call.respond(HttpStatusCode.OK)
            else call.respond(HttpStatusCode.NotFound)
        }

        // --- IA NUTRICIONAL ---
        post("/api/recetas/nutricion") {
            try {
                if (geminiApiKey.isBlank()) {
                    call.respond(HttpStatusCode.InternalServerError, "Error: API Key no configurada")
                    return@post
                }
                val receta = call.receive<RecetaDto>()
                val prompt = """
                    Analiza nutricionalmente esta receta: ${receta.titulo}.
                    Ingredientes: ${receta.ingredientes}.
                    Pasos: ${receta.pasos}.
                    Devuelve ESTRICTAMENTE un JSON con: { "calorias": "...", "proteinas": "...", "grasas": "...", "carbos": "...", "consejo": "..." }.
                    No incluyas texto extra.
                """.trimIndent()

                val res = geminiClient.post("https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent") {
                    contentType(ContentType.Application.Json)
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
                    val rawText = body["candidates"]?.jsonArray?.get(0)?.jsonObject
                        ?.get("content")?.jsonObject
                        ?.get("parts")?.jsonArray?.get(0)?.jsonObject
                        ?.get("text")?.jsonPrimitive?.content ?: ""

                    // Extractor robusto de JSON OBJETO { ... }
                    val jsonMatch = Regex("""\{.*\}""", RegexOption.DOT_MATCHES_ALL).find(rawText)
                    val cleanJson = jsonMatch?.value ?: rawText.replace("```json", "").replace("```", "").trim()

                    try {
                        call.respond(Json.parseToJsonElement(cleanJson))
                    } catch (e: Exception) {
                        println("DEPURACION_IA: Error parse Nutricion. Raw: $rawText")
                        call.respond(HttpStatusCode.InternalServerError, "Error al procesar el análisis nutricional")
                    }
                } else {
                    println("DEPURACION_IA: Error Gemini Nutricion (${res.status}): ${res.bodyAsText()}")
                    call.respond(HttpStatusCode.InternalServerError, "Error de Gemini")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Error")
            }
        }

        // --- IA VISIÓN (INVENTARIO) ---
        post("/api/inventario/vision") {
            try {
                val multipart = call.receiveMultipart()
                var imageBytes: ByteArray? = null
                
                multipart.forEachPart { part ->
                    if (part is PartData.FileItem) {
                        imageBytes = part.provider().readRemaining().readByteArray()
                    }
                    part.dispose()
                }

                if (imageBytes == null) {
                    call.respond(HttpStatusCode.BadRequest, "Imagen no recibida")
                    return@post
                }

                val prompt = "Detecta alimentos en esta imagen. Devuelve un JSON: [ { \"nombre\": \"...\", \"cantidad\": 1 } ]."
                val base64Image = java.util.Base64.getEncoder().encodeToString(imageBytes)

                val res = geminiClient.post("https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent") {
                    contentType(ContentType.Application.Json)
                    header("x-goog-api-key", geminiApiKey)
                    setBody(buildJsonObject {
                        putJsonArray("contents") {
                            addJsonObject {
                                putJsonArray("parts") {
                                    addJsonObject { put("text", prompt) }
                                    addJsonObject {
                                        putJsonObject("inline_data") {
                                            put("mime_type", "image/jpeg")
                                            put("data", base64Image)
                                        }
                                    }
                                }
                            }
                        }
                    })
                }

                if (res.status == HttpStatusCode.OK) {
                    val body = res.body<JsonObject>()
                    val rawText = body["candidates"]?.jsonArray?.get(0)?.jsonObject
                        ?.get("content")?.jsonObject
                        ?.get("parts")?.jsonArray?.get(0)?.jsonObject
                        ?.get("text")?.jsonPrimitive?.content ?: ""

                    // Extractor robusto para listas JSON [ ... ]
                    val jsonMatch = Regex("""\[.*\]""", RegexOption.DOT_MATCHES_ALL).find(rawText)
                    val cleanJson = jsonMatch?.value ?: rawText.replace("```json", "").replace("```", "").trim()

                    try {
                        call.respond(Json.parseToJsonElement(cleanJson))
                    } catch (e: Exception) {
                        println("DEPURACION_IA: Error parse Vision. Raw: $rawText")
                        call.respond(HttpStatusCode.InternalServerError, "Error al procesar la imagen")
                    }
                } else {
                    println("DEPURACION_IA: Error Gemini Vision (${res.status}): ${res.bodyAsText()}")
                    call.respond(HttpStatusCode.InternalServerError, "IA Error")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Error")
            }
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

                // Restaurada versión Gemini 3 Flash Preview v1beta solicitada por el usuario
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent"
                println("DEPURACION_IA: Llamando a Gemini 3 Flash Preview (v1beta)...")

                val res = geminiClient.post(url) {
                    contentType(ContentType.Application.Json)
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
                    val rawText = body["candidates"]?.jsonArray?.get(0)?.jsonObject
                        ?.get("content")?.jsonObject
                        ?.get("parts")?.jsonArray?.get(0)?.jsonObject
                        ?.get("text")?.jsonPrimitive?.content ?: ""

                    // Extractor robusto para OBJETO JSON { ... }
                    val jsonMatch = Regex("""\{.*\}""", RegexOption.DOT_MATCHES_ALL).find(rawText)
                    val cleanJson = jsonMatch?.value ?: rawText.replace("```json", "").replace("```", "").trim()

                    try {
                        val receta = Json.decodeFromString<RecetaDto>(cleanJson)
                        call.respond(receta)
                    } catch (e: Exception) {
                        println("DEPURACION_IA: Error al parsear JSON Receta. Raw: $rawText")
                        call.respond(HttpStatusCode.InternalServerError, "Error al procesar la respuesta de la IA")
                    }
                } else {
                    val errorBody = res.bodyAsText()
                    println("DEPURACION_IA: Error de Gemini (${res.status}): $errorBody")
                    call.respond(HttpStatusCode.InternalServerError, "Google Gemini Error (${res.status})")
                }
            } catch (e: Exception) {
                println("DEPURACION_IA: Excepción en el endpoint: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Error desconocido")
            }
        }

        // --- COMPRAS (USER) ---
        get("/api/compras") {
            val userId = call.userId()
            val list = prodRepo.allCompras(userId)
            call.respond(ProductosResponse("Lista Personal", list.size, list))
        }

        post("/api/compras") {
            val userId = call.userId()
            val p = call.receive<ProductoDto>()
            prodRepo.addCompra(p, userId)
            call.respond(HttpStatusCode.Created, "Agregado")
        }

        delete("/api/compras/{id}") {
            val userId = call.userId()
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            if (prodRepo.deleteCompra(id, userId)) call.respond(HttpStatusCode.OK, "Eliminado")
            else call.respond(HttpStatusCode.NotFound)
        }

        put("/api/compras/{id}") {
            val userId = call.userId()
            val id = call.parameters["id"]?.toIntOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest)
            val p = call.receive<ProductoDto>()
            if (prodRepo.updateCompra(id, p, userId)) call.respond(HttpStatusCode.OK, "Actualizado")
            else call.respond(HttpStatusCode.NotFound)
        }
    }
}

fun ApplicationCall.userId(): Int? {
    val principal = principal<JWTPrincipal>()
    return principal?.payload?.getClaim("id")?.asInt()
}

fun ApplicationCall.userEmail(): String? {
    val principal = principal<JWTPrincipal>()
    return principal?.payload?.getClaim("email")?.asString()
}
