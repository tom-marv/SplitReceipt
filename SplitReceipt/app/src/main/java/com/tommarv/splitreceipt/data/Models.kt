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

data class SavedSplit(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val place: String,
    val date: Long = System.currentTimeMillis(),
    val people: List<Person>,
    val items: List<ReceiptItem>,
    val discount: Double,
    val receiptImagePath: String? = null
)
