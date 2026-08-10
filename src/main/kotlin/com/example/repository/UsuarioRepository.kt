package com.example.repository

import com.example.config.DatabaseFactory.dbQuery
import com.example.dtos.RegisterRequest
import com.example.models.Usuarios
import com.example.security.PasswordHasher
import org.jetbrains.exposed.sql.*

class UsuarioRepository {
    suspend fun findByEmail(identifier: String) = dbQuery {
        Usuarios.selectAll()
            .where { (Usuarios.email eq identifier) or (Usuarios.nombre eq identifier) }
            .map { rowToUser(it) }
            .singleOrNull()
    }

    suspend fun create(req: RegisterRequest) = dbQuery {
        Usuarios.insert {
            it[nombre] = req.nombre
            it[email] = req.email
            it[password] = PasswordHasher.hash(req.password)
            it[role] = req.role
        }.resultedValues?.firstOrNull()?.let { rowToUser(it) }
    }

    private fun rowToUser(row: ResultRow) = UserEntity(
        id = row[Usuarios.id],
        nombre = row[Usuarios.nombre],
        email = row[Usuarios.email],
        password = row[Usuarios.password],
        role = row[Usuarios.role]
    )
}

data class UserEntity(
    val id: Int,
    val nombre: String,
    val email: String,
    val password: String,
    val role: String
)
