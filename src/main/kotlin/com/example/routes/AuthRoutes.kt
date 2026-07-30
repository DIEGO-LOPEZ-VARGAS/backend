package com.example.routes

import com.example.dtos.LoginRequest
import com.example.dtos.LoginResponse
import com.example.dtos.RegisterRequest
import com.example.repository.UsuarioRepository
import com.example.security.JwtConfig
import com.example.security.PasswordHasher
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

        // Comparación segura usando el Hasher
        if (user != null && PasswordHasher.check(req.password, user.password)) {
            val token = JwtConfig.generateToken(user.email, user.role)

            // Registrar actividad de login
            dbQuery {
                Actividades.insert {
                    it[usuarioId] = user.id
                    it[accion] = "Login"
                    it[detalle] = "El usuario ${user.email} ha iniciado sesión con éxito."
                }
            }

            call.respond(LoginResponse(200, "Éxito", token, user.role, user.nombre))
        } else {
            // Registrar intento fallido
            if (user != null) {
                dbQuery {
                    Actividades.insert {
                        it[usuarioId] = user.id
                        it[accion] = "Login Fallido"
                        it[detalle] = "Intento de acceso fallido para: ${user.email}"
                    }
                }
            }
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
            
            val newUser = repo.create(req)

            // Registrar actividad de registro
            dbQuery {
                Actividades.insert {
                    it[usuarioId] = newUser?.id
                    it[accion] = "Registro"
                    it[detalle] = "Nuevo usuario registrado: ${req.email} (${req.nombre})"
                }
            }

            call.respond(HttpStatusCode.Created, "Usuario registrado")
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, "Error al registrar: ${e.message}")
        }
    }
}
