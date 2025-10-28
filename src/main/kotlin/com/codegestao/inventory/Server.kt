
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
import java.util.UUID
import java.time.LocalDateTime
import java.time.ZoneOffset

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
                val createdAt = LocalDateTime.now(ZoneOffset.UTC)
                transaction {
                    Sessions.insert { row ->
                        row[Sessions.id] = id
                        row[Sessions.name] = req.name
                        row[Sessions.createdAt] = createdAt
                        row[Sessions.status] = "OPEN"
                    }
                }
                call.respond(mapOf("id" to id.toString(), "name" to req.name, "createdAt" to createdAt.toString(), "status" to "OPEN"))
            }

            post("/sessions/{id}/items") {
                val sessionId = UUID.fromString(call.parameters["id"] ?: error("session id required"))
                val req = call.receive<UpsertItemsRequest>()
                val now = LocalDateTime.now(ZoneOffset.UTC)

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
                            SessionItems.insert { row ->
                                row[SessionItems.id] = UUID.randomUUID()
                                row[SessionItems.sessionId] = sessionId
                                row[SessionItems.code] = item.code
                                row[SessionItems.barcode] = item.barcode
                                row[SessionItems.sku] = item.sku
                                row[SessionItems.location] = item.location
                                row[SessionItems.company] = item.company
                                row[SessionItems.quantity] = item.quantity
                                row[SessionItems.idempotencyKey] = item.idempotencyKey
                                row[SessionItems.createdAt] = now
                                row[SessionItems.updatedAt] = now
                            }
                        } else {
                            SessionItems.update({
                                (SessionItems.sessionId eq sessionId) and
                                (SessionItems.code eq item.code) and
                                (SessionItems.company eq item.company) and
                                (SessionItems.location eq item.location)
                            }) { row ->
                                row[SessionItems.quantity] = item.quantity
                                row[SessionItems.barcode] = item.barcode
                                row[SessionItems.sku] = item.sku
                                row[SessionItems.updatedAt] = now
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
                        .map { r ->
                            ItemResponse(
                                id = r[SessionItems.id],
                                code = r[SessionItems.code],
                                barcode = r[SessionItems.barcode],
                                sku = r[SessionItems.sku],
                                location = r[SessionItems.location],
                                company = r[SessionItems.company],
                                quantity = r[SessionItems.quantity]
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
