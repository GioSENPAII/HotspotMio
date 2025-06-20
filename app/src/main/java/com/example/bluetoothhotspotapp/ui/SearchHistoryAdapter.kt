package com.example.bluetoothhotspotapp.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothhotspotapp.data.model.SearchHistory
import com.example.bluetoothhotspotapp.databinding.ItemSearchHistoryBinding

class SearchHistoryAdapter : ListAdapter<SearchHistory, SearchHistoryAdapter.SearchHistoryViewHolder>(
    DiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchHistoryViewHolder {
        val binding = ItemSearchHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchHistoryViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(currentItem)
    }

    class SearchHistoryViewHolder(private val binding: ItemSearchHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(searchHistory: SearchHistory) {
            binding.apply {
                textViewQuery.text = searchHistory.query
                textViewClientName.text = searchHistory.clientName
                textViewTime.text = searchHistory.getFormattedTime()
                textViewResultsCount.text = "${searchHistory.resultsCount} resultados"
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SearchHistory>() {
        override fun areItemsTheSame(oldItem: SearchHistory, newItem: SearchHistory) =
            oldItem.timestamp == newItem.timestamp
        override fun areContentsTheSame(oldItem: SearchHistory, newItem: SearchHistory) =
            oldItem == newItem
    }
}