package com.example.dtos

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val usuario: String, val password: String)

@Serializable
data class RegisterRequest(val nombre: String, val email: String, val password: String, val role: String = "user")

@Serializable
data class LoginResponse(
    val status: Int,
    val message: String,
    val token: String? = null,
    val role: String? = null,
    val nombre: String? = null
)

@Serializable
data class FrutaDto(
    val id: Int = 0, 
    val nombre: String, 
    val cantidad: Int,
    val fecha_caducidad: String = "",
    val lugar_almacenamiento: String = "Refri"
)

@Serializable
data class RecetaDto(val id: Int = 0, val titulo: String, val ingredientes: String, val pasos: String)

@Serializable
data class ProductoDto(val id: Int = 0, val nombre_producto: String, val cantidad: Int, val disponible: Boolean = true)

@Serializable
data class ProductosResponse(val rama: String, val total: Int, val productos: List<ProductoDto>)

@Serializable
data class RouteInfo(val method: String, val path: String, val description: String)

@Serializable
data class RailwayStatusResponse(val online: Boolean, val serverUrl: String, val latencyMs: Long, val routes: List<RouteInfo>)

@Serializable
data class IngredientsRequest(val ingredientes: List<String>)
