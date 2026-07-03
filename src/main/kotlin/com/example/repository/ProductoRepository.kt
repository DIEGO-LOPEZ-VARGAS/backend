package com.example.repository

import com.example.config.DatabaseFactory.dbQuery
import com.example.dtos.FrutaDto
import com.example.dtos.ProductoDto
import com.example.models.Compras
import com.example.models.Frutas
import org.jetbrains.exposed.sql.*

class ProductoRepository {
    suspend fun allFrutas() = dbQuery {
        Frutas.selectAll().map { FrutaDto(it[Frutas.nombre], it[Frutas.cantidad]) }
    }

    suspend fun addFruta(f: FrutaDto) = dbQuery {
        Frutas.insert {
            it[nombre] = f.nombre
            it[cantidad] = f.cantidad
        }
    }

    suspend fun allCompras() = dbQuery {
        Compras.selectAll().map {
            ProductoDto(it[Compras.id], it[Compras.nombreProducto], it[Compras.cantidad], it[Compras.disponible])
        }
    }

    suspend fun addCompra(p: ProductoDto) = dbQuery {
        Compras.insert {
            it[nombreProducto] = p.nombre_producto
            it[cantidad] = p.cantidad
            it[disponible] = p.disponible
        }
    }
}
