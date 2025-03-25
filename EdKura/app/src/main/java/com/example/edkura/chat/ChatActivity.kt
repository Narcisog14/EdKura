package com.example.edkura.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.R
import com.google.firebase.database.DatabaseReference
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var sendButton: Button
    private lateinit var messageEditText: EditText

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private lateinit var chatRoomId: String
    private lateinit var messagesAdapter: ArrayAdapter<String>
    private var messagesList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val partnerId = intent.getStringExtra("partnerId") ?: ""
        val partnerName = intent.getStringExtra("partnerName") ?: ""

        title = "Chat with $partnerName"

        messagesRecyclerView = findViewById(R.id.recyclerViewMessages)
        sendButton = findViewById(R.id.buttonSend)
        messageEditText = findViewById(R.id.editTextMessage)

        messagesRecyclerView.layoutManager = LinearLayoutManager(this)
        messagesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, messagesList)
        messagesRecyclerView.adapter = SimpleMessageAdapter()

        database = FirebaseDatabase.getInstance().reference
        chatRoomId = if (currentUserId < partnerId) "${currentUserId}_$partnerId" else "${partnerId}_$currentUserId"

        loadMessages()

        sendButton.setOnClickListener {
            val message = messageEditText.text.toString()
            if (message.isNotEmpty()) {
                sendMessage(message)
            }
        }
    }

    private fun loadMessages() {
        database.child("chats").child(chatRoomId).child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = snapshot.children.map {
                        val text = it.child("text").getValue(String::class.java) ?: ""
                        val sender = it.child("senderId").getValue(String::class.java)
                        Pair(if(sender == currentUserId) "$text" else text, sender == currentUserId)
                    }
                    messagesRecyclerView.adapter = ChatMessageAdapter(list)
                    messagesRecyclerView.scrollToPosition(list.size - 1)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }


    private fun sendMessage(text: String) {
        val newMessageRef = database.child("chats").child(chatRoomId).child("messages").push()
        newMessageRef.setValue(mapOf(
            "senderId" to currentUserId,
            "text" to text,
            "timestamp" to ServerValue.TIMESTAMP
        ))
        messageEditText.setText("")
    }

    inner class SimpleMessageAdapter : RecyclerView.Adapter<SimpleMessageAdapter.MessageHolder>() {
        inner class MessageHolder(view: View) : RecyclerView.ViewHolder(view) {
            val messageTextView: TextView = view.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            return MessageHolder(view)
        }

        override fun getItemCount() = messagesList.size

        override fun onBindViewHolder(holder: MessageHolder, position: Int) {
            holder.messageTextView.text = messagesList[position]
        }
    }
}