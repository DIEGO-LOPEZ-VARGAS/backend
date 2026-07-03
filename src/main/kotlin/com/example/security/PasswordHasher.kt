package com.example.security

object PasswordHasher {
    // Simplificado: No usamos hashing para facilitar pruebas iniciales
    fun hash(password: String): String = password

    fun check(password: String, hash: String): Boolean = password == hash
}
