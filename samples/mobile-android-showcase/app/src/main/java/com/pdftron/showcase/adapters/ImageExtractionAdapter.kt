package com.pdftron.showcase.adapters

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.pdftron.showcase.R
import com.squareup.picasso.Picasso
import java.io.File


internal class ImageExtractionAdapter(@param:NonNull @field:NonNull
                          private val mImages: ArrayList<String>, private val mSize: Int) : androidx.recyclerview.widget.RecyclerView.Adapter<ImageExtractionAdapter.ViewHolder>() {

    @NonNull
    override fun onCreateViewHolder(@NonNull viewGroup: ViewGroup, i: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.item_image, viewGroup, false)
        return ImageExtractionAdapter.ViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull viewHolder: ViewHolder, i: Int) {
        val path = getItem(i)
        Picasso.get()
                .load(File(path))
                .resize(mSize, mSize)
                .centerCrop()
                .into(viewHolder.mImageView)
    }

    @Nullable
    fun getItem(i: Int): String {
        return mImages[i]
    }

    override fun getItemCount(): Int {
        return mImages.size
    }

    internal class ViewHolder(@NonNull itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {

        var mImageView: ImageView = itemView.findViewById(R.id.image_view)

    }
}