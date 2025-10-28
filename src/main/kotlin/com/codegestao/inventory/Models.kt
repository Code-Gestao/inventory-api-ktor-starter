
package com.codegestao.inventory

import java.util.UUID

data class CreateSessionRequest(val name: String)
data class SessionResponse(
    val id: UUID,
    val name: String,
    val createdAt: String,
    val status: String,
    val items: List<ItemResponse> = emptyList()
)

data class ItemUpsert(
    val code: String,
    val barcode: String?,
    val sku: String?,
    val location: String?,
    val company: String?,
    val quantity: Int,
    val idempotencyKey: String? = null
)
data class UpsertItemsRequest(val items: List<ItemUpsert>)

data class ItemResponse(
    val id: UUID,
    val code: String,
    val barcode: String?,
    val sku: String?,
    val location: String?,
    val company: String?,
    val quantity: Int
)
