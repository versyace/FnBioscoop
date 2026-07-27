package com.fnbioscoop

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fnb_code.MenuAdapter
import com.example.fnb_code.MenuItem
import com.fnbioscoop.R
import com.google.android.material.textfield.TextInputEditText

class MenuActivity : AppCompatActivity() {

    private lateinit var adapter: MenuAdapter
    private lateinit var layoutPesan: View
    private lateinit var tvTotalItem: TextView
    private lateinit var tvEmptySearch: TextView
    private lateinit var tvBadge: TextView

    private val menuList = mutableListOf(
        MenuItem(1, "Popcorn Salt", "Popcorn renyah dengan taburan garam pilihan, cocok sebagai camilan saat nonton.", 20000, "450 kcal", R.drawable.pop),
        MenuItem(2, "Hot Dog", "Sosis sapi panggang dalam roti lembut dengan saus tomat dan mustard.", 18000, "380 kcal", R.drawable.hotdog),
        MenuItem(3, "Sistagor", "Camilan goreng renyah khas dengan bumbu spesial yang gurih dan kriuk.", 25000, "320 kcal", R.drawable.sistagor),
        MenuItem(4, "Nachos", "Keripik jagung renyah disajikan dengan saus keju, salsa, dan jalapeño.", 18000, "290 kcal", R.drawable.nachos),
        MenuItem(5, "French Fries", "Kentang goreng crispy dengan garam, disajikan hangat bersama saus sambal.", 15000, "80 kcal", R.drawable.french),
        MenuItem(6, "Iced Tea", "Teh dingin segar dengan es batu, manis dan menyegarkan.", 14000, "210 kcal", R.drawable.iced),
        MenuItem(7, "Soda", "Minuman bersoda dingin dengan berbagai pilihan rasa yang menyegarkan.", 15000, "520 kcal", R.drawable.soda),
        MenuItem(8, "Mineral Water", "Air mineral segar dalam kemasan, murni dan menyehatkan.", 5000, "180 kcal", R.drawable.mineral)
    )

    private fun refreshMenuHabis() {
        menuList.forEachIndexed { i, item ->
            menuList[i] = item.copy(isHabis = UserPrefs.isMenuHabis(this, item.id))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        layoutPesan   = findViewById(R.id.layoutPesanSekarang)
        tvTotalItem   = findViewById(R.id.tvTotalItem)
        tvEmptySearch = findViewById(R.id.tvEmptySearch)
        tvBadge       = findViewById(R.id.tvBadgeUnpaid)

        val rv = findViewById<RecyclerView>(R.id.rvMenu)

        adapter = MenuAdapter(
            menuList,
            onItemClick = { item -> showDetailDialog(item) },
            onQtyChanged = { updateCartUI() }
        )
        adapter.refreshQty()

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        val etSearch = findViewById<TextInputEditText>(R.id.etSearch)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                adapter.filter(query)
                if (adapter.itemCount == 0 && query.isNotEmpty()) {
                    tvEmptySearch.visibility = View.VISIBLE
                    rv.visibility = View.GONE
                } else {
                    tvEmptySearch.visibility = View.GONE
                    rv.visibility = View.VISIBLE
                }
            }
        })

        findViewById<View>(R.id.btnRiwayat).setOnClickListener {
            startActivity(Intent(this, RiwayatActivity::class.java))
        }
        findViewById<View>(R.id.btnAkun).setOnClickListener {
            showSignOutDialog()
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnPesanSekarang)
            .setOnClickListener { goToRingkasan() }

        updateCartUI()
    }

    override fun onResume() {
        super.onResume()
        refreshMenuHabis()
        adapter.refreshList(menuList)
        adapter.refreshQty()
        updateCartUI()
        updateBadge()
        checkExpiredOrders()
    }

    private fun checkExpiredOrders() {
        val username = UserPrefs.getLoggedInUsername(this) ?: return
        val orders = UserPrefs.getOrdersByUsername(this, username)
        var anyExpired = false
        for (i in 0 until orders.length()) {
            val o = orders.getJSONObject(i)
            if (o.optString("status") == "unpaid") {
                val nomor = o.getString("nomor")
                val savedTs = UserPrefs.getPaymentTimestamp(this, nomor)
                if (savedTs > 0) {
                    val elapsed = System.currentTimeMillis() - savedTs
                    if (elapsed >= 5 * 60 * 1000L) {
                        UserPrefs.cancelOrder(this, nomor)
                        anyExpired = true
                    }
                }
            }
        }
        if (anyExpired) {
            updateBadge()
            showExpiredPopupMenu()
        }
    }

    private fun showExpiredPopupMenu() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⏰ Waktu Habis!")
            .setMessage("Maaf, waktu pembayaran telah habis.\nPesanan Anda telah dibatalkan secara otomatis.")
            .setCancelable(false)
            .setPositiveButton("Oke") { _, _ -> }
            .show()
    }

    private fun updateBadge() {
        val username = UserPrefs.getLoggedInUsername(this) ?: ""
        val orders = UserPrefs.getOrdersByUsername(this, username)
        var unpaidCount = 0
        for (i in 0 until orders.length()) {
            if (orders.getJSONObject(i).optString("status") == "unpaid") unpaidCount++
        }
        if (unpaidCount > 0) {
            tvBadge.visibility = View.VISIBLE
            tvBadge.text = if (unpaidCount > 9) "9+" else unpaidCount.toString()
        } else {
            tvBadge.visibility = View.GONE
        }
    }

    private fun showSignOutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Akun")
            .setMessage("Apakah kamu yakin ingin Sign Out?")
            .setPositiveButton("Sign Out") { _, _ ->
                UserPrefs.logout(this)
                CartManager.clear()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showDetailDialog(item: MenuItem) {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_detail_menu)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialog.findViewById<TextView>(R.id.tvDetailNama).text    = item.nama
        dialog.findViewById<TextView>(R.id.tvDetailHarga).text   = CartManager.formatRupiah(item.harga)
        dialog.findViewById<TextView>(R.id.tvDetailDeskripsi).text = item.deskripsi
        dialog.findViewById<TextView>(R.id.tvDetailKalori).text  = item.kalori

        val imgDetail = dialog.findViewById<android.widget.ImageView>(R.id.imgDetailMenu)
        if (item.imageRes != 0) imgDetail.setImageResource(item.imageRes)

        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
    }

    private fun updateCartUI() {
        val total = CartManager.totalItem()
        if (total > 0) {
            layoutPesan.visibility = View.VISIBLE
            tvTotalItem.text = "$total item"
        } else {
            layoutPesan.visibility = View.GONE
        }
    }

    private fun goToRingkasan() {
        startActivity(Intent(this, RingkasanPesananActivity::class.java))
    }
}
