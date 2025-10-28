package com.codegestao.inventory

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Tabelas usando UUIDTable (DAO), que expõe a coluna id como EntityID<UUID>.
 * Isso evita a ambiguidade do set[...] na hora do insert/update.
 */
object Sessions : UUIDTable("sessions") {
    val name = varchar("name", 255)
    val createdAt = timestamp("created_at")
    val status = varchar("status", 16)
}

object SessionItems : UUIDTable("session_items") {
    // relação com Sessions (chave estrangeira já existe no banco via Flyway)
    val sessionId = reference("session_id", Sessions)
    val code = varchar("code", 128)
    val barcode = varchar("barcode", 128).nullable()
    val sku = varchar("sku", 128).nullable()
    val location = varchar("location", 64).nullable()
    val company = varchar("company", 64).nullable()
    val quantity = integer("quantity")
    val idempotencyKey = varchar("idempotency_key", 256).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
