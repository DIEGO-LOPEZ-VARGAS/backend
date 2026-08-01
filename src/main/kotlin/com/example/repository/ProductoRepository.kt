package com.example.repository

import com.example.config.DatabaseFactory.dbQuery
import com.example.dtos.FrutaDto
import com.example.dtos.ProductoDto
import com.example.models.Compras
import com.example.models.Frutas
import org.jetbrains.exposed.sql.*

class ProductoRepository {
    suspend fun allFrutas(usuarioId: Int?) = dbQuery {
        Frutas.selectAll()
            .where { if (usuarioId != null) Frutas.usuarioId eq usuarioId else Op.TRUE }
            .map { FrutaDto(it[Frutas.nombre], it[Frutas.cantidad]) }
    }

    suspend fun addFruta(f: FrutaDto, usuarioId: Int?) = dbQuery {
        Frutas.insert {
            it[nombre] = f.nombre
            it[cantidad] = f.cantidad
            it[this.usuarioId] = usuarioId
        }
    }

    suspend fun allCompras(usuarioId: Int?) = dbQuery {
        Compras.selectAll()
            .where { if (usuarioId != null) Compras.usuarioId eq usuarioId else Op.TRUE }
            .map {
                ProductoDto(it[Compras.id], it[Compras.nombreProducto], it[Compras.cantidad], it[Compras.disponible])
            }
    }

    suspend fun addCompra(p: ProductoDto, usuarioId: Int?) = dbQuery {
        Compras.insert {
            it[nombreProducto] = p.nombre_producto
            it[cantidad] = p.cantidad
            it[disponible] = p.disponible
            it[this.usuarioId] = usuarioId
        }
    }
}
