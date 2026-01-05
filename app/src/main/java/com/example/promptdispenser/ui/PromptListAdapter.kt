package com.example.promptdispenser.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.promptdispenser.data.PromptListEntity
import com.example.promptdispenser.databinding.ItemDateHeaderBinding
import com.example.promptdispenser.databinding.ItemPromptListBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PromptListAdapter(
    private val onDispense: (PromptListEntity) -> Unit,
    private val onReset: (PromptListEntity) -> Unit,
    private val onDelete: (PromptListEntity) -> Unit
) : ListAdapter<Any, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
        private val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    }

    class HeaderViewHolder(val binding: ItemDateHeaderBinding) : RecyclerView.ViewHolder(binding.root)
    class ItemViewHolder(val binding: ItemPromptListBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position) is Date) TYPE_HEADER else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemDateHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                HeaderViewHolder(binding)
            }
            else -> {
                val binding = ItemPromptListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ItemViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                val date = getItem(position) as Date
                holder.binding.textDate.text = dateFormat.format(date)
            }
            is ItemViewHolder -> {
                val item = getItem(position) as PromptListEntity
                holder.binding.textName.text = item.name
                holder.binding.textCount.text = "${item.allPrompts.size} prompts (${item.usedPrompts.size} used)"
                holder.binding.btnDispense.setOnClickListener { onDispense(item) }
                holder.binding.btnReset.setOnClickListener { onReset(item) }
                holder.binding.btnDelete.setOnClickListener { onDelete(item) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is Date && newItem is Date -> oldItem.time == newItem.time
                oldItem is PromptListEntity && newItem is PromptListEntity -> oldItem.id == newItem.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return oldItem == newItem
        }
    }
}