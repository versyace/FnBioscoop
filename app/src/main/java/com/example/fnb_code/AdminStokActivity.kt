package com.fnbioscoop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fnb_code.MenuItem
import com.fnbioscoop.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class AdminStokActivity : AppCompatActivity() {

    private val menuList = listOf(
        MenuItem(1, "Popcorn Salt", "", 20000, "", R.drawable.pop),
        MenuItem(2, "Hot Dog", "", 25000, "", R.drawable.hotdog),
        MenuItem(3, "Sistagor", "", 20000, "", R.drawable.sistagor),
        MenuItem(4, "Nachos", "", 18000, "", R.drawable.nachos),
        MenuItem(5, "French Fries", "", 8000, "", R.drawable.french),
        MenuItem(6, "Iced Tea", "", 15000, "", R.drawable.iced),
        MenuItem(7, "Soda", "", 30000, "", R.drawable.soda),
        MenuItem(8, "Mineral Water", "", 12000, "", R.drawable.mineral)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_stok)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarStok)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvStok)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

            inner class VH(v: View) : RecyclerView.ViewHolder(v) {
                val tvNama: TextView          = v.findViewById(R.id.tvStokNama)
                val tvStatus: TextView        = v.findViewById(R.id.tvStokStatus)
                val imgMenu: android.widget.ImageView = v.findViewById(R.id.imgStokMenu)
                val btnToggle: MaterialButton = v.findViewById(R.id.btnToggleStok)
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_stok, parent, false)
                return VH(v)
            }

            override fun getItemCount() = menuList.size

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val h    = holder as VH
                val item = menuList[position]
                val habis = UserPrefs.isMenuHabis(this@AdminStokActivity, item.id)

                h.tvNama.text = item.nama
                if (item.imageRes != 0) h.imgMenu.setImageResource(item.imageRes)
                updateUI(h, habis)

                h.btnToggle.setOnClickListener {
                    val currentHabis = UserPrefs.isMenuHabis(this@AdminStokActivity, item.id)
                    UserPrefs.setMenuHabis(this@AdminStokActivity, item.id, !currentHabis)
                    updateUI(h, !currentHabis)
                }
            }

            private fun updateUI(h: VH, habis: Boolean) {
                if (habis) {
                    h.tvStatus.text = "Stok Habis"
                    h.tvStatus.setTextColor(android.graphics.Color.parseColor("#E53935"))
                    h.btnToggle.text = "Tandai Tersedia"
                    h.btnToggle.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#27AE60"))
                } else {
                    h.tvStatus.text = "Tersedia"
                    h.tvStatus.setTextColor(android.graphics.Color.parseColor("#27AE60"))
                    h.btnToggle.text = "Tandai Habis"
                    h.btnToggle.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E53935"))
                }
            }
        }
    }
}
