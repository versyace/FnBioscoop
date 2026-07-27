package com.fnbioscoop

import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fnbioscoop.R
import com.google.android.material.appbar.MaterialToolbar
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class PembayaranActivity : AppCompatActivity() {

    private var totalBayar: Int = 0
    private var nomorPesanan: String = ""
    private var tanggal: String = ""
    private var metode: String = "QRIS"
    private var isUnpaidMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pembayaran)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        metode = intent.getStringExtra("metode") ?: "QRIS"
        val unpaidNomor = intent.getStringExtra("unpaid_nomor")

        if (unpaidNomor != null) {
            // Mode lanjutkan pembayaran dari riwayat
            isUnpaidMode = true
            val username = UserPrefs.getLoggedInUsername(this) ?: ""
            val allOrders = UserPrefs.getOrdersByUsername(this, username)
            for (i in 0 until allOrders.length()) {
                val o = allOrders.getJSONObject(i)
                if (o.getString("nomor") == unpaidNomor && o.optString("status") == "unpaid") {
                    nomorPesanan = o.getString("nomor")
                    tanggal      = o.getString("tanggal")
                    totalBayar   = o.getInt("total")
                    metode       = o.optString("metode", "QRIS")
                    break
                }
            }
        } else {
            // Mode pesanan baru — ambil data dari cart SEKARANG (cart masih ada)
            isUnpaidMode = false
            nomorPesanan = CartManager.generateNomorPesanan()
            tanggal      = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id")).format(Date())
            totalBayar   = CartManager.total()  // Cart masih ada, total benar
        }

        findViewById<TextView>(R.id.tvMetodePembayaran).text = metode
        findViewById<TextView>(R.id.tvTotalBayar).text       = CartManager.formatRupiah(totalBayar)
        findViewById<TextView>(R.id.tvNomorPesanan).text     = nomorPesanan
        findViewById<TextView>(R.id.tvTanggal).text          = tanggal
        findViewById<TextView>(R.id.tvMetodeRingkas).text    = metode

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBayar)
            .setOnClickListener {
                if (!isUnpaidMode) {
                    // Simpan order sebagai unpaid dan kosongkan cart saat "Bayar Sekarang"
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
                        put("metode", metode)
                        put("total", totalBayar)  // total sudah benar dari atas
                        put("items", itemsArr)
                        put("status", "unpaid")
                    }
                    UserPrefs.saveUnpaidOrder(this, order)
                    CartManager.clear()  // Cart kosong setelah "Bayar Sekarang"
                    isUnpaidMode = true  // Sekarang sudah tersimpan, mode jadi unpaid
                }
                showQrisDialog()
            }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBatalkanPesanan)
            .setOnClickListener {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Batalkan Pesanan")
                    .setMessage("Yakin ingin membatalkan pesanan ini?")
                    .setPositiveButton("Ya, Batalkan") { _, _ ->
                        if (isUnpaidMode) {
                            UserPrefs.cancelOrder(this, nomorPesanan)
                        }
                        CartManager.clear()  // Cart dikosongkan saat batalkan
                        val intent = Intent(this, MenuActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                        finish()
                    }
                    .setNegativeButton("Tidak", null)
                    .show()
            }
    }

    private fun showQrisDialog() {
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

        // Unduh QR = tandai order sebagai paid (pending)
        dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnUnduhQr)
            .setOnClickListener {
                saveQrImage(imgQris)
                UserPrefs.markOrderAsPaid(this, nomorPesanan)
                dialog.dismiss()
                val intent = Intent(this, DetailPembayaranActivity::class.java)
                intent.putExtra("metode", metode)
                intent.putExtra("nomor", nomorPesanan)
                intent.putExtra("tanggal", tanggal)
                intent.putExtra("total", totalBayar)
                startActivity(intent)
                finish()
            }

        // Tutup = hanya tutup dialog, order tetap unpaid
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
