package com.example.routes

import com.example.dtos.*
import com.example.repository.ProductoRepository
import com.example.repository.RecetaRepository
import com.example.models.Actividades
import com.example.config.DatabaseFactory.dbQuery
import io.ktor.client.*
import io.ktor.client.call.*
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
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.insert
import java.time.LocalDateTime

fun Route.productRoutes(
    prodRepo: ProductoRepository,
    recRepo: RecetaRepository
) {
    val geminiClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            // Usamos la extensión json() de io.ktor.serialization.kotlinx.json
            json(jsonConfig)
        }
    }
    val geminiApiKey = System.getenv("GEMINI_API_KEY") ?: "TU_API_KEY_AQUI"

    authenticate("auth-jwt") {

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
                val request = call.receive<IngredientsRequest>()
                val prompt = "Eres un chef. Genera una receta JSON con {titulo, ingredientes, pasos} usando: ${request.ingredientes.joinToString()}"
                val res = geminiClient.post("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$geminiApiKey") {
                    contentType(ContentType.Application.Json)
                    setBody(buildJsonObject { putJsonArray("contents") { addJsonObject { putJsonArray("parts") { addJsonObject { put("text", prompt) } } } } })
                }
                if (res.status == HttpStatusCode.OK) {
                    val body = res.body<JsonObject>()
                    val text = body["candidates"]?.jsonArray?.get(0)?.jsonObject?.get("content")?.jsonObject?.get("parts")?.jsonArray?.get(0)?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                    val json = text.replace("```json", "").replace("```", "").trim()
                    call.respond(Json.decodeFromString<RecetaDto>(json))
                } else {
                    call.respond(HttpStatusCode.InternalServerError, "IA Error")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Error")
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
