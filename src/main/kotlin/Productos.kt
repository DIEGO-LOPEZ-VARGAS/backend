package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class Producto(
    val id: Int,
    val nombre_producto: String,
    val cantidad: Int,
    val fecha_caducidad: String,
    val tipo_almacenamiento: String,
    val disponible: Boolean
)

@Serializable
data class ProductosResponse(
    val rama: String,
    val total: Int,
    val productos: List<Producto>
)

fun Application.configureProductos() {
    routing {
        get("/api/rama2/productos") {
            val productos = listOf(
                Producto(
                    id = 1,
                    nombre_producto = "Albahaca Fresca",
                    cantidad = 5,
                    fecha_caducidad = "2026-06-08",
                    tipo_almacenamiento = "refrigerador",
                    disponible = true
                ),
                Producto(
                    id = 2,
                    nombre_producto = "Tomates Bola",
                    cantidad = 3,
                    fecha_caducidad = "2026-06-10",
                    tipo_almacenamiento = "despensa",
                    disponible = true
                ),
                Producto(
                    id = 3,
                    nombre_producto = "Brócoli",
                    cantidad = 2,
                    fecha_caducidad = "2026-06-07",
                    tipo_almacenamiento = "refrigerador",
                    disponible = false
                )
            )
            call.respond(
                HttpStatusCode.OK,
                ProductosResponse(
                    rama = "Rama 2 - Gabi",
                    total = productos.size,
                    productos = productos
                )
            )
        }

        get("/api/rama2/compras") {
            val compras = listOf(
                Producto(
                    id = 4,
                    nombre_producto = "Leche",
                    cantidad = 0,
                    fecha_caducidad = "2026-06-01",
                    tipo_almacenamiento = "refrigerador",
                    disponible = false
                ),
                Producto(
                    id = 5,
                    nombre_producto = "Huevos",
                    cantidad = 0,
                    fecha_caducidad = "2026-06-05",
                    tipo_almacenamiento = "despensa",
                    disponible = false
                ),
                Producto(
                    id = 6,
                    nombre_producto = "Zanahoria",
                    cantidad = 0,
                    fecha_caducidad = "2026-06-03",
                    tipo_almacenamiento = "refrigerador",
                    disponible = false
                )
            )
            call.respond(
                HttpStatusCode.OK,
                ProductosResponse(
                    rama = "Rama 2 - Lista de Compras",
                    total = compras.size,
                    productos = compras
                )
            )
        }
    }
}