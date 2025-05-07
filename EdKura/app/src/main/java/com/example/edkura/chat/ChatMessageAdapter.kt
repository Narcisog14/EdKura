package com.example.edkura.chat

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.R

class ChatMessageAdapter(
    private var messages: MutableList<ChatActivity.ChatMessage>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_SENT = 0
        private const val TYPE_RECEIVED = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isSentByCurrentUser) TYPE_SENT else TYPE_RECEIVED
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layout = if (viewType == TYPE_SENT) R.layout.item_group_message_sent else R.layout.item_group_message_received
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return object : RecyclerView.ViewHolder(view) {}
    }

    override fun getItemCount() = messages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val (text, sender, isSentByCurrentUser) = messages[position]
        holder.itemView.findViewById<TextView>(R.id.textMessage).text = text

        if (!isSentByCurrentUser) {
            Log.d("ChatMessageAdapter", "Sender: $sender")
            holder.itemView.findViewById<TextView>(R.id.textSenderName).text = sender
        }
    }
}

