package com.example.petlink.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.example.petlink.R
import com.example.petlink.data.model.AnimalPhotoReq

class AnimalPhotoAdapter(private val photos: List<AnimalPhotoReq>) : PagerAdapter() {

    override fun getCount(): Int = if (photos.isEmpty()) 1 else photos.size

    override fun isViewFromObject(view: View, `object`: Any): Boolean = view == `object`

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = LayoutInflater.from(container.context)
            .inflate(R.layout.item_photo, container, false)
        val imageView = view.findViewById<ImageView>(R.id.imageView)

        if (photos.isEmpty()) {
            // Show default image when no photos
            Glide.with(container.context)
                .load(R.drawable.placeholder_animal)
                .into(imageView)
        } else {
            val photo = photos[position]
            Glide.with(container.context)
                .load(photo.photo_url)
                .placeholder(R.drawable.placeholder_animal)
                .error(R.drawable.placeholder_animal)
                .into(imageView)
        }

        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }
}