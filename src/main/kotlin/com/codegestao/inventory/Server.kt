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
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

fun main() {
    Db.connectAndMigrate()
    embeddedServer(Netty, port = 8080) { module() }.start(wait = true)
}

fun Application.module() {
    install(CallLogging)
    install(ContentNegotiation) { jackson() }

    routing {
        get("/health") { call.respond(mapOf("status" to "ok")) }

        route("/v1") {
            // POST /v1/sessions
            post("/sessions") {
                val req = call.receive<CreateSessionRequest>()
                val createdAt = LocalDateTime.now(ZoneOffset.UTC)

                // NÃO setar Sessions.id. O UUIDTable gera automaticamente.
                val newId: UUID = transaction {
                    Sessions.insertAndGetId { stmt ->
                        stmt[Sessions.name] = req.name
                        stmt[Sessions.createdAt] = createdAt
                        stmt[Sessions.status] = "OPEN"
                    }.value
                }

                call.respond(
                    mapOf(
                        "id" to newId.toString(),
                        "name" to req.name,
                        "createdAt" to createdAt.toString(),
                        "status" to "OPEN"
                    )
                )
            }

            // POST /v1/sessions/{id}/items
            post("/v1/sessions/{id}/items") {
                val sessionId = UUID.fromString(call.parameters["id"] ?: error("session id required"))
                val req = call.receive<UpsertItemsRequest>()
                val now = LocalDateTime.now(ZoneOffset.UTC)
                val sessionEid = EntityID(sessionId, Sessions)

                transaction {
                    val exists = Sessions.select { Sessions.id eq sessionEid }.count() > 0
                    if (!exists) error("Session not found")

                    req.items.forEach { item ->
                        val where = (SessionItems.sessionId eq sessionEid) and
                                    (SessionItems.code eq item.code) and
                                    (SessionItems.company eq item.company) and
                                    (SessionItems.location eq item.location)

                        val current = SessionItems.select { where }.singleOrNull()

                        if (current == null) {
                            // NÃO setar SessionItems.id (UUIDTable gera sozinho)
                            SessionItems.insert { stmt ->
                                stmt[SessionItems.sessionId] = sessionEid
                                stmt[SessionItems.code] = item.code
                                stmt[SessionItems.barcode] = item.barcode
                                stmt[SessionItems.sku] = item.sku
                                stmt[SessionItems.location] = item.location
                                stmt[SessionItems.company] = item.company
                                stmt[SessionItems.quantity] = item.quantity
                                stmt[SessionItems.idempotencyKey] = item.idempotencyKey
                                stmt[SessionItems.createdAt] = now
                                stmt[SessionItems.updatedAt] = now
                            }
                        } else {
                            SessionItems.update({ where }) { stmt ->
                                stmt[SessionItems.quantity] = item.quantity
                                stmt[SessionItems.barcode] = item.barcode
                                stmt[SessionItems.sku] = item.sku
                                stmt[SessionItems.updatedAt] = now
                            }
                        }
                    }
                }

                call.respond(mapOf("status" to "ok", "upserted" to req.items.size))
            }

            // GET /v1/sessions/{id}
            get("/v1/sessions/{id}") {
                val sessionId = UUID.fromString(call.parameters["id"] ?: error("session id required"))
                val sessionEid = EntityID(sessionId, Sessions)

                val dto = transaction {
                    val s = Sessions.select { Sessions.id eq sessionEid }.singleOrNull()
                        ?: error("Session not found")

                    val items = SessionItems.select { SessionItems.sessionId eq sessionEid }
                        .map { r ->
                            ItemResponse(
                                id = r[SessionItems.id].value,
                                code = r[SessionItems.code],
                                barcode = r[SessionItems.barcode],
                                sku = r[SessionItems.sku],
                                location = r[SessionItems.location],
                                company = r[SessionItems.company],
                                quantity = r[SessionItems.quantity]
                            )
                        }

                    SessionResponse(
                        id = s[Sessions.id].value,
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
