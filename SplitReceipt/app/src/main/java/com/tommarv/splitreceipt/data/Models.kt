package com.tommarv.splitreceipt.data

import java.util.UUID

data class Person(
    val id: String = UUID.randomUUID().toString(),
    val name: String
)

data class ReceiptItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val price: Double,
    val assignedPersonIds: List<String> = emptyList()
)
