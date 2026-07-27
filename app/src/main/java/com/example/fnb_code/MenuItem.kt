package com.example.fnb_code

data class MenuItem(
    val id: Int,
    val nama: String,
    val deskripsi: String,
    val harga: Int,
    val kalori: String,
    val imageRes: Int = 0,
    var qty: Int = 0,
    var isHabis: Boolean = false
)