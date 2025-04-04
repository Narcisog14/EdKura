package com.example.edkura.GroupProject

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.edkura.R
import com.example.edkura.models.ProjectGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class GroupDetailActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: ""
    private lateinit var leaveGroupButton: Button
    private lateinit var groupName: TextView
    private lateinit var groupDescription: TextView
    private lateinit var groupId: String
    private lateinit var projectGroup: ProjectGroup
    private lateinit var inviteMoreButton: FloatingActionButton
    private var isGroupCreator = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_detail)
        database = FirebaseDatabase.getInstance("https://edkura-81d7c-default-rtdb.firebaseio.com").reference
        //get the views
        leaveGroupButton = findViewById(R.id.leaveGroupButton)
        groupName = findViewById(R.id.groupName)
        groupDescription = findViewById(R.id.groupDescription)
        inviteMoreButton = findViewById(R.id.inviteMoreButton)
        groupId = intent.getStringExtra("GROUP_ID") ?: ""
        loadGroup()
        leaveGroupButton.setOnClickListener {
            leaveGroup()
        }
        inviteMoreButton.setOnClickListener{
            inviteMore()
        }
    }

    private fun loadGroup() {
        database.child("projectGroups").child(groupId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                projectGroup = snapshot.getValue(ProjectGroup::class.java) ?: ProjectGroup()
                groupName.text = projectGroup.name
                groupDescription.text = projectGroup.description
                //set the visibility of the buttons
                if (projectGroup.members.containsKey(currentUserId)) {
                    leaveGroupButton.visibility = View.VISIBLE
                } else {
                    leaveGroupButton.visibility = View.GONE
                }
                // Check if current user is the group creator
                isGroupCreator = projectGroup.creator == currentUserId
                inviteMoreButton.visibility = if (isGroupCreator) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@GroupDetailActivity,
                    "Failed to load group",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
    private fun leaveGroup() {
        // Remove the current user from the members list
        projectGroup.members.remove(currentUserId)
        // Remove the current user from the invitedUsers list
        projectGroup.invitedUsers.remove(currentUserId)
        // Update the ProjectGroup in Firebase
        database.child("projectGroups").child(groupId).setValue(projectGroup)
            .addOnSuccessListener {
                // Remove group from user's invitedToGroups list
                database.child("users").child(currentUserId).child("invitedToGroups").child(groupId).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "You have left the group", Toast.LENGTH_SHORT).show()
                        finish()
                    }.addOnFailureListener {
                        Toast.makeText(this, "Failed to leave the group", Toast.LENGTH_SHORT).show()
                    }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to leave the group", Toast.LENGTH_SHORT).show()
            }
    }
    private fun inviteMore() {
        val dialog = CreateGroupDialogFragment.newInstance(groupId)
        dialog.show(supportFragmentManager, "CreateGroupDialogFragment")
    }
}