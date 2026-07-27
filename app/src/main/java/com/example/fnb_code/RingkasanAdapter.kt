package com.fnbioscoop

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fnb_code.MenuItem
import com.fnbioscoop.R

class RingkasanAdapter(
    private val items: List<MenuItem>
) : RecyclerView.Adapter<RingkasanAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvQty: TextView = itemView.findViewById(R.id.tvQtyRingkasan)
        val tvNama: TextView = itemView.findViewById(R.id.tvNamaRingkasan)
        val tvHarga: TextView = itemView.findViewById(R.id.tvHargaRingkasan)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ringkasan, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvQty.text = "${item.qty}x"
        holder.tvNama.text = item.nama
        holder.tvHarga.text = CartManager.formatRupiah(item.harga * item.qty)
    }

    override fun getItemCount() = items.size
}