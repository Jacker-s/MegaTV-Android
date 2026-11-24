package com.mega.tv.ui.home.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mega.tv.databinding.ItemCategoryRowBinding

data class CategoryRowModel(
    val id: String,
    val title: String,
    val items: List<MediaCardItem>
)

class CategoryRowAdapter(
    private var rows: List<CategoryRowModel>,
    private val onItemClick: (MediaCardItem) -> Unit
) : RecyclerView.Adapter<CategoryRowAdapter.RowViewHolder>() {

    private val viewPool = RecyclerView.RecycledViewPool()

    inner class RowViewHolder(val binding: ItemCategoryRowBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val adapter = MediaCardAdapter(emptyList(), onItemClick)

        init {
            binding.rvRowItems.layoutManager = LinearLayoutManager(
                binding.root.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            binding.rvRowItems.setRecycledViewPool(viewPool)
            binding.rvRowItems.adapter = adapter
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        val binding = ItemCategoryRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RowViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        val row = rows[position]
        holder.binding.tvRowTitle.text = row.title
        holder.adapter.updateItems(row.items)
    }

    override fun getItemCount(): Int = rows.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateRows(newRows: List<CategoryRowModel>) {
        rows = newRows
        notifyDataSetChanged()
    }
}
