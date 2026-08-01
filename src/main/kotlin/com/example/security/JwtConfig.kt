package com.example.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

object JwtConfig {
    private const val SECRET = "super-secret-key-albahaca" // In prod, use env var
    private const val ISSUER = "albahaca-backend"
    private const val VALIDITY_MS = 36_000_000 // 10 hours
    private val algorithm = Algorithm.HMAC256(SECRET)

    val verifier = JWT
        .require(algorithm)
        .withIssuer(ISSUER)
        .build()

    fun generateToken(id: Int, email: String, role: String): String = JWT.create()
        .withSubject("Authentication")
        .withIssuer(ISSUER)
        .withClaim("id", id)
        .withClaim("email", email)
        .withClaim("role", role)
        .withExpiresAt(Date(System.currentTimeMillis() + VALIDITY_MS))
        .sign(algorithm)
}
