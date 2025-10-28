package com.codegestao.inventory

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.UUID

object Sessions : Table("sessions") {
    val id = uuid("id").uniqueIndex()
    val name = varchar("name", 255)
    val createdAt = timestamp("created_at")
    val status = varchar("status", 16)
    override val primaryKey = PrimaryKey(id)
}

object SessionItems : Table("session_items") {
    val id = uuid("id").uniqueIndex()
    val sessionId = uuid("session_id").index()   // FK lógica (sem EntityID)
    val code = varchar("code", 128)
    val barcode = varchar("barcode", 128).nullable()
    val sku = varchar("sku", 128).nullable()
    val location = varchar("location", 64).nullable()
    val company = varchar("company", 64).nullable()
    val quantity = integer("quantity")
    val idempotencyKey = varchar("idempotency_key", 256).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}
