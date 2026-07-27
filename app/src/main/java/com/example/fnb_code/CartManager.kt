package com.fnbioscoop

import com.example.fnb_code.MenuItem

object CartManager {

    private val items = mutableListOf<MenuItem>()

    fun getItems(): List<MenuItem> = items.toList()

    fun updateItem(item: MenuItem) {
        val idx = items.indexOfFirst { it.id == item.id }
        if (idx >= 0) {
            if (item.qty == 0) items.removeAt(idx)
            else items[idx] = item
        } else {
            if (item.qty > 0) items.add(item)
        }
    }

    fun getQtyById(id: Int): Int = items.find { it.id == id }?.qty ?: 0

    fun totalItem(): Int = items.sumOf { it.qty }

    fun subtotal(): Int = items.sumOf { it.harga * it.qty }

    fun biayaLayanan(): Int = 1000

    fun total(): Int = subtotal() + biayaLayanan()

    fun clear() = items.clear()

    fun formatRupiah(amount: Int): String {
        val fmt = java.text.NumberFormat.getNumberInstance(java.util.Locale("id", "ID"))
        return "Rp ${fmt.format(amount)}"
    }

    fun generateNomorPesanan(): String {
        val num = (1..99999).random()
        return "#FNB-%05d".format(num)
    }
}