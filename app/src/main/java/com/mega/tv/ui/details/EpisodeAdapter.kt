package com.mega.tv.ui.details

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.mega.tv.R
import com.mega.tv.data.model.SeriesEpisode
import com.mega.tv.databinding.ItemEpisodeCardBinding

class EpisodeAdapter(
    private var episodes: List<SeriesEpisode>,
    private val onEpisodeClick: (SeriesEpisode) -> Unit
) : RecyclerView.Adapter<EpisodeAdapter.EpisodeViewHolder>() {

    inner class EpisodeViewHolder(val binding: ItemEpisodeCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val binding = ItemEpisodeCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EpisodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        val episode = episodes[position]
        holder.binding.tvEpisodeNumber.text = "Episódio ${episode.episodeNum}"
        holder.binding.tvEpisodeTitle.text = episode.title ?: "Episódio ${episode.episodeNum}"
        holder.binding.tvEpisodePlot.text = episode.info?.plot ?: ""

        val still = episode.info?.getEffectiveImage()
        if (!still.isNullOrBlank()) {
            holder.binding.imgEpisodeStill.load(still) {
                crossfade(true)
                placeholder(R.drawable.bg_tv_card_border)
                error(R.drawable.banner)
            }
        } else {
            holder.binding.imgEpisodeStill.setImageResource(R.drawable.banner)
        }

        holder.binding.root.setOnClickListener {
            onEpisodeClick(episode)
        }
    }

    override fun getItemCount(): Int = episodes.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateEpisodes(newEpisodes: List<SeriesEpisode>) {
        episodes = newEpisodes
        notifyDataSetChanged()
    }
}
