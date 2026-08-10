package com.example.config

import com.example.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        val url = System.getenv("DATABASE_URL")
        println("--- Iniciando DatabaseFactory ---")

        if (!url.isNullOrBlank() && (url.startsWith("postgresql://") || url.startsWith("postgres://"))) {
            val dbUrl = url.replace("postgres://", "postgresql://")

            try {
                // Parseo inteligente de URI de Railway
                val uri = java.net.URI(dbUrl)
                val userInfo = uri.userInfo ?: ""
                val user = if (userInfo.contains(":")) userInfo.split(":")[0] else userInfo
                val password = if (userInfo.contains(":")) userInfo.split(":")[1] else ""
                val host = uri.host
                val port = if (uri.port != -1) uri.port else 5432 // Default port 5432
                val path = uri.path
                
                val jdbcUrl = "jdbc:postgresql://$host:$port$path"

                println("Conectando a Postgres: $jdbcUrl (Usuario: $user)")

                Database.connect(
                    url = jdbcUrl,
                    driver = "org.postgresql.Driver",
                    user = user,
                    password = password
                )

                testConnection()
            } catch (e: Exception) {
                println("Error en conexión Postgres: ${e.message}. Probando respaldo H2.")
                fallbackToH2()
            }
        } else {
            println("No se detectó DATABASE_URL válida. Usando H2 en memoria.")
            fallbackToH2()
        }
    }

    private fun testConnection() {
        transaction {
            try {
                SchemaUtils.createMissingTablesAndColumns(Usuarios, Frutas, Recetas, Compras, Actividades)

                // Asegurar Admin
                val adminEmail = "admin@albahaca.com"
                if (Usuarios.selectAll().where { Usuarios.email eq adminEmail }.count() == 0L) {
                    Usuarios.insert {
                        it[nombre] = "Administrador"
                        it[email] = adminEmail
                        it[password] = "1234"
                        it[role] = "admin"
                    }
                    println("Admin creado exitosamente.")
                }
            } catch (e: Exception) {
                println("Aviso: No se pudo validar el esquema al inicio: ${e.message}")
            }
        }
    }

    private fun fallbackToH2() {
        Database.connect(
            url = "jdbc:h2:mem:albahaca;DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver"
        )
        testConnection()
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction { block() }
}
