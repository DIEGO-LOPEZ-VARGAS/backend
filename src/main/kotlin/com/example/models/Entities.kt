package com.example.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object Usuarios : Table("usuarios") {
    val id = integer("id").autoIncrement()
    val nombre = varchar("nombre", 255)
    val email = varchar("email", 255).uniqueIndex()
    val password = varchar("password", 255)
    val role = varchar("role", 50) // "admin" o "user"
    override val primaryKey = PrimaryKey(id)
}

object Actividades : Table("actividades") {
    val id = integer("id").autoIncrement()
    val usuarioId = integer("usuario_id").references(Usuarios.id).nullable()
    val accion = varchar("accion", 255)
    val detalle = text("detalle")
    val fecha = datetime("fecha").default(LocalDateTime.now())
    override val primaryKey = PrimaryKey(id)
}

object Frutas : Table("frutas") {
    val id = integer("id").autoIncrement()
    val nombre = varchar("nombre", 255)
    val cantidad = integer("cantidad")
    val fechaCaducidad = varchar("fecha_caducidad", 50).default("")
    val lugarAlmacenamiento = varchar("lugar_almacenamiento", 100).default("Refri")
    val usuarioId = integer("usuario_id").references(Usuarios.id).nullable()
    override val primaryKey = PrimaryKey(id)
}

object Recetas : Table("recetas") {
    val id = integer("id").autoIncrement()
    val titulo = varchar("titulo", 255)
    val ingredientes = text("ingredientes")
    val pasos = text("pasos")
    val usuarioId = integer("usuario_id").references(Usuarios.id).nullable()
    override val primaryKey = PrimaryKey(id)
}

object Compras : Table("compras") {
    val id = integer("id").autoIncrement()
    val nombreProducto = varchar("nombre_producto", 255)
    val cantidad = integer("cantidad")
    val disponible = bool("disponible").default(true)
    val fechaCaducidad = varchar("fecha_caducidad", 50).default("")
    val tipoAlmacenamiento = varchar("tipo_almacenamiento", 100).default("Despensa")
    val usuarioId = integer("usuario_id").references(Usuarios.id).nullable()
    override val primaryKey = PrimaryKey(id)
}
