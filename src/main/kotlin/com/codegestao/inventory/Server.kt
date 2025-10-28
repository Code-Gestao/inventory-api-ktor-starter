
package com.codegestao.inventory

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

fun main() {
    Db.connectAndMigrate()
    embeddedServer(Netty, port = 8080) { module() }.start(wait = true)
}

fun Application.module() {
    install(CallLogging)
    install(ContentNegotiation) { jackson() }

    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        route("/v1") {
            post("/sessions") {
                val req = call.receive<CreateSessionRequest>()
                val id = UUID.randomUUID()
                val createdAt = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC)
                transaction {
                    Sessions.insert {
                        it[Sessions.id] = id
                        it[Sessions.name] = req.name
                        it[Sessions.createdAt] = createdAt
                        it[Sessions.status] = "OPEN"
                    }
                }
                call.respond(mapOf("id" to id.toString(), "name" to req.name, "createdAt" to createdAt.toString(), "status" to "OPEN"))
            }

            post("/sessions/{id}/items") {
                val sessionId = UUID.fromString(call.parameters["id"] ?: error("session id required"))
                val req = call.receive<UpsertItemsRequest>()
                val now = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC)

                transaction {
                    val exists = Sessions.select { Sessions.id eq sessionId }.count() > 0
                    if (!exists) error("Session not found")

                    req.items.forEach { item ->
                        val existsItem = SessionItems.select {
                            (SessionItems.sessionId eq sessionId) and
                            (SessionItems.code eq item.code) and
                            (SessionItems.company eq item.company) and
                            (SessionItems.location eq item.location)
                        }.singleOrNull()

                        if (existsItem == null) {
                            SessionItems.insert {
                                it[id] = UUID.randomUUID()
                                it[sessionId] = sessionId
                                it[code] = item.code
                                it[barcode] = item.barcode
                                it[sku] = item.sku
                                it[location] = item.location
                                it[company] = item.company
                                it[quantity] = item.quantity
                                it[idempotencyKey] = item.idempotencyKey
                                it[createdAt] = now
                                it[updatedAt] = now
                            }
                        } else {
                            SessionItems.update({
                                (SessionItems.sessionId eq sessionId) and
                                (SessionItems.code eq item.code) and
                                (SessionItems.company eq item.company) and
                                (SessionItems.location eq item.location)
                            }) {
                                it[quantity] = item.quantity
                                it[barcode] = item.barcode
                                it[sku] = item.sku
                                it[updatedAt] = now
                            }
                        }
                    }
                }
                call.respond(mapOf("status" to "ok", "upserted" to req.items.size))
            }

            get("/sessions/{id}") {
                val sessionId = UUID.fromString(call.parameters["id"] ?: error("session id required"))
                val dto = transaction {
                    val s = Sessions.select { Sessions.id eq sessionId }.singleOrNull()
                        ?: error("Session not found")
                    val items = SessionItems.select { SessionItems.sessionId eq sessionId }
                        .map {
                            ItemResponse(
                                id = it[SessionItems.id],
                                code = it[SessionItems.code],
                                barcode = it[SessionItems.barcode],
                                sku = it[SessionItems.sku],
                                location = it[SessionItems.location],
                                company = it[SessionItems.company],
                                quantity = it[SessionItems.quantity]
                            )
                        }
                    SessionResponse(
                        id = s[Sessions.id],
                        name = s[Sessions.name],
                        createdAt = s[Sessions.createdAt].toString(),
                        status = s[Sessions.status],
                        items = items
                    )
                }
                call.respond(dto)
            }
        }
    }
}
