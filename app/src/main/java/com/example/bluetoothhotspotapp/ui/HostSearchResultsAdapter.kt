package com.example.bluetoothhotspotapp.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothhotspotapp.data.model.SearchResult
import com.example.bluetoothhotspotapp.databinding.ItemHostSearchResultBinding

class HostSearchResultsAdapter : ListAdapter<SearchResult, HostSearchResultsAdapter.HostSearchResultViewHolder>(
    DiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HostSearchResultViewHolder {
        val binding = ItemHostSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HostSearchResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HostSearchResultViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(currentItem, position + 1)
    }

    class HostSearchResultViewHolder(private val binding: ItemHostSearchResultBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(searchResult: SearchResult, position: Int) {
            binding.apply {
                textViewPosition.text = "$position."
                textViewTitle.text = searchResult.title
                textViewUrl.text = searchResult.url
                textViewSnippet.text = searchResult.snippet
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SearchResult>() {
        override fun areItemsTheSame(oldItem: SearchResult, newItem: SearchResult) =
            oldItem.url == newItem.url
        override fun areContentsTheSame(oldItem: SearchResult, newItem: SearchResult) =
            oldItem == newItem
    }
}