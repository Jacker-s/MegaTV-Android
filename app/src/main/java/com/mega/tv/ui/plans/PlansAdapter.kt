package com.mega.tv.ui.plans

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mega.tv.data.model.PlanItem
import com.mega.tv.databinding.ItemPlanCardBinding
import java.util.Locale

class PlansAdapter(
    private var plans: List<PlanItem>,
    private val onPlanClick: (PlanItem) -> Unit
) : RecyclerView.Adapter<PlansAdapter.PlanViewHolder>() {

    inner class PlanViewHolder(val binding: ItemPlanCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val binding = ItemPlanCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        val plan = plans[position]
        holder.binding.tvPlanTitle.text = plan.title
        holder.binding.tvPlanPrice.text = String.format(Locale.getDefault(), "R$ %.2f", plan.price)
        holder.binding.tvPlanDuration.text = "${plan.durationDays} dias de acesso"
        holder.binding.tvPlanDescription.text = plan.description ?: "Acesso liberado a todo o catálogo"

        holder.binding.root.setOnClickListener {
            onPlanClick(plan)
        }
    }

    override fun getItemCount(): Int = plans.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newPlans: List<PlanItem>) {
        plans = newPlans
        notifyDataSetChanged()
    }
}
