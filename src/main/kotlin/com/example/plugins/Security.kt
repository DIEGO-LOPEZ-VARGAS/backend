package com.example.plugins

import com.example.security.JwtConfig
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

fun Application.configureSecurity() {
    authentication {
        jwt("auth-jwt") {
            realm = "albahaca-backend"
            verifier(JwtConfig.verifier)
            validate { credential ->
                if (credential.payload.getClaim("email").asString() != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(io.ktor.http.HttpStatusCode.Unauthorized, "Token no válido o expirado")
            }
        }
    }
}
