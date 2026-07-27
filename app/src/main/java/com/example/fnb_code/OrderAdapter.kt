package com.fnbioscoop

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fnbioscoop.R
import com.google.android.material.button.MaterialButton

class OrderAdapter(
    private val onConfirm: (OrderItem) -> Unit,
    private val onTolak: (OrderItem) -> Unit
) : ListAdapter<OrderItem, OrderAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<OrderItem>() {
            override fun areItemsTheSame(a: OrderItem, b: OrderItem) = a.nomor == b.nomor
            override fun areContentsTheSame(a: OrderItem, b: OrderItem) = a == b
        }
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvNomor: TextView   = v.findViewById(R.id.tvOrderNomor)
        val tvUser: TextView    = v.findViewById(R.id.tvOrderUser)
        val tvItems: TextView   = v.findViewById(R.id.tvOrderItems)
        val tvTotal: TextView   = v.findViewById(R.id.tvOrderTotal)
        val tvMetode: TextView  = v.findViewById(R.id.tvOrderMetode)
        val tvTanggal: TextView = v.findViewById(R.id.tvOrderTanggal)
        val tvStatus: TextView  = v.findViewById(R.id.tvOrderStatus)
        val tvCatatan: TextView = v.findViewById(R.id.tvOrderCatatan)
        val layoutCatatan: View = v.findViewById(R.id.layoutOrderCatatan)
        val btnConfirm: MaterialButton = v.findViewById(R.id.btnConfirmOrder)
        val btnTolak: MaterialButton   = v.findViewById(R.id.btnTolakOrder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_order_admin, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val order = getItem(position)
        holder.tvNomor.text   = order.nomor
        holder.tvUser.text    = "${order.namaUser} (@${order.username})"
        holder.tvItems.text   = order.items
        holder.tvTotal.text   = CartManager.formatRupiah(order.total)
        holder.tvMetode.text  = order.metode
        holder.tvTanggal.text = order.tanggal

        if (order.catatan.isNotEmpty()) {
            holder.layoutCatatan.visibility = View.VISIBLE
            holder.tvCatatan.text = order.catatan
        } else {
            holder.layoutCatatan.visibility = View.GONE
        }

        holder.tvStatus.text = "⏳ Menunggu"
        holder.tvStatus.setTextColor(Color.parseColor("#FF9800"))
        holder.btnConfirm.visibility = View.VISIBLE
        holder.btnTolak.visibility   = View.VISIBLE
        holder.btnConfirm.setOnClickListener { onConfirm(order) }
        holder.btnTolak.setOnClickListener   { onTolak(order) }
    }
}
