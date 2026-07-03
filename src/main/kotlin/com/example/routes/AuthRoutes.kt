package com.example.routes

import com.example.dtos.LoginRequest
import com.example.dtos.LoginResponse
import com.example.dtos.RegisterRequest
import com.example.repository.UsuarioRepository
import com.example.security.JwtConfig
import com.example.config.DatabaseFactory.dbQuery
import com.example.models.Actividades
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.insert

fun Route.authRoutes(repo: UsuarioRepository) {
    post("/login") {
        val req = call.receive<LoginRequest>()
        val user = repo.findByEmail(req.usuario)

        // Comparación simple de contraseña (sin hash complicado)
        if (user != null && user.password == req.password) {
            val token = JwtConfig.generateToken(user.email, user.role)

            // Registrar actividad de login
            dbQuery {
                Actividades.insert {
                    it[accion] = "Login"
                    it[detalle] = "El usuario ${user.email} ha iniciado sesión."
                }
            }

            call.respond(LoginResponse(200, "Éxito", token, user.role, user.nombre))
        } else {
            call.respond(HttpStatusCode.Unauthorized, LoginResponse(401, "Credenciales incorrectas"))
        }
    }

    post("/register") {
        try {
            val req = call.receive<RegisterRequest>()
            val existing = repo.findByEmail(req.email)
            if (existing != null) {
                call.respond(HttpStatusCode.Conflict, "El usuario ya existe")
                return@post
            }
            repo.create(req)

            // Registrar actividad de registro
            dbQuery {
                Actividades.insert {
                    it[accion] = "Registro"
                    it[detalle] = "Nuevo usuario registrado: ${req.email}"
                }
            }

            call.respond(HttpStatusCode.Created, "Usuario registrado")
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, "Error al registrar: ${e.message}")
        }
    }
}
