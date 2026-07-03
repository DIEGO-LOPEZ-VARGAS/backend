package com.example.repository

import com.example.config.DatabaseFactory.dbQuery
import com.example.dtos.RecetaDto
import com.example.models.Recetas
import org.jetbrains.exposed.sql.*

class RecetaRepository {
    suspend fun allRecetas() = dbQuery {
        Recetas.selectAll().map { RecetaDto(it[Recetas.titulo], it[Recetas.ingredientes], it[Recetas.pasos]) }
    }

    suspend fun addReceta(r: RecetaDto) = dbQuery {
        Recetas.insert {
            it[titulo] = r.titulo
            it[ingredientes] = r.ingredientes
            it[pasos] = r.pasos
        }
    }
}
