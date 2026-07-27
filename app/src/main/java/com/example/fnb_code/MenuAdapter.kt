package com.example.fnb_code

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fnbioscoop.CartManager
import com.fnbioscoop.R
import com.google.android.material.card.MaterialCardView

class MenuAdapter(
    private val menuList: List<MenuItem>,
    private val onItemClick: (MenuItem) -> Unit,
    private val onQtyChanged: (MenuItem) -> Unit
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    private val qtyMap = mutableMapOf<Int, Int>()
    private var filteredList: List<MenuItem> = menuList.toList()

    fun filter(query: String) {
        filteredList = if (query.isEmpty()) menuList.toList()
        else menuList.filter { it.nama.contains(query, ignoreCase = true) }
        notifyDataSetChanged()
    }

    /** Dipanggil setiap onResume agar isHabis dari UserPrefs terbaca ulang */
    fun refreshList(updatedList: List<MenuItem>) {
        filteredList = updatedList.toList()
        notifyDataSetChanged()
    }

    fun refreshQty() {
        menuList.forEach { item -> qtyMap[item.id] = CartManager.getQtyById(item.id) }
        notifyDataSetChanged()
    }

    inner class MenuViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView  = itemView.findViewById(R.id.cardMenu)
        val imgMenu: ImageView      = itemView.findViewById(R.id.imgMenu)
        val tvNama: TextView        = itemView.findViewById(R.id.tvNamaMenu)
        val tvDeskripsi: TextView   = itemView.findViewById(R.id.tvDeskripsi)
        val tvHarga: TextView       = itemView.findViewById(R.id.tvHarga)
        val btnTambah: ImageButton  = itemView.findViewById(R.id.btnTambah)
        val btnKurang: ImageButton  = itemView.findViewById(R.id.btnKurang)
        val tvQty: TextView         = itemView.findViewById(R.id.tvQty)
        val overlayHabis: View      = itemView.findViewById(R.id.overlayHabis)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_menu, parent, false)
        return MenuViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val item  = filteredList[position]
        val qty   = qtyMap[item.id] ?: 0
        val habis = item.isHabis

        holder.tvNama.text      = item.nama
        holder.tvDeskripsi.text = item.deskripsi
        holder.tvHarga.text     = CartManager.formatRupiah(item.harga)
        holder.tvQty.text       = qty.toString()

        if (item.imageRes != 0) holder.imgMenu.setImageResource(item.imageRes)
        else holder.imgMenu.setImageResource(R.drawable.ic_food_placeholder)

        if (habis) {
            // Tampilkan overlay stok habis, nonaktifkan interaksi
            holder.overlayHabis.visibility = View.VISIBLE
            holder.btnTambah.isEnabled     = false
            holder.btnKurang.isEnabled     = false
            holder.card.isClickable        = false
            holder.card.alpha              = 0.75f
        } else {
            holder.overlayHabis.visibility = View.GONE
            holder.btnTambah.isEnabled     = true
            holder.btnKurang.isEnabled     = true
            holder.card.isClickable        = true
            holder.card.alpha              = 1.0f

            holder.card.setOnClickListener { onItemClick(item.copy(qty = qty)) }

            holder.btnTambah.setOnClickListener {
                val newQty = (qtyMap[item.id] ?: 0) + 1
                qtyMap[item.id] = newQty
                holder.tvQty.text = newQty.toString()
                CartManager.updateItem(item.copy(qty = newQty))
                onQtyChanged(item.copy(qty = newQty))
            }

            holder.btnKurang.setOnClickListener {
                val current = qtyMap[item.id] ?: 0
                if (current > 0) {
                    val newQty = current - 1
                    qtyMap[item.id] = newQty
                    holder.tvQty.text = newQty.toString()
                    CartManager.updateItem(item.copy(qty = newQty))
                    onQtyChanged(item.copy(qty = newQty))
                }
            }
        }
    }

    override fun getItemCount() = filteredList.size
}
