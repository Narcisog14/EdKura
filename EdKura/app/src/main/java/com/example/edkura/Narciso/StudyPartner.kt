package com.example.edkura.Narciso

import android.content.Intent
import android.view.View
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.example.edkura.R
import com.example.edkura.Rao.StudyPartnerRequest
import com.example.edkura.chat.ChatActivity

class StudyPartner : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ArrayAdapter<String>
    private var partnerList = mutableListOf<Pair<String, String>>() // Pair<name, userId>
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.study_partner_dashboard)

        recyclerView = findViewById(R.id.studentsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        recyclerView.adapter = SimpleAdapter()

        database = FirebaseDatabase.getInstance().reference
        loadAcceptedPartners()
    }

    private fun loadAcceptedPartners() {
        database.child("study_partner_requests")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    partnerList.clear()

                    snapshot.children.forEach { requestSnapshot ->
                        val request = requestSnapshot.getValue(StudyPartnerRequest::class.java)
                        if (request?.status == "accepted" &&
                            (request.receiverId == currentUserId || request.senderId == currentUserId)
                        ) {
                            val partnerId = if (request.senderId == currentUserId) request.receiverId else request.senderId
                            val partnerNameRef = database.child("users").child(partnerId).child("name")
                            partnerNameRef.get().addOnSuccessListener { nameSnap ->
                                val partnerName = nameSnap.getValue(String::class.java) ?: "Unknown"
                                partnerList.add(Pair(partnerName, partnerId))
                                recyclerView.adapter = SimpleAdapter()
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@StudyPartner, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    inner class SimpleAdapter : RecyclerView.Adapter<SimpleAdapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nameView: TextView = view.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount() = partnerList.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val (name, id) = partnerList[position]
            holder.nameView.text = name
            holder.itemView.setOnClickListener {
                val intent = Intent(this@StudyPartner, ChatActivity::class.java)
                intent.putExtra("partnerId", id)
                intent.putExtra("partnerName", name)
                startActivity(intent)
            }
        }
    }
}
