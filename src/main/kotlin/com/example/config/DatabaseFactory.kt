package com.example.config

import com.example.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        val url = System.getenv("DATABASE_URL")
        println("--- Iniciando DatabaseFactory ---")

        if (url != null && (url.startsWith("postgresql://") || url.startsWith("postgres://"))) {
            val dbUrl = if (url.startsWith("postgres://")) {
                url.replace("postgres://", "postgresql://")
            } else {
                url
            }

            try {
                // Parseo manual robusto para extraer credenciales de Railway
                val uri = java.net.URI(dbUrl)
                val userInfo = uri.userInfo ?: ""
                val user = if (userInfo.contains(":")) userInfo.split(":")[0] else userInfo
                val password = if (userInfo.contains(":")) userInfo.split(":")[1] else ""
                val host = uri.host
                val port = uri.port
                val path = uri.path
                
                val jdbcUrl = "jdbc:postgresql://$host:$port$path"

                println("Conectando a Postgres con URI: $jdbcUrl (Usuario: $user)")

                Database.connect(
                    url = jdbcUrl,
                    driver = "org.postgresql.Driver",
                    user = user,
                    password = password
                )
            } catch (e: Exception) {
                println("Error parseando DB URI: ${e.message}. Probando conexión directa...")
                try {
                    Database.connect(dbUrl, driver = "org.postgresql.Driver")
                } catch (e2: Exception) {
                    println("Falla total de conexión: ${e2.message}. Usando H2 de respaldo.")
                    fallbackToH2()
                }
            }
        } else {
            println("No se detectó DATABASE_URL. Usando H2 en memoria.")
            fallbackToH2()
        }

        transaction {
            try {
                // createMissingTablesAndColumns asegura que si añadimos una columna (como usuario_id),
                // la base de datos se actualice sin borrar lo anterior.
                SchemaUtils.createMissingTablesAndColumns(Usuarios, Frutas, Recetas, Compras, Actividades)

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
                    println("Admin por defecto creado.")
                } else {
                    // Forzar contraseña simple por si quedó hasheada antes
                    Usuarios.update({ Usuarios.email eq adminEmail }) {
                        it[password] = "1234"
                    }
                    println("Password de Admin actualizado a '1234'.")
                }
            } catch (e: Exception) {
                println("Aviso: No se pudo verificar/crear tablas en el inicio: ${e.message}")
            }
        }
    }

    private fun fallbackToH2() {
        Database.connect(
            url = "jdbc:h2:mem:albahaca;DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver"
        )
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction { block() }
}
