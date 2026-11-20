package com.vagdroid.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vagdroid.R

/** Простой список строк с обработчиком клика. */
class SimpleAdapter(
    private val items: MutableList<String> = mutableListOf(),
    private val onClick: (Int) -> Unit = {}
) : RecyclerView.Adapter<SimpleAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tv: TextView = view.findViewById(R.id.tvLine)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_line, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.tv.text = items[position]
        holder.itemView.setOnClickListener { onClick(holder.bindingAdapterPosition) }
    }

    fun setItems(newItems: List<String>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun addItem(item: String) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }

    fun getItem(position: Int): String? = items.getOrNull(position)
}
