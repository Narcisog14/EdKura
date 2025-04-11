package com.example.edkura.GroupProject

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.edkura.R
import com.example.edkura.models.ProjectGroup
import com.example.edkura.GroupProject.CreateGroupDialogFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class GroupProjectDashboardActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: ""
    private lateinit var dashboardTitle: TextView
    private lateinit var addUserItem: FloatingActionButton
    private lateinit var promptMessage: TextView
    private lateinit var groupDescription: TextView
    private val groups = mutableListOf<ProjectGroup>()
    private var groupName = "" // Declared as class property

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_project_dashboard)

        // Initialize views
        dashboardTitle = findViewById(R.id.dashboardTitle)
        addUserItem = findViewById(R.id.addUserItem)
        promptMessage = findViewById(R.id.promptMessage)
        groupDescription = findViewById(R.id.groupDescription)

        database = FirebaseDatabase
            .getInstance("https://edkura-81d7c-default-rtdb.firebaseio.com")
            .reference

        // Initially hide the group list and show the prompt
        groupDescription.visibility = View.GONE
        promptMessage.visibility = View.VISIBLE

        // Set the click listener for the card view
        addUserItem.setOnClickListener {
            showCreateOrJoinDialog()
        }

        loadGroups()
    }

    private fun showCreateOrJoinDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Create or Join a Group")

        builder.setPositiveButton("Create Group") { dialog, _ ->
            val createGroupDialog = CreateGroupDialogFragment()
            createGroupDialog.show(supportFragmentManager, "CreateGroupDialog")
            dialog.dismiss()
        }

        builder.setNegativeButton("Join Group") { dialog, _ ->
            // Show join group dialog or navigate to join group activity
            Toast.makeText(this, "Join Group Clicked", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        builder.show()
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
                            if (groups.isNotEmpty()){
                                groupName = it.name
                                updateUI(it)
                            }else{
                                updateUI(null)
                            }

                        }
                    }

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

    private fun updateUI(group: ProjectGroup?) {
        if (group != null) {
            // Show group name if there are groups
            dashboardTitle.text = group.name
            groupDescription.visibility = View.VISIBLE
            groupDescription.text = "${group.description}"//Replace with the actual group description
            promptMessage.visibility = View.GONE
        } else {
            // Show prompt if there are no groups
            dashboardTitle.text = ""
            groupDescription.visibility = View.GONE
            promptMessage.visibility = View.VISIBLE
        }
    }
}