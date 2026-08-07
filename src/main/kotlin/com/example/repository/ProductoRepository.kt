package com.example.repository

import com.example.config.DatabaseFactory.dbQuery
import com.example.dtos.FrutaDto
import com.example.dtos.ProductoDto
import com.example.models.Compras
import com.example.models.Frutas
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class ProductoRepository {
    suspend fun allFrutas(usuarioId: Int?) = dbQuery {
        Frutas.selectAll()
            .where { if (usuarioId != null) Frutas.usuarioId eq usuarioId else Op.TRUE }
            .map { FrutaDto(it[Frutas.id], it[Frutas.nombre], it[Frutas.cantidad], it[Frutas.fechaCaducidad], it[Frutas.lugarAlmacenamiento]) }
    }

    suspend fun addFruta(f: FrutaDto, usuarioId: Int?) = dbQuery {
        Frutas.insert {
            it[nombre] = f.nombre
            it[cantidad] = f.cantidad
            it[fechaCaducidad] = f.fecha_caducidad
            it[lugarAlmacenamiento] = f.lugar_almacenamiento
            it[this.usuarioId] = usuarioId
        }
    }

    suspend fun deleteFruta(id: Int, usuarioId: Int?) = dbQuery {
        Frutas.deleteWhere { (Frutas.id eq id) and (Frutas.usuarioId eq usuarioId) } > 0
    }

    suspend fun updateFruta(id: Int, f: FrutaDto, usuarioId: Int?) = dbQuery {
        Frutas.update({ (Frutas.id eq id) and (Frutas.usuarioId eq usuarioId) }) {
            it[nombre] = f.nombre
            it[cantidad] = f.cantidad
        } > 0
    }

    suspend fun allCompras(usuarioId: Int?) = dbQuery {
        Compras.selectAll()
            .where { if (usuarioId != null) Compras.usuarioId eq usuarioId else Op.TRUE }
            .map {
                ProductoDto(
                    it[Compras.id], 
                    it[Compras.nombreProducto], 
                    it[Compras.cantidad], 
                    it[Compras.disponible],
                    it[Compras.fechaCaducidad],
                    it[Compras.tipoAlmacenamiento]
                )
            }
    }

    suspend fun addCompra(p: ProductoDto, usuarioId: Int?) = dbQuery {
        Compras.insert {
            it[nombreProducto] = p.nombre_producto
            it[cantidad] = p.cantidad
            it[disponible] = p.disponible
            it[fechaCaducidad] = p.fecha_caducidad
            it[tipoAlmacenamiento] = p.tipo_almacenamiento
            it[this.usuarioId] = usuarioId
        }
    }

    suspend fun deleteCompra(id: Int, usuarioId: Int?) = dbQuery {
        Compras.deleteWhere { (Compras.id eq id) and (Compras.usuarioId eq usuarioId) } > 0
    }

    suspend fun updateCompra(id: Int, p: ProductoDto, usuarioId: Int?) = dbQuery {
        Compras.update({ (Compras.id eq id) and (Compras.usuarioId eq usuarioId) }) {
            it[nombreProducto] = p.nombre_producto
            it[cantidad] = p.cantidad
            it[disponible] = p.disponible
            it[fechaCaducidad] = p.fecha_caducidad
            it[tipoAlmacenamiento] = p.tipo_almacenamiento
        } > 0
    }
}
