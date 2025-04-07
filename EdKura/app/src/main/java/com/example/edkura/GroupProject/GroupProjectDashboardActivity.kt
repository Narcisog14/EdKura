package com.example.edkura.GroupProject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.R
import com.example.edkura.adapters.ProjectGroupsAdapter
import com.example.edkura.models.ProjectGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class GroupProjectDashboardActivity : AppCompatActivity() {

    private lateinit var adapter: ProjectGroupsAdapter
    private lateinit var database: DatabaseReference
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: ""
    private lateinit var dashboardTitle: TextView
    private lateinit var groupsRecyclerView: RecyclerView
    private lateinit var backButton: Button
    private lateinit var addUserItem: CardView
    private val groups = mutableListOf<ProjectGroup>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_project_dashboard)
        //get the views
        dashboardTitle = findViewById(R.id.dashboardTitle)
        groupsRecyclerView = findViewById(R.id.noteListContainer)
        backButton = findViewById(R.id.backButton)
        addUserItem = findViewById(R.id.addUserItem)

        database = FirebaseDatabase
            .getInstance("https://edkura-81d7c-default-rtdb.firebaseio.com")
            .reference
        adapter = ProjectGroupsAdapter(groups, this::onGroupClick) // Pass the click listener
        groupsRecyclerView.layoutManager = LinearLayoutManager(this)
        groupsRecyclerView.adapter = adapter

        // Set the click listener for the card view
        addUserItem.setOnClickListener {
            // Show the create group dialog
            val dialog = CreateGroupDialogFragment()
            dialog.show(supportFragmentManager, "CreateGroupDialog")
        }
        //other code
        backButton.setOnClickListener {
            finish()
        }
        loadGroups()
    }

    private fun loadGroups() {
        database.child("projectGroups").orderByChild("members/$currentUserId").equalTo(true)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    groups.clear()
                    for (dataSnapshot in snapshot.children) {
                        val group = dataSnapshot.getValue(ProjectGroup::class.java)
                        group?.let {
                            groups.add(it)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@GroupProjectDashboardActivity,
                        "Failed to load groups",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun onGroupClick(group: ProjectGroup) {
        // Open GroupDetailActivity
        val intent = Intent(this, GroupDetailActivity::class.java)
        intent.putExtra("GROUP_ID", group.groupId)
        startActivity(intent)
    }
}