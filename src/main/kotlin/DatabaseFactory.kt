package com.example

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

object Frutas : Table("frutas") {
    val id = integer("id").autoIncrement()
    val nombre = varchar("nombre", 255)
    val cantidad = integer("cantidad")
    override val primaryKey = PrimaryKey(id)
}

object Recetas : Table("recetas") {
    val id = integer("id").autoIncrement()
    val titulo = varchar("titulo", 255)
    val ingredientes = text("ingredientes")
    val pasos = text("pasos")
    override val primaryKey = PrimaryKey(id)
}

object DatabaseFactory {
    fun init() {
        val url = System.getenv("DATABASE_URL")
            ?: throw IllegalStateException("DATABASE_URL no está configurada")

        // Railway entrega la URL en formato postgresql://usuario:password@host:puerto/db
        // Exposed/JDBC necesita el formato jdbc:postgresql://host:puerto/db
        val regex = Regex("postgresql://([^:]+):([^@]+)@(.+)")
        val match = regex.find(url) ?: throw IllegalStateException("DATABASE_URL con formato inesperado")
        val (user, password, hostAndDb) = match.destructured
        val jdbcUrl = "jdbc:postgresql://$hostAndDb"

        Database.connect(
            url = jdbcUrl,
            driver = "org.postgresql.Driver",
            user = user,
            password = password
        )

        transaction {
            SchemaUtils.create(Frutas, Recetas)
        }
    }
}