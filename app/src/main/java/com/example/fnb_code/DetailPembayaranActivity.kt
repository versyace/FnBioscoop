package com.fnbioscoop

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.fnbioscoop.R
import com.google.android.material.button.MaterialButton

class DetailPembayaranActivity : AppCompatActivity() {

    private var nomorPesanan: String = ""
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var tvStatus: TextView
    private lateinit var tvStatusLabel: TextView
    private var alreadyConfirmed = false
    private var cancelTimer: CountDownTimer? = null

    private val pollRunnable = object : Runnable {
        override fun run() {
            if (alreadyConfirmed) return
            val status = UserPrefs.getOrderStatus(this@DetailPembayaranActivity, nomorPesanan)
            if (status == "confirmed") {
                alreadyConfirmed = true
                showConfirmedUI()
            } else {
                handler.postDelayed(this, 3000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_pembayaran)

        val metode   = intent.getStringExtra("metode") ?: "QRIS"
        nomorPesanan = intent.getStringExtra("nomor") ?: "#FNB-00001"
        val tanggal  = intent.getStringExtra("tanggal") ?: "—"
        val total    = intent.getIntExtra("total", 0)

        tvStatus      = findViewById(R.id.tvOrderStatus)
        tvStatusLabel = findViewById(R.id.tvStatusLabel)

        findViewById<TextView>(R.id.tvDetailNoPesanan).text = nomorPesanan
        findViewById<TextView>(R.id.tvDetailMetode).text    = metode
        findViewById<TextView>(R.id.tvDetailTanggal).text   = tanggal
        findViewById<TextView>(R.id.tvDetailTotal).text     = CartManager.formatRupiah(total)

        // Timer batalkan 1 menit setelah paid
        val cancelDeadlineMillis = UserPrefs.getCancelDeadline(this, nomorPesanan)
        val now     = System.currentTimeMillis()
        val sisaMs  = cancelDeadlineMillis - now

        val btnBatalkan   = findViewById<MaterialButton?>(R.id.btnBatalkanPesananDetail)
        val tvCancelTimer = findViewById<TextView?>(R.id.tvCancelTimer)

        if (btnBatalkan != null && tvCancelTimer != null) {
            if (cancelDeadlineMillis > 0 && sisaMs > 0) {
                btnBatalkan.visibility   = View.VISIBLE
                tvCancelTimer.visibility = View.VISIBLE

                cancelTimer = object : CountDownTimer(sisaMs, 1000) {
                    override fun onTick(ms: Long) {
                        val sec = (ms / 1000).toInt()
                        tvCancelTimer.text = "Batalkan dalam $sec detik"
                    }
                    override fun onFinish() {
                        btnBatalkan.visibility   = View.GONE
                        tvCancelTimer.visibility = View.GONE
                        UserPrefs.clearCancelDeadline(this@DetailPembayaranActivity, nomorPesanan)
                    }
                }.start()

                btnBatalkan.setOnClickListener {
                    AlertDialog.Builder(this)
                        .setTitle("Batalkan Pesanan")
                        .setMessage("Yakin ingin membatalkan pesanan yang sudah dibayar?")
                        .setPositiveButton("Ya, Batalkan") { _, _ ->
                            cancelTimer?.cancel()
                            UserPrefs.clearCancelDeadline(this, nomorPesanan)
                            UserPrefs.cancelOrder(this, nomorPesanan)
                            val intent = Intent(this, MenuActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                            finish()
                        }
                        .setNegativeButton("Tidak", null)
                        .show()
                }
            } else {
                btnBatalkan.visibility   = View.GONE
                tvCancelTimer.visibility = View.GONE
                if (cancelDeadlineMillis > 0) UserPrefs.clearCancelDeadline(this, nomorPesanan)
            }
        }

        val currentStatus = UserPrefs.getOrderStatus(this, nomorPesanan)
        if (currentStatus == "confirmed") {
            alreadyConfirmed = true
            showConfirmedUI()
        } else {
            showPendingUI()
            handler.postDelayed(pollRunnable, 8000)
        }

        findViewById<MaterialButton>(R.id.btnKembaliMenu)
            .setOnClickListener {
                val intent = Intent(this, MenuActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
    }

    private fun showPendingUI() {
        tvStatusLabel.visibility = View.VISIBLE
        tvStatus.visibility      = View.VISIBLE
        tvStatus.text = "⏳ Menunggu konfirmasi admin..."
        tvStatus.setTextColor(Color.parseColor("#FF9800"))
        tvStatusLabel.text = "Status Pesanan"
    }

    private fun showConfirmedUI() {
        tvStatusLabel.visibility = View.VISIBLE
        tvStatus.visibility      = View.VISIBLE
        tvStatus.text = "✅ Pesanan dikonfirmasi! Silakan ambil pesananmu."
        tvStatus.setTextColor(Color.parseColor("#4CAF50"))
        tvStatusLabel.text = "Status Pesanan"
        android.widget.Toast.makeText(
            this,
            "🎉 Pesanan #${nomorPesanan} telah dikonfirmasi! Pesanan siap diambil.",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(pollRunnable)
        cancelTimer?.cancel()
    }
}
