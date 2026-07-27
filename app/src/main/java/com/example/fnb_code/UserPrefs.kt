package com.fnbioscoop

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object UserPrefs {

    private const val PREF_NAME   = "fnbioscoop_prefs"
    private const val KEY_USERS   = "registered_users"
    private const val KEY_LOGGED  = "logged_in_user"
    private const val KEY_ROLE    = "logged_in_role"
    private const val KEY_ORDERS  = "all_orders"

    // ── Auth ────────────────────────────────────────────────────────────────

    fun register(context: Context, nama: String, username: String, password: String): Boolean {
        if (isUsernameTaken(context, username)) return false
        val users = getUsersJson(context)
        users.put(JSONObject().apply {
            put("nama", nama)
            put("username", username)
            put("password", password)
            put("role", "user")
        })
        saveUsersJson(context, users)
        return true
    }

    /** Login khusus pelanggan — menolak akun admin */
    fun loginPelanggan(context: Context, username: String, password: String): Boolean {
        val users = getUsersJson(context)
        for (i in 0 until users.length()) {
            val u = users.getJSONObject(i)
            if (u.getString("username") == username && u.getString("password") == password) {
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
                    .putString(KEY_LOGGED, username)
                    .putString(KEY_ROLE, "user")
                    .apply()
                return true
            }
        }
        return false
    }

    /** Login khusus admin */
    fun loginAdmin(context: Context, username: String, password: String): Boolean {
        if (username == "admin" && password == "admin123") {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_LOGGED, "admin")
                .putString(KEY_ROLE, "admin")
                .apply()
            return true
        }
        return false
    }

    /** Legacy — dipakai oleh AdminDashboard cek role */
    fun login(context: Context, username: String, password: String): Boolean {
        if (username == "admin" && password == "admin123") {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_LOGGED, "admin")
                .putString(KEY_ROLE, "admin")
                .apply()
            return true
        }
        return loginPelanggan(context, username, password)
    }

    fun logout(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .remove(KEY_LOGGED).remove(KEY_ROLE).apply()
    }

    fun isLoggedIn(context: Context): Boolean =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LOGGED, null) != null

    fun getLoggedInUsername(context: Context): String? =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LOGGED, null)

    fun getLoggedInNama(context: Context): String {
        val username = getLoggedInUsername(context) ?: return ""
        if (username == "admin") return "Admin"
        val users = getUsersJson(context)
        for (i in 0 until users.length()) {
            val u = users.getJSONObject(i)
            if (u.getString("username") == username) return u.getString("nama")
        }
        return username
    }

    fun isAdmin(context: Context): Boolean =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ROLE, "user") == "admin"

    // ── Stock Management ─────────────────────────────────────────────────────

    private const val KEY_HABIS = "menu_habis"

    fun setMenuHabis(context: Context, menuId: Int, habis: Boolean) {
        val set = getMenuHabisSet(context).toMutableSet()
        if (habis) set.add(menuId.toString()) else set.remove(menuId.toString())
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putStringSet(KEY_HABIS, set).apply()
    }

    fun isMenuHabis(context: Context, menuId: Int): Boolean =
        getMenuHabisSet(context).contains(menuId.toString())

    fun getMenuHabisSet(context: Context): Set<String> =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_HABIS, emptySet()) ?: emptySet()

    // ── Orders ───────────────────────────────────────────────────────────────

    fun saveOrder(context: Context, order: JSONObject) {
        val orders = getOrdersJson(context)
        orders.put(order)
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_ORDERS, orders.toString()).apply()
    }

    fun getAllOrders(context: Context): JSONArray = getOrdersJson(context)

    fun confirmOrder(context: Context, nomorPesanan: String) {
        val orders = getOrdersJson(context)
        for (i in 0 until orders.length()) {
            val o = orders.getJSONObject(i)
            if (o.getString("nomor") == nomorPesanan) {
                o.put("status", "confirmed")
                break
            }
        }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_ORDERS, orders.toString()).apply()
    }

    fun getOrdersByUsername(context: Context, username: String): JSONArray {
        val all = getOrdersJson(context)
        val result = JSONArray()
        for (i in 0 until all.length()) {
            val o = all.getJSONObject(i)
            if (o.optString("username") == username) result.put(o)
        }
        return result
    }

    fun getOrderStatus(context: Context, nomorPesanan: String): String {
        val orders = getOrdersJson(context)
        for (i in 0 until orders.length()) {
            val o = orders.getJSONObject(i)
            if (o.getString("nomor") == nomorPesanan) return o.optString("status", "pending")
        }
        return "pending"
    }

    /** Cari pesanan belum dibayar (unpaid) milik user tertentu */
    fun getUnpaidOrderByUsername(context: Context, username: String): JSONObject? {
        val all = getOrdersJson(context)
        for (i in 0 until all.length()) {
            val o = all.getJSONObject(i)
            if (o.optString("username") == username && o.optString("status") == "unpaid") {
                return o
            }
        }
        return null
    }

    /** Simpan pesanan sebagai "belum dibayar" (sebelum QR diunduh) */
    fun saveUnpaidOrder(context: Context, order: JSONObject) {
        // Tidak replace — bisa ada banyak unpaid sekaligus (beda order)
        val orders = getOrdersJson(context)
        orders.put(order)
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_ORDERS, orders.toString()).apply()
    }

    /** Batalkan pesanan (hapus dari daftar) */
    fun cancelOrder(context: Context, nomorPesanan: String) {
        val orders = getOrdersJson(context)
        val newOrders = JSONArray()
        for (i in 0 until orders.length()) {
            val o = orders.getJSONObject(i)
            if (o.getString("nomor") != nomorPesanan) newOrders.put(o)
        }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_ORDERS, newOrders.toString()).apply()
    }

    /** Admin menolak pesanan — ubah status jadi "rejected" agar muncul di riwayat pelanggan */
    fun rejectOrder(context: Context, nomorPesanan: String) {
        val orders = getOrdersJson(context)
        for (i in 0 until orders.length()) {
            val o = orders.getJSONObject(i)
            if (o.getString("nomor") == nomorPesanan) {
                o.put("status", "rejected")
                break
            }
        }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_ORDERS, orders.toString()).apply()
    }

    // ── Order Timestamps (for payment timer) ─────────────────────────────────

    private fun timerKey(nomor: String) = "timer_$nomor"
    private fun paidTimerKey(nomor: String) = "paid_timer_$nomor"

    /** Simpan timestamp saat tombol "Bayar Sekarang" ditekan (untuk timer 5 menit) */
    fun savePaymentTimestamp(context: Context, nomorPesanan: String, timestamp: Long) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putLong(timerKey(nomorPesanan), timestamp).apply()
    }

    fun getPaymentTimestamp(context: Context, nomorPesanan: String): Long =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getLong(timerKey(nomorPesanan), 0L)

    /** Simpan timestamp saat pesanan berhasil dibayar (untuk timer batalkan 1 menit) */
    fun savePaidTimestamp(context: Context, nomorPesanan: String, timestamp: Long) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putLong(paidTimerKey(nomorPesanan), timestamp).apply()
    }

    fun getPaidTimestamp(context: Context, nomorPesanan: String): Long =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getLong(paidTimerKey(nomorPesanan), 0L)

    /** Tandai pesanan unpaid menjadi pending (sudah bayar / QR diunduh) */
    fun markOrderAsPaid(context: Context, nomorPesanan: String) {
        val orders = getOrdersJson(context)
        for (i in 0 until orders.length()) {
            val o = orders.getJSONObject(i)
            if (o.getString("nomor") == nomorPesanan && o.optString("status") == "unpaid") {
                o.put("status", "pending")
                break
            }
        }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_ORDERS, orders.toString()).apply()
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private fun isUsernameTaken(context: Context, username: String): Boolean {
        val users = getUsersJson(context)
        for (i in 0 until users.length()) {
            if (users.getJSONObject(i).getString("username") == username) return true
        }
        return false
    }

    private fun getUsersJson(context: Context): JSONArray {
        val raw = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USERS, "[]")!!
        return JSONArray(raw)
    }

    private fun saveUsersJson(context: Context, users: JSONArray) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_USERS, users.toString()).apply()
    }

    private fun getOrdersJson(context: Context): JSONArray {
        val raw = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ORDERS, "[]")!!
        return JSONArray(raw)
    }

    // ── Alias untuk backward compatibility ──────────────────────────────────

    /** Alias: deadline cancel = paidTimestamp + 60 detik */
    fun getCancelDeadline(context: Context, nomorPesanan: String): Long {
        val paidTs = getPaidTimestamp(context, nomorPesanan)
        return if (paidTs > 0) paidTs + 60_000L else 0L
    }

    /** Alias: hapus paid timestamp (reset cancel window) */
    fun clearCancelDeadline(context: Context, nomorPesanan: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .remove(paidTimerKey(nomorPesanan)).apply()
    }
}
