package com.example.edkura.chat

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.Narciso.Student
import com.example.edkura.R
import com.example.edkura.Rao.StudyPartnerRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AllChatsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val db = FirebaseDatabase.getInstance().reference
    private val partnerList = mutableListOf<Student>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_chats)

        recyclerView = findViewById(R.id.recyclerViewAllChats)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadAllPartners()
    }

    private fun loadAllPartners() {
        db.child("study_partner_requests")
            .orderByChild("status")
            .equalTo("accepted")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    partnerList.clear()
                    snapshot.children.forEach { child ->
                        val request = child.getValue(StudyPartnerRequest::class.java) ?: return@forEach
                        val isCurrentUserInvolved = request.senderId == currentUserId || request.receiverId == currentUserId
                        if (isCurrentUserInvolved) {
                            val partnerId = if (request.senderId == currentUserId) request.receiverId else request.senderId
                            db.child("users").child(partnerId).child("name").get()
                                .addOnSuccessListener { nameSnap ->
                                    val partnerName = nameSnap.getValue(String::class.java) ?: "Unknown"
                                    partnerList.add(Student(partnerId, partnerName, "accepted", null))
                                    recyclerView.adapter = GlobalChatAdapter(partnerList)
                                }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    inner class GlobalChatAdapter(private val items: List<Student>) :
        RecyclerView.Adapter<GlobalChatAdapter.Holder>() {
        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val nameBtn: Button = view.findViewById(R.id.buttonPartnerName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = layoutInflater.inflate(R.layout.item_partner, parent, false)
            return Holder(view)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val partner = items[position]
            holder.nameBtn.text = partner.name
            holder.nameBtn.setOnClickListener {
                startActivity(Intent(this@AllChatsActivity, ChatActivity::class.java).apply {
                    putExtra("partnerId", partner.id)
                    putExtra("partnerName", partner.name)
                })
            }
        }
    }
}