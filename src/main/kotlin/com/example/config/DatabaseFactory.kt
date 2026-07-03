package com.example.config

import com.example.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        val url = System.getenv("DATABASE_URL")

        if (url != null && url.startsWith("postgresql://")) {
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
        } else {
            Database.connect(
                url = "jdbc:h2:mem:albahaca;DB_CLOSE_DELAY=-1;",
                driver = "org.h2.Driver"
            )
        }

        transaction {
            SchemaUtils.create(Usuarios, Frutas, Recetas, Compras, Actividades)

            // Asegurar Admin con contraseña sencilla
            val adminEmail = "admin@albahaca.com"
            val exists = Usuarios.selectAll().where { Usuarios.email eq adminEmail }.count() > 0

            if (!exists) {
                Usuarios.insert {
                    it[nombre] = "Administrador"
                    it[email] = adminEmail
                    it[password] = "1234"
                    it[role] = "admin"
                }
            } else {
                // Forzar contraseña simple por si quedó hasheada antes
                Usuarios.update({ Usuarios.email eq adminEmail }) {
                    it[password] = "1234"
                }
            }
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction { block() }
}
