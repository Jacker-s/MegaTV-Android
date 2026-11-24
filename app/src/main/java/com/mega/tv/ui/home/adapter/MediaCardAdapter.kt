package com.mega.tv.ui.home.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.mega.tv.R
import com.mega.tv.databinding.ItemMediaCardBinding

class MediaCardAdapter(
    private var items: List<MediaCardItem>,
    private val onItemClick: (MediaCardItem) -> Unit
) : RecyclerView.Adapter<MediaCardAdapter.MediaViewHolder>() {

    inner class MediaViewHolder(val binding: ItemMediaCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvCardTitle.text = item.title

        if (!item.subtitle.isNullOrBlank()) {
            holder.binding.tvCardSubtitle.text = item.subtitle
            holder.binding.tvCardSubtitle.visibility = View.VISIBLE
        } else {
            holder.binding.tvCardSubtitle.visibility = View.GONE
        }

        // Poster Image
        if (!item.posterUrl.isNullOrBlank()) {
            holder.binding.imgCardPoster.load(item.posterUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_tv_card_border)
                error(R.drawable.banner)
            }
        } else {
            holder.binding.imgCardPoster.setImageResource(R.drawable.banner)
        }

        // Progress bar for "Continuar Assistindo"
        if (item.progressPercentage > 0) {
            holder.binding.pbCardProgress.progress = item.progressPercentage
            holder.binding.pbCardProgress.visibility = View.VISIBLE
        } else {
            holder.binding.pbCardProgress.visibility = View.GONE
        }

        holder.binding.root.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateItems(newItems: List<MediaCardItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
