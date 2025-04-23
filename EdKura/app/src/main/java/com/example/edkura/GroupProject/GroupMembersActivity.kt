package com.example.edkura.GroupProject

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.R
import com.google.firebase.database.*

data class Member(val id: String, val name: String)

class GroupMembersActivity : AppCompatActivity() {
    private lateinit var db: DatabaseReference
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MemberAdapter
    private val members = mutableListOf<Member>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_members)

        val groupId = intent.getStringExtra("GROUP_ID") ?: return
        recyclerView = findViewById(R.id.recyclerViewMembers)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MemberAdapter(members)
        recyclerView.adapter = adapter

        db = FirebaseDatabase.getInstance(
            "https://edkura-81d7c-default-rtdb.firebaseio.com"
        ).reference

        db.child("projectGroups").child(groupId).child("members")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    members.clear()
                    snapshot.children.forEach { child ->
                        val uid = child.key ?: return@forEach
                        // fetch that user’s name
                        db.child("users").child(uid).child("name")
                            .get().addOnSuccessListener { ds ->
                                val name = ds.getValue(String::class.java) ?: "Unknown"
                                members.add(Member(uid, name))
                                adapter.notifyDataSetChanged()
                            }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
}