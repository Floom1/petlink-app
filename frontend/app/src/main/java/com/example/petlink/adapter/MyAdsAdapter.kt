package com.example.petlink.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.petlink.R
import com.example.petlink.data.model.AnimalReq

class MyAdsAdapter(
    private val onClick: (AnimalReq) -> Unit
) : RecyclerView.Adapter<MyAdsAdapter.VH>() {

    private val items = mutableListOf<AnimalReq>()

    fun submitList(data: List<AnimalReq>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_application, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.bind(item)
        holder.itemView.setOnClickListener { onClick(item) }
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        private val tvSubtitle: TextView = view.findViewById(R.id.tvSubtitle)
        private val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        private val tvRisk: TextView = view.findViewById(R.id.tvRisk)
        private val tvDate: TextView = view.findViewById(R.id.tvDate)

        fun bind(item: AnimalReq) {
            tvTitle.text = item.name ?: "Без имени"
            tvSubtitle.text = ""
            tvRisk.visibility = View.GONE
            tvDate.text = ""
            tvStatus.text = when (item.status) {
                else -> ""
            }
        }
    }
}
