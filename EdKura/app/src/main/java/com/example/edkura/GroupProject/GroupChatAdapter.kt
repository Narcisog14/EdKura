package com.example.edkura.GroupProject

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class GroupChatAdapter(
    private val currentUserId: String
) : RecyclerView.Adapter<GroupChatAdapter.Holder>() {

    companion object {
        private const val TYPE_SENT = 0
        private const val TYPE_RECEIVED = 1
    }

    // each entry is Pair<messageText, senderId>
    private val messages = mutableListOf<Pair<String, String>>()
    // cache userId → displayName
    private val nameCache = mutableMapOf<String, String>()

    inner class Holder(viewType: Int, parent: ViewGroup) :
        RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(
                    if (viewType == TYPE_SENT)
                        R.layout.item_group_message_sent
                    else
                        R.layout.item_group_message_received,
                    parent,
                    false
                )
        ) {
        val msgText: TextView = itemView.findViewById(R.id.textMessage)
        val senderText: TextView = itemView.findViewById(R.id.textSenderName)
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].second == currentUserId) TYPE_SENT else TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(viewType, parent)

    override fun getItemCount() = messages.size

    override fun onBindViewHolder(holder: Holder, pos: Int) {
        val (text, senderId) = messages[pos]
        holder.msgText.text = text

        if (senderId == currentUserId) {
            holder.senderText.text = "You"
        } else {
            // use cached name if we have it
            val cached = nameCache[senderId]
            if (cached != null) {
                holder.senderText.text = cached
            } else {
                // fetch once from Firebase
                val ref = FirebaseDatabase.getInstance()
                    .reference.child("users").child(senderId).child("name")
                ref.get().addOnSuccessListener { snap ->
                    val name = snap.getValue(String::class.java) ?: "Unknown"
                    nameCache[senderId] = name
                    holder.senderText.text = name
                }
            }
        }
    }

    /** Call from your Activity when data changes: **/
    fun updateData(newMessages: List<Pair<String, String>>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }
}