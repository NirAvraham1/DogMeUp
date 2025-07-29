package com.example.dogmeup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OrdersAdapter(private val orders: List<Order>) :
    RecyclerView.Adapter<OrdersAdapter.OrderViewHolder>() {

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvOrderDate)
        val tvPrice: TextView = itemView.findViewById(R.id.tvOrderPrice)
        val tvStatus: TextView = itemView.findViewById(R.id.tvOrderStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        holder.tvDate.text = "${order.date} (${order.startTime} - ${order.endTime})"
        holder.tvPrice.text = "Rate: ₪${order.rate}"
        holder.tvStatus.text = "Status: ${order.status}"
    }

    override fun getItemCount() = orders.size
}
