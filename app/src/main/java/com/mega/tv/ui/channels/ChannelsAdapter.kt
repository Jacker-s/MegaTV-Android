package com.mega.tv.ui.channels

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.mega.tv.R
import com.mega.tv.data.model.LiveStream
import com.mega.tv.databinding.ItemChannelCardBinding

class ChannelsAdapter(
    private var channels: List<LiveStream>,
    private val onChannelClick: (LiveStream) -> Unit
) : RecyclerView.Adapter<ChannelsAdapter.ChannelViewHolder>() {

    inner class ChannelViewHolder(val binding: ItemChannelCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val binding = ItemChannelCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChannelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val channel = channels[position]
        holder.binding.tvChannelName.text = channel.name

        if (!channel.streamIcon.isNullOrBlank()) {
            holder.binding.imgChannelLogo.load(channel.streamIcon) {
                crossfade(true)
                placeholder(R.drawable.bg_tv_card_border)
                error(R.drawable.banner)
            }
        } else {
            holder.binding.imgChannelLogo.setImageResource(R.drawable.banner)
        }

        holder.binding.root.setOnClickListener {
            onChannelClick(channel)
        }
    }

    override fun getItemCount(): Int = channels.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateChannels(newChannels: List<LiveStream>) {
        channels = newChannels
        notifyDataSetChanged()
    }
}
