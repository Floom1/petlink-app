package com.example.petlink.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.petlink.R
import com.example.petlink.data.model.AnimalPhotoReq

class LocalPhotoAdapter(
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<LocalPhotoAdapter.VH>() {
    private val uris = mutableListOf<Uri>()

    fun submit(list: List<Uri>) {
        uris.clear()
        uris.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_edit_photo, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = uris.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(uris[position], onRemove, position)
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val iv: ImageView = v.findViewById(R.id.ivPhoto)
        private val btn: Button = v.findViewById(R.id.btnRemove)

        fun bind(uri: Uri, onRemove: (Int) -> Unit, pos: Int) {
            Glide.with(iv.context).load(uri).centerCrop().into(iv)
            btn.setOnClickListener { onRemove(pos) }
        }
    }
}

class ExistingPhotoAdapter(
    private val onRemove: (AnimalPhotoReq) -> Unit,
    private val onSelectMain: (AnimalPhotoReq) -> Unit
) : RecyclerView.Adapter<ExistingPhotoAdapter.VH>() {

    private val photos = mutableListOf<AnimalPhotoReq>()
    private var mainId: Long? = null

    fun submit(list: List<AnimalPhotoReq>, mainId: Long? = this.mainId) {
        photos.clear()
        photos.addAll(list)
        this.mainId = mainId
        notifyDataSetChanged()
    }

    fun setMainId(id: Long?) {
        mainId = id
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_edit_photo, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = photos.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val photo = photos[position]
        holder.bind(photo, onRemove, onSelectMain, isMain = (photo.id == mainId))
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val iv: ImageView = v.findViewById(R.id.ivPhoto)
        private val btn: Button = v.findViewById(R.id.btnRemove)
        private val tvMain: android.widget.TextView? = v.findViewById(R.id.tvMain)

        fun bind(
            photo: AnimalPhotoReq,
            onRemove: (AnimalPhotoReq) -> Unit,
            onSelectMain: (AnimalPhotoReq) -> Unit,
            isMain: Boolean
        ) {
            Glide.with(iv.context).load(photo.photo_url).centerCrop().into(iv)
            btn.setOnClickListener { onRemove(photo) }
            iv.setOnClickListener { onSelectMain(photo) }
            tvMain?.visibility = if (isMain) View.VISIBLE else View.GONE
        }
    }
}
