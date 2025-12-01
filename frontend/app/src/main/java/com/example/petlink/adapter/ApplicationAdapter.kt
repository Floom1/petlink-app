package com.example.petlink.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.petlink.R
import com.example.petlink.data.model.AnimalApplication

class ApplicationAdapter(
    private val role: String,
    private val onClick: (AnimalApplication) -> Unit
) : RecyclerView.Adapter<ApplicationAdapter.VH>() {

    private val items = mutableListOf<AnimalApplication>()

    fun submitList(data: List<AnimalApplication>) {
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
        holder.bind(item, role)
        holder.itemView.setOnClickListener { onClick(item) }
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        private val tvSubtitle: TextView = view.findViewById(R.id.tvSubtitle)
        private val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        private val tvRisk: TextView = view.findViewById(R.id.tvRisk)
        private val tvDate: TextView = view.findViewById(R.id.tvDate)

        fun bind(item: AnimalApplication, role: String) {
            // Для входящих (seller) заголовок — покупатель, подзаголовок — животное
            // Для моих (buyer) заголовок — животное, подзаголовок — статус
            val isSeller = role == "seller"
            if (isSeller) {
                tvTitle.text = item.buyer_name ?: "Покупатель"
                tvSubtitle.text = item.animal_name ?: "Животное"
            } else {
                tvTitle.text = item.animal_name ?: "Животное"
                // Не дублируем статус, показываем только в tvStatus
                tvSubtitle.text = ""
            }
            tvStatus.text = when (item.status) {
                "submitted" -> "Новая"
                "approved" -> "Одобрена"
                "rejected" -> "Отклонена"
                else -> item.status
            }
            if (isSeller && !item.risk_info.isNullOrEmpty()) {
                tvRisk.visibility = View.VISIBLE
                tvRisk.text = "⚠️ Есть риски!"
            } else {
                tvRisk.visibility = View.GONE
            }
            tvDate.text = formatDate(item.created_at)
        }

        private fun formatDate(createdAt: String?): String {
            if (createdAt.isNullOrEmpty()) return ""
            // Ожидаем формат ISO: YYYY-MM-DDTHH:MM:SS...
            val datePart = createdAt.split('T', ' ').firstOrNull() ?: createdAt
            val parts = datePart.split('-')
            return if (parts.size == 3) {
                "${parts[2]}.${parts[1]}.${parts[0]}"
            } else createdAt
        }
    }
}
