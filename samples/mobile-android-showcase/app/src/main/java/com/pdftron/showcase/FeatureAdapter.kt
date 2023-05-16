package com.pdftron.showcase

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.pdftron.showcase.helpers.setPicassoDrawable
import com.pdftron.showcase.models.Feature
import java.util.*

class FeatureAdapter(private val context: Context, private val mFeatures: ArrayList<Feature>, private var onItemClick: ((Feature) -> Unit)) : androidx.recyclerview.widget.RecyclerView.Adapter<FeatureAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_feature, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val feature = mFeatures[position]
        holder.apply {
            imageView.setPicassoDrawable(imageView.context, "thumbnail_" + feature.imageName!!)
            nameLabel!!.text = feature.name
        }
    }

    override fun getItemCount(): Int {
        return mFeatures.size
    }

    inner class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        var imageView: ImageView
        var nameLabel: TextView? = null

        init {
            itemView.setSafeOnClickListener {
                onItemClick.invoke(mFeatures[adapterPosition])
            }
            imageView = itemView.findViewById(R.id.imageView) as ImageView
            nameLabel = itemView.findViewById(R.id.nameLabel) as TextView
        }

    }

}