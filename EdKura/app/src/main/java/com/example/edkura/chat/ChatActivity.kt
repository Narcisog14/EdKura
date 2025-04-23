package com.example.edkura.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
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
    private lateinit var unblockButton: Button

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private lateinit var chatRoomId: String
    private var messagesList = mutableListOf<Pair<String, Boolean>>()
    private var isBlocked = false

    private val prefs by lazy { getSharedPreferences("chat_prefs", MODE_PRIVATE) }

    // helper to persist each room’s “last read” timestamp
    private fun setLastRead(roomId: String, ts: Long) {
        prefs.edit().putLong("last_read_$roomId", ts).apply()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        findViewById<TextView>(R.id.chatPartnerName).text = intent.getStringExtra("partnerName") ?: "Chat"

        val partnerId = intent.getStringExtra("partnerId") ?: ""
        val partnerName = intent.getStringExtra("partnerName") ?: ""

        title = "Chat with $partnerName"

        messagesRecyclerView = findViewById(R.id.recyclerViewMessages)
        sendButton = findViewById(R.id.buttonSend)
        messageEditText = findViewById(R.id.editTextMessage)
        unblockButton = findViewById(R.id.buttonUnblock)

        messagesRecyclerView.layoutManager = LinearLayoutManager(this)
        database = FirebaseDatabase.getInstance().reference
        chatRoomId = if (currentUserId < partnerId) "${currentUserId}_$partnerId" else "${partnerId}_$currentUserId"

        checkBlockStatus(partnerId)
        loadMessages()

        sendButton.setOnClickListener {
            if (!isBlocked) {
                val message = messageEditText.text.toString()
                if (message.isNotEmpty()) {
                    sendMessage(message)
                }
            } else {
                showToast("This partner is blocked. Unblock to chat.")
            }
        }

        unblockButton.setOnClickListener {
            unblockPartner(partnerId)
        }
    }

    override fun onResume() {
        super.onResume()
        // mark “everything up to right now” as read
        setLastRead(chatRoomId, System.currentTimeMillis())
    }

    private fun checkBlockStatus(partnerId: String) {
        database.child("study_partner_requests")
            .orderByChild("status")
            .equalTo("blocked")
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    isBlocked = false
                    snapshot.children.forEach { child ->
                        val request = child.getValue(com.example.edkura.Rao.StudyPartnerRequest::class.java)
                        if (request != null &&
                            ((request.senderId == currentUserId && request.receiverId == partnerId) ||
                                    (request.receiverId == currentUserId && request.senderId == partnerId))
                        ) {
                            isBlocked = true
                        }
                    }
                    if (isBlocked) {
                        sendButton.isEnabled = false
                        messageEditText.isEnabled = false
                        unblockButton.visibility = View.VISIBLE
                        showToast("This partner is blocked. Unblock to chat.")
                    } else {
                        sendButton.isEnabled = true
                        messageEditText.isEnabled = true
                        unblockButton.visibility = View.GONE
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun unblockPartner(partnerId: String) {
        database.child("study_partner_requests")
            .orderByChild("status")
            .equalTo("blocked")
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach { child ->
                        val request = child.getValue(com.example.edkura.Rao.StudyPartnerRequest::class.java)
                        if (request != null &&
                            ((request.senderId == currentUserId && request.receiverId == partnerId) ||
                                    (request.receiverId == currentUserId && request.senderId == partnerId))
                        ) {
                            child.ref.child("status").setValue("accepted").addOnSuccessListener {
                                showToast("Partner unblocked")
                                checkBlockStatus(partnerId)
                            }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadMessages() {
        database.child("chats").child(chatRoomId).child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messagesList.clear()
                    snapshot.children.forEach {
                        val text = it.child("text").getValue(String::class.java) ?: ""
                        val sender = it.child("senderId").getValue(String::class.java)
                        messagesList.add(Pair(text, sender == currentUserId))
                    }
                    messagesRecyclerView.adapter = ChatMessageAdapter(messagesList)
                    messagesRecyclerView.scrollToPosition(messagesList.size - 1)
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

    private fun showToast(msg: String) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
    }
}