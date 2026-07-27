package com.fnbioscoop

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fnbioscoop.R
import com.google.android.material.appbar.MaterialToolbar

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var rvOrders: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: OrderAdapter
    private val handler = Handler(Looper.getMainLooper())

    private val refreshRunnable = object : Runnable {
        override fun run() {
            loadOrders()
            handler.postDelayed(this, 10000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarAdmin)
        setSupportActionBar(toolbar)

        rvOrders = findViewById(R.id.rvOrders)
        tvEmpty  = findViewById(R.id.tvEmptyOrders)

        adapter = OrderAdapter(
            onConfirm = { order ->
                AlertDialog.Builder(this)
                    .setTitle("Konfirmasi Pesanan")
                    .setMessage("Konfirmasi pesanan ${order.nomor} dari ${order.namaUser}?")
                    .setPositiveButton("Konfirmasi") { _, _ ->
                        UserPrefs.confirmOrder(this, order.nomor)
                        loadOrders()
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            },
            onTolak = { order ->
                AlertDialog.Builder(this)
                    .setTitle("Tolak Pesanan")
                    .setMessage("Yakin ingin menolak pesanan ${order.nomor} dari ${order.namaUser}?")
                    .setPositiveButton("Ya, Tolak") { _, _ ->
                        UserPrefs.rejectOrder(this, order.nomor)
                        loadOrders()
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
        )
        rvOrders.layoutManager = LinearLayoutManager(this)
        rvOrders.adapter = adapter

        // Tombol Kelola Stok
        findViewById<View>(R.id.btnKelolaStok).setOnClickListener {
            startActivity(Intent(this, AdminStokActivity::class.java))
        }

        // Logout dengan konfirmasi
        findViewById<View>(R.id.btnAdminLogout).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Apakah anda yakin ingin log out?")
                .setPositiveButton("Ya, Log Out") { _, _ ->
                    UserPrefs.logout(this)
                    startActivity(Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        loadOrders()
        handler.postDelayed(refreshRunnable, 5000)
    }

    private fun loadOrders() {
        val ordersJson = UserPrefs.getAllOrders(this)
        val orderList = mutableListOf<OrderItem>()
        for (i in 0 until ordersJson.length()) {
            val o = ordersJson.getJSONObject(i)
            val status = o.optString("status", "pending")
            // Admin hanya lihat pesanan pending (sudah bayar, belum dikonfirmasi)
            if (status != "pending") continue
            val itemsArr = o.getJSONArray("items")
            val itemNames = StringBuilder()
            for (j in 0 until itemsArr.length()) {
                val item = itemsArr.getJSONObject(j)
                if (j > 0) itemNames.append(", ")
                itemNames.append("${item.getInt("qty")}x ${item.getString("nama")}")
            }
            orderList.add(
                OrderItem(
                    nomor    = o.getString("nomor"),
                    namaUser = o.getString("namaUser"),
                    username = o.getString("username"),
                    tanggal  = o.getString("tanggal"),
                    metode   = o.getString("metode"),
                    total    = o.getInt("total"),
                    items    = itemNames.toString(),
                    status   = status,
                    catatan  = o.optString("catatan", "")
                )
            )
        }
        orderList.reverse()
        if (orderList.isEmpty()) {
            tvEmpty.visibility  = View.VISIBLE
            rvOrders.visibility = View.GONE
        } else {
            tvEmpty.visibility  = View.GONE
            rvOrders.visibility = View.VISIBLE
            adapter.submitList(orderList)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(refreshRunnable)
    }
}
