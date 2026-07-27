package com.fnbioscoop

import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.fnbioscoop.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class RingkasanPesananActivity : AppCompatActivity() {

    private var nomorPesanan: String = ""
    private var tanggal: String = ""
    private var totalBayar: Int = 0
    private var isUnpaidMode: Boolean = false
    private var unpaidNomor: String? = null

    private var countDownTimer: CountDownTimer? = null
    private lateinit var tvTimerBox: TextView
    private lateinit var btnBatalkan: View
    private lateinit var btnBayar: com.google.android.material.button.MaterialButton
    private lateinit var rgPembayaran: android.widget.RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ringkasan_pesanan)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            val intent = Intent(this, MenuActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        tvTimerBox = findViewById(R.id.tvTimerBox)
        btnBatalkan = findViewById(R.id.btnBatalkanPesanan)
        btnBayar = findViewById(R.id.btnSelesaikanPembayaran)
        rgPembayaran = findViewById(R.id.rgPembayaran)

        unpaidNomor = intent.getStringExtra("unpaid_nomor")

        if (unpaidNomor != null) {
            isUnpaidMode = true
            val username = UserPrefs.getLoggedInUsername(this) ?: ""
            val allOrders = UserPrefs.getOrdersByUsername(this, username)
            for (i in 0 until allOrders.length()) {
                val o = allOrders.getJSONObject(i)
                if (o.getString("nomor") == unpaidNomor && o.optString("status") == "unpaid") {
                    nomorPesanan = o.getString("nomor")
                    tanggal      = o.getString("tanggal")
                    totalBayar   = o.getInt("total")
                    break
                }
            }
            findViewById<TextView>(R.id.tvSubtotal).text    = CartManager.formatRupiah(totalBayar - CartManager.biayaLayanan())
            findViewById<TextView>(R.id.tvBiayaLayanan).text = CartManager.formatRupiah(CartManager.biayaLayanan())
            findViewById<TextView>(R.id.tvTotal).text       = CartManager.formatRupiah(totalBayar)

            // Restore metode yang sudah dipilih sebelumnya
            val allOrders2 = UserPrefs.getOrdersByUsername(this, UserPrefs.getLoggedInUsername(this) ?: "")
            for (i in 0 until allOrders2.length()) {
                val o = allOrders2.getJSONObject(i)
                if (o.getString("nomor") == unpaidNomor) {
                    when (o.optString("metode", "")) {
                        "Cash" -> rgPembayaran.check(R.id.rbCash)
                        "QRIS" -> rgPembayaran.check(R.id.rbQris)
                    }
                    break
                }
            }

            // Cek apakah sudah ada timer berjalan (dari sebelumnya klik Bayar Sekarang)
            val savedTs = UserPrefs.getPaymentTimestamp(this, nomorPesanan)

            // Kunci radio button karena sudah pernah klik Bayar Sekarang
            rgPembayaran.isEnabled = false
            for (i in 0 until rgPembayaran.childCount) {
                rgPembayaran.getChildAt(i).isEnabled = false
            }

            if (savedTs > 0) {
                val elapsed = System.currentTimeMillis() - savedTs
                val remaining = 5 * 60 * 1000L - elapsed
                if (remaining > 0) {
                    showTimerBox()
                    startCountdown(remaining)
                } else {
                    // Timer habis — hanguskan
                    expireOrder()
                }
            }
        } else {
            isUnpaidMode = false
            nomorPesanan = CartManager.generateNomorPesanan()
            tanggal      = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id")).format(Date())
            totalBayar   = CartManager.total()

            val rv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvItemPesanan)
            rv.layoutManager = LinearLayoutManager(this)
            rv.adapter = RingkasanAdapter(CartManager.getItems())

            findViewById<TextView>(R.id.tvSubtotal).text    = CartManager.formatRupiah(CartManager.subtotal())
            findViewById<TextView>(R.id.tvBiayaLayanan).text = CartManager.formatRupiah(CartManager.biayaLayanan())
            findViewById<TextView>(R.id.tvTotal).text       = CartManager.formatRupiah(totalBayar)

            // Tampilkan tombol batalkan sebelum bayar
            btnBatalkan.visibility = View.VISIBLE
            tvTimerBox.visibility  = View.GONE
        }

        btnBayar.setOnClickListener {
            val catatan = findViewById<TextInputEditText>(R.id.etCatatan).text.toString().trim()
            val isCash   = findViewById<android.widget.RadioButton>(R.id.rbCash).isChecked
            val isQris   = findViewById<android.widget.RadioButton>(R.id.rbQris).isChecked
            if (!isCash && !isQris) {
                Toast.makeText(this, "Pilih metode pembayaran terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val metodeStr = if (isCash) "Cash" else "QRIS"

            if (!isUnpaidMode) {
                val username = UserPrefs.getLoggedInUsername(this) ?: "unknown"
                val namaUser = UserPrefs.getLoggedInNama(this)
                val itemsArr = JSONArray()
                CartManager.getItems().forEach { item ->
                    itemsArr.put(JSONObject().apply {
                        put("nama", item.nama)
                        put("qty", item.qty)
                        put("harga", item.harga)
                        put("subtotal", item.harga * item.qty)
                    })
                }
                val order = JSONObject().apply {
                    put("nomor", nomorPesanan)
                    put("username", username)
                    put("namaUser", namaUser)
                    put("tanggal", tanggal)
                    put("metode", metodeStr)
                    put("total", totalBayar)
                    put("items", itemsArr)
                    put("status", "unpaid")
                    if (catatan.isNotEmpty()) put("catatan", catatan)
                }
                UserPrefs.saveUnpaidOrder(this, order)
                CartManager.clear()
                isUnpaidMode = true

                // Simpan timestamp mulai timer 5 menit
                val now = System.currentTimeMillis()
                UserPrefs.savePaymentTimestamp(this, nomorPesanan, now)
                showTimerBox()
                startCountdown(5 * 60 * 1000L)

                // Kunci radio button — tidak bisa ganti metode setelah Bayar Sekarang
                rgPembayaran.isEnabled = false
                for (i in 0 until rgPembayaran.childCount) {
                    rgPembayaran.getChildAt(i).isEnabled = false
                }
            }

            if (isCash) {
                showCashDialog()
            } else {
                showQrisDialog(catatan)
            }
        }

        // btnBatalkan hanya tampil sebelum Bayar Sekarang ditekan
        // (setelah Bayar Sekarang ditekan, diganti timer box)
        (btnBatalkan as? com.google.android.material.button.MaterialButton)?.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Batalkan Pesanan")
                .setMessage("Yakin ingin membatalkan pesanan ini?")
                .setPositiveButton("Ya, Batalkan") { _, _ ->
                    if (isUnpaidMode) UserPrefs.cancelOrder(this, nomorPesanan)
                    CartManager.clear()
                    val intent = Intent(this, MenuActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Tidak", null)
                .show()
        }
    }

    private fun showTimerBox() {
        tvTimerBox.visibility  = View.VISIBLE
        btnBatalkan.visibility = View.GONE
    }

    private fun startCountdown(millisRemaining: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(millisRemaining, 1000) {
            override fun onTick(ms: Long) {
                val min = ms / 60000
                val sec = (ms % 60000) / 1000
                tvTimerBox.text = "Selesaikan pembayaran anda dalam %02d : %02d".format(min, sec)
            }
            override fun onFinish() {
                expireOrder()
            }
        }.start()
    }

    private fun expireOrder() {
        countDownTimer?.cancel()
        UserPrefs.cancelOrder(this, nomorPesanan)
        CartManager.clear()
        // Tampilkan popup kadaluarsa lalu kembali ke menu
        showExpiredPopup()
    }

    private fun showExpiredPopup() {
        if (isFinishing || isDestroyed) return
        AlertDialog.Builder(this)
            .setTitle("⏰ Waktu Habis!")
            .setMessage("Maaf, waktu pembayaran telah habis.\nPesanan Anda telah dibatalkan secara otomatis.")
            .setCancelable(false)
            .setPositiveButton("Kembali ke Menu") { _, _ ->
                val intent = Intent(this, MenuActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    override fun onBackPressed() {
        val intent = Intent(this, MenuActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    private fun showCashDialog() {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Pembayaran Cash")
            .setMessage("Silakan bayar di konter dengan total ${CartManager.formatRupiah(totalBayar)}.\n\nPesananmu akan menunggu konfirmasi dari kasir.")
            .setPositiveButton("Oke, Saya Mengerti") { _, _ ->
                countDownTimer?.cancel()
                tvTimerBox.visibility = View.GONE
                UserPrefs.markOrderAsPaid(this, nomorPesanan)
                // Simpan timestamp paid untuk timer batalkan 1 menit
                UserPrefs.savePaidTimestamp(this, nomorPesanan, System.currentTimeMillis())
                val intent = Intent(this, DetailPembayaranActivity::class.java)
                intent.putExtra("metode", "Cash")
                intent.putExtra("nomor", nomorPesanan)
                intent.putExtra("tanggal", tanggal)
                intent.putExtra("total", totalBayar)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showQrisDialog(catatan: String) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_qris)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCanceledOnTouchOutside(false)

        dialog.findViewById<TextView>(R.id.tvQrisTotal).text = CartManager.formatRupiah(totalBayar)

        val imgQris = dialog.findViewById<ImageView>(R.id.imgQris)
        val qrResId = resources.getIdentifier("qr_code", "drawable", packageName)
        if (qrResId != 0) imgQris.setImageResource(qrResId)
        else imgQris.setImageResource(R.drawable.qris)

        dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnUnduhQr)
            .setOnClickListener {
                saveQrImage(imgQris)
                countDownTimer?.cancel()
                tvTimerBox.visibility = View.GONE
                UserPrefs.markOrderAsPaid(this, nomorPesanan)
                // Simpan timestamp paid untuk timer batalkan 1 menit
                UserPrefs.savePaidTimestamp(this, nomorPesanan, System.currentTimeMillis())
                dialog.dismiss()
                val intent = Intent(this, DetailPembayaranActivity::class.java)
                intent.putExtra("metode", "QRIS")
                intent.putExtra("nomor", nomorPesanan)
                intent.putExtra("tanggal", tanggal)
                intent.putExtra("total", totalBayar)
                startActivity(intent)
                finish()
            }

        dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnTutupQris)
            .setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun saveQrImage(imageView: ImageView) {
        try {
            val bitmap = Bitmap.createBitmap(imageView.width, imageView.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            imageView.draw(canvas)
            val filename = "QRIS_FNBioscoop_${System.currentTimeMillis()}.png"
            var outputStream: OutputStream? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FNBioscoop")
                }
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let { outputStream = contentResolver.openOutputStream(it) }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "FNBioscoop")
                if (!dir.exists()) dir.mkdirs()
                outputStream = FileOutputStream(File(dir, filename))
            }
            outputStream?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                Toast.makeText(this, "QR Code berhasil diunduh ke Galeri 📥", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal mengunduh: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}