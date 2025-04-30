package com.example.edkura.GroupProject

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class GroupChatActivity : AppCompatActivity() {
    private lateinit var db: DatabaseReference
    private lateinit var recycler: RecyclerView
    private lateinit var sendBtn: Button
    private lateinit var input: EditText
    private lateinit var adapter: GroupChatAdapter
    private val me = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private lateinit var groupId: String
    private lateinit var roomRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_chat)

        groupId = intent.getStringExtra("GROUP_ID") ?: error("no GROUP_ID")
        db = FirebaseDatabase.getInstance().reference
        roomRef = db.child("groupChats").child(groupId).child("messages")

        recycler = findViewById(R.id.recyclerViewGroupChat)
        sendBtn = findViewById(R.id.buttonSendGroup)
        input = findViewById(R.id.editTextGroupMessage)

        adapter = GroupChatAdapter(me)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        // load in real‐time
        roomRef.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snap: DataSnapshot) {
                val list = snap.children.mapNotNull {
                    val text = it.child("text").getValue(String::class.java)
                    val sender = it.child("senderId").getValue(String::class.java)
                    if (text!=null && sender!=null) text to sender else null
                }
                adapter.updateData(list)
                recycler.scrollToPosition(list.size-1)
            }
            override fun onCancelled(e: DatabaseError) {}
        })

        sendBtn.setOnClickListener {
            val txt = input.text.toString().trim()
            if(txt.isNotEmpty()) {
                roomRef.push().setValue(mapOf(
                    "senderId" to me,
                    "text" to txt,
                    "timestamp" to ServerValue.TIMESTAMP
                ))
                input.setText("")
            }
        }
    }
}