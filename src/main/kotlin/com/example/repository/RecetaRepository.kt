package com.example.repository

import com.example.config.DatabaseFactory.dbQuery
import com.example.dtos.RecetaDto
import com.example.models.Recetas
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class RecetaRepository {
    suspend fun allRecetas(usuarioId: Int?) = dbQuery {
        Recetas.selectAll()
            .where { if (usuarioId != null) Recetas.usuarioId eq usuarioId else Op.TRUE }
            .map { RecetaDto(it[Recetas.id], it[Recetas.titulo], it[Recetas.ingredientes], it[Recetas.pasos]) }
    }

    suspend fun addReceta(r: RecetaDto, usuarioId: Int?) = dbQuery {
        Recetas.insert {
            it[titulo] = r.titulo
            it[ingredientes] = r.ingredientes
            it[pasos] = r.pasos
            it[this.usuarioId] = usuarioId
        }
    }

    suspend fun deleteReceta(id: Int, usuarioId: Int?) = dbQuery {
        Recetas.deleteWhere { (Recetas.id eq id) and (Recetas.usuarioId eq usuarioId) } > 0
    }

    suspend fun updateReceta(id: Int, r: RecetaDto, usuarioId: Int?) = dbQuery {
        Recetas.update({ (Recetas.id eq id) and (Recetas.usuarioId eq usuarioId) }) {
            it[titulo] = r.titulo
            it[ingredientes] = r.ingredientes
            it[pasos] = r.pasos
        } > 0
    }
}
