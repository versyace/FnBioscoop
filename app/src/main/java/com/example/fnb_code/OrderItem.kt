package com.fnbioscoop

data class OrderItem(
    val nomor: String,
    val namaUser: String,
    val username: String,
    val tanggal: String,
    val metode: String,
    val total: Int,
    val items: String,
    val status: String,
    val catatan: String = ""
)
