package com.fnbioscoop

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fnbioscoop.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class RiwayatActivity : AppCompatActivity() {

    private lateinit var rv: RecyclerView
    private lateinit var tvEmpty: TextView

    // Simpan semua CountDownTimer agar bisa di-cancel saat activity destroy
    private val activeTimers = mutableListOf<CountDownTimer>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_riwayat)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarRiwayat)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        rv      = findViewById(R.id.rvRiwayat)
        tvEmpty = findViewById(R.id.tvEmptyRiwayat)

        rv.layoutManager = LinearLayoutManager(this)
        loadRiwayat()
    }

    override fun onResume() {
        super.onResume()
        loadRiwayat()
    }

    override fun onDestroy() {
        super.onDestroy()
        activeTimers.forEach { it.cancel() }
        activeTimers.clear()
    }

    private fun loadRiwayat() {
        // Cancel semua timer lama
        activeTimers.forEach { it.cancel() }
        activeTimers.clear()

        val username   = UserPrefs.getLoggedInUsername(this) ?: ""
        val ordersJson = UserPrefs.getOrdersByUsername(this, username)

        data class OrderData(
            val nomor: String, val tanggal: String, val metode: String,
            val total: String, val items: String, val status: String,
            val catatan: String
        )

        val list = mutableListOf<OrderData>()
        for (i in 0 until ordersJson.length()) {
            val o = ordersJson.getJSONObject(i)
            val itemsArr = o.getJSONArray("items")
            val itemNames = StringBuilder()
            for (j in 0 until itemsArr.length()) {
                val item = itemsArr.getJSONObject(j)
                if (j > 0) itemNames.append(", ")
                itemNames.append("${item.getInt("qty")}x ${item.getString("nama")}")
            }
            list.add(OrderData(
                nomor   = o.getString("nomor"),
                tanggal = o.getString("tanggal"),
                metode  = o.getString("metode"),
                total   = CartManager.formatRupiah(o.getInt("total")),
                items   = itemNames.toString(),
                status  = o.optString("status", "pending"),
                catatan = o.optString("catatan", "")
            ))
        }
        list.reverse()

        if (list.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rv.visibility      = View.GONE
            return
        }

        tvEmpty.visibility = View.GONE
        rv.visibility      = View.VISIBLE

        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            inner class VH(v: View) : RecyclerView.ViewHolder(v) {
                val tvNomor: TextView           = v.findViewById(R.id.tvRiwayatNomor)
                val tvTanggal: TextView         = v.findViewById(R.id.tvRiwayatTanggal)
                val tvItems: TextView           = v.findViewById(R.id.tvRiwayatItems)
                val tvTotal: TextView           = v.findViewById(R.id.tvRiwayatTotal)
                val tvMetode: TextView          = v.findViewById(R.id.tvRiwayatMetode)
                val tvStatus: TextView          = v.findViewById(R.id.tvRiwayatStatus)
                val tvCatatan: TextView         = v.findViewById(R.id.tvRiwayatCatatan)
                val layoutCatatan: View         = v.findViewById(R.id.layoutCatatan)
                val tvTimerUnpaid: TextView     = v.findViewById(R.id.tvTimerUnpaid)
                val btnLanjutkan: MaterialButton = v.findViewById(R.id.btnLanjutkanPembayaran)
                val btnBatalkan: MaterialButton  = v.findViewById(R.id.btnBatalkanPesananRiwayat)
                val tvTimerBatalkan: TextView    = v.findViewById(R.id.tvTimerBatalkan)
                val btnBatalkanPaid: MaterialButton = v.findViewById(R.id.btnBatalkanPesananPaid)
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_riwayat, parent, false)
                return VH(v)
            }

            override fun getItemCount() = list.size

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val h    = holder as VH
                val data = list[position]

                h.tvNomor.text   = data.nomor
                h.tvTanggal.text = data.tanggal
                h.tvItems.text   = data.items
                h.tvTotal.text   = data.total
                h.tvMetode.text  = data.metode

                if (data.catatan.isNotEmpty()) {
                    h.layoutCatatan.visibility = View.VISIBLE
                    h.tvCatatan.text           = data.catatan
                } else {
                    h.layoutCatatan.visibility = View.GONE
                }

                // Reset
                h.btnLanjutkan.visibility    = View.GONE
                h.btnBatalkan.visibility     = View.GONE
                h.tvTimerUnpaid.visibility   = View.GONE
                h.tvTimerBatalkan.visibility = View.GONE
                h.btnBatalkanPaid.visibility = View.GONE

                when (data.status) {
                    "confirmed" -> {
                        h.tvStatus.text = "✅ Dikonfirmasi"
                        h.tvStatus.setTextColor(android.graphics.Color.parseColor("#27AE60"))
                    }
                    "rejected" -> {
                        h.tvStatus.text = "❌ Pesanan anda ditolak"
                        h.tvStatus.setTextColor(android.graphics.Color.parseColor("#E53935"))
                    }
                    "unpaid" -> {
                        h.tvStatus.text = "🕐 Belum Dibayar"
                        h.tvStatus.setTextColor(android.graphics.Color.parseColor("#E53935"))
                        h.btnLanjutkan.visibility = View.VISIBLE
                        h.btnBatalkan.visibility  = View.VISIBLE

                        // Tampilkan timer countdown unpaid (sisa dari 5 menit)
                        val savedTs = UserPrefs.getPaymentTimestamp(this@RiwayatActivity, data.nomor)
                        if (savedTs > 0) {
                            val elapsed   = System.currentTimeMillis() - savedTs
                            val remaining = 5 * 60 * 1000L - elapsed
                            if (remaining > 0) {
                                h.tvTimerUnpaid.visibility = View.VISIBLE
                                val timer = object : CountDownTimer(remaining, 1000) {
                                    override fun onTick(ms: Long) {
                                        val min = ms / 60000
                                        val sec = (ms % 60000) / 1000
                                        h.tvTimerUnpaid.text =
                                            "Selesaikan pembayaran anda dalam waktu %02d:%02d".format(min, sec)
                                    }
                                    override fun onFinish() {
                                        // Timer habis — hanguskan dan refresh
                                        UserPrefs.cancelOrder(this@RiwayatActivity, data.nomor)
                                        CartManager.clear()
                                        showExpiredDialog()
                                    }
                                }.start()
                                activeTimers.add(timer)
                            } else {
                                // Sudah kadaluarsa
                                UserPrefs.cancelOrder(this@RiwayatActivity, data.nomor)
                                CartManager.clear()
                            }
                        }

                        h.btnLanjutkan.setOnClickListener {
                            val intent = Intent(this@RiwayatActivity, RingkasanPesananActivity::class.java)
                            intent.putExtra("unpaid_nomor", data.nomor)
                            startActivity(intent)
                        }

                        h.btnBatalkan.setOnClickListener {
                            AlertDialog.Builder(this@RiwayatActivity)
                                .setTitle("Batalkan Pesanan")
                                .setMessage("Yakin ingin membatalkan pesanan ini?")
                                .setPositiveButton("Ya, Batalkan") { _, _ ->
                                    UserPrefs.cancelOrder(this@RiwayatActivity, data.nomor)
                                    CartManager.clear()
                                    loadRiwayat()
                                }
                                .setNegativeButton("Tidak", null)
                                .show()
                        }
                    }
                    "pending" -> {
                        h.tvStatus.text = "⏳ Menunggu konfirmasi"
                        h.tvStatus.setTextColor(android.graphics.Color.parseColor("#FF9800"))

                        // Timer batalkan 1 menit setelah paid
                        val paidTs = UserPrefs.getPaidTimestamp(this@RiwayatActivity, data.nomor)
                        if (paidTs > 0) {
                            val elapsed   = System.currentTimeMillis() - paidTs
                            val remaining = 60 * 1000L - elapsed
                            if (remaining > 0) {
                                h.tvTimerBatalkan.visibility = View.VISIBLE
                                h.btnBatalkanPaid.visibility = View.VISIBLE

                                val timer = object : CountDownTimer(remaining, 1000) {
                                    override fun onTick(ms: Long) {
                                        val sec = (ms / 1000).toInt()
                                        h.tvTimerBatalkan.text = "Batalkan dalam $sec detik"
                                    }
                                    override fun onFinish() {
                                        h.tvTimerBatalkan.visibility = View.GONE
                                        h.btnBatalkanPaid.visibility = View.GONE
                                    }
                                }.start()
                                activeTimers.add(timer)

                                h.btnBatalkanPaid.setOnClickListener {
                                    AlertDialog.Builder(this@RiwayatActivity)
                                        .setTitle("Batalkan Pesanan")
                                        .setMessage("Yakin ingin membatalkan pesanan yang sudah dibayar?")
                                        .setPositiveButton("Ya, Batalkan") { _, _ ->
                                            UserPrefs.cancelOrder(this@RiwayatActivity, data.nomor)
                                            loadRiwayat()
                                        }
                                        .setNegativeButton("Tidak", null)
                                        .show()
                                }
                            }
                        }
                    }
                    else -> {
                        h.tvStatus.text = "⏳ Menunggu konfirmasi"
                        h.tvStatus.setTextColor(android.graphics.Color.parseColor("#FF9800"))
                    }
                }
            }
        }
    }

    private fun showExpiredDialog() {
        if (isFinishing || isDestroyed) return
        AlertDialog.Builder(this)
            .setTitle("⏰ Waktu Habis!")
            .setMessage("Maaf, waktu pembayaran telah habis.\nPesanan Anda telah dibatalkan secara otomatis.")
            .setCancelable(false)
            .setPositiveButton("Oke") { _, _ -> loadRiwayat() }
            .show()
    }
}
