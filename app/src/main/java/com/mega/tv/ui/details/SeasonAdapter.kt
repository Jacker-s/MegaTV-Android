package com.mega.tv.ui.details

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mega.tv.data.model.SeriesSeason
import com.mega.tv.databinding.ItemSeasonTabBinding
import com.mega.tv.ui.theme.ThemeManager
import com.mega.tv.util.AnimUtils
import com.mega.tv.util.DeviceUtils

class SeasonAdapter(
    private var seasons: List<SeriesSeason>,
    private var selectedSeason: Int = 1,
    private val onSeasonClick: (SeriesSeason) -> Unit
) : RecyclerView.Adapter<SeasonAdapter.SeasonViewHolder>() {

    inner class SeasonViewHolder(val binding: ItemSeasonTabBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeasonViewHolder {
        val binding = ItemSeasonTabBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SeasonViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SeasonViewHolder, position: Int) {
        val season = seasons[position]
        val tab = holder.binding.tvSeasonName
        tab.text = season.name ?: "Temporada ${season.seasonNumber}"

        val isSelected = season.seasonNumber == selectedSeason
        tab.isSelected = isSelected
        tab.isFocusable = true
        tab.isFocusableInTouchMode = false

        val context = tab.context
        val isTv = DeviceUtils.isTv(context)

        fun updateTabBg(focused: Boolean) {
            val density = context.resources.displayMetrics.density
            val primaryColor = ThemeManager.getPrimaryColor(context)

            val shape = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 22f * density
                when {
                    focused -> {
                        setColor(primaryColor)
                        setStroke((2.5f * density).toInt(), Color.WHITE)
                    }
                    isSelected -> {
                        setColor(primaryColor)
                        setStroke((1f * density).toInt(), Color.parseColor("#66FFFFFF"))
                    }
                    else -> {
                        setColor(Color.parseColor("#24FFFFFF"))
                        setStroke((1f * density).toInt(), Color.parseColor("#18FFFFFF"))
                    }
                }
            }
            tab.background = RippleDrawable(ColorStateList.valueOf(Color.parseColor("#44FFFFFF")), shape, null)
            tab.setTextColor(Color.WHITE)
        }

        updateTabBg(tab.isFocused)

        tab.setOnFocusChangeListener { v, hasFocus ->
            updateTabBg(hasFocus)
            if (isTv || hasFocus) {
                AnimUtils.animateFocusChange(v, hasFocus, scale = 1.06f, elevationFocused = 10f)
            }
        }

        tab.setOnClickListener {
            selectedSeason = season.seasonNumber
            notifyDataSetChanged()
            onSeasonClick(season)
        }
    }

    override fun getItemCount(): Int = seasons.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateSeasons(newSeasons: List<SeriesSeason>, initialSeason: Int = 1) {
        seasons = newSeasons
        selectedSeason = initialSeason
        notifyDataSetChanged()
    }
}
