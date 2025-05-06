package com.example.edkura.GroupProject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.R

class MemberAdapter(
    private val items: List<Member>
) : RecyclerView.Adapter<MemberAdapter.VH>() {

    var onReportClick: ((Member) -> Unit)? = null

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val nameTv: TextView = view.findViewById(R.id.textMemberName)

        init {
            nameTv.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onReportClick?.invoke(items[position])
                    return@setOnLongClickListener true // Consume the long click
                }
                return@setOnLongClickListener false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.item_member, parent, false)
    )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.nameTv.text = items[position].name
    }
}