package com.example.edkura.GroupProject

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import android.content.Intent
import com.example.edkura.R
import com.example.edkura.models.ProjectGroup
import com.example.edkura.Deadlines.DeadlinesActivity
import com.example.edkura.FileSharing.FileMessage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    private lateinit var leaveGroupButton: FloatingActionButton
    private var groupId: String? = null
    private var currentGroup: ProjectGroup? = null
    private lateinit var groupInviteList: MutableList<GroupInvite>
    private lateinit var buttonsLinearLayout: LinearLayout
    private lateinit var cardViewDeadlines: CardView

    private var currentUserCourse: String = ""



    data class GroupInvite(
        val inviteId: String = "",
        val groupId: String = "",
        val invitedBy: String = "",
        val groupName: String = "",
        val invitedByUserName: String = "",
        var status: String = ""
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_project_dashboard)

        // Initialize views
        dashboardTitle = findViewById(R.id.dashboardTitle)
        addUserItem = findViewById(R.id.addUserItem)
        promptMessage = findViewById(R.id.promptMessage)
        groupDescription = findViewById(R.id.groupDescription)
        leaveGroupButton = findViewById(R.id.leaveGroupButton)
        groupInviteList = mutableListOf()
        cardViewDeadlines = findViewById(R.id.cardViewDeadlines)
        buttonsLinearLayout = findViewById(R.id.buttonsLinearLayout)

        currentUserCourse = intent.getStringExtra("courseName") ?: ""


        database = FirebaseDatabase
            .getInstance("https://edkura-81d7c-default-rtdb.firebaseio.com")
            .reference

        // Initially hide the group description and show the prompt
        groupDescription.visibility = View.GONE
        promptMessage.visibility = View.VISIBLE
        // Initially hide leave group button and the cardviews
        leaveGroupButton.visibility = View.GONE
        buttonsLinearLayout.visibility = View.GONE


        leaveGroupButton.setOnClickListener {
            showLeaveGroupConfirmationDialog()
        }

        // Set the click listener for the card view
        addUserItem.setOnClickListener {
            showCreateOrJoinDialog()
        }
        cardViewDeadlines.setOnClickListener {
            Log.d("GroupProjectDashboard", "Deadlines button clicked!")
            val intent = Intent(this, DeadlinesActivity::class.java)
            startActivity(intent)
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
            loadGroupInvites()
            dialog.dismiss()
        }

        // Check if the user is in a group before showing the "Invite to Group" button
        if (groupId != null) {
            builder.setNeutralButton("Invite to Group") { dialog, _ ->
                val inviteMoreDialogFragment = InviteMoreDialogFragment.newInstance(groupId ?: "")
                inviteMoreDialogFragment.show(supportFragmentManager, "inviteMoreDialogFragment")
                dialog.dismiss()
            }
        }

        builder.show()
    }

    private fun loadGroups() {
        database.child("projectGroups").orderByChild("members/$currentUserId").equalTo(true)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (dataSnapshot in snapshot.children) {
                            val projectGroup = dataSnapshot.getValue(ProjectGroup::class.java) ?: continue
                            Log.d("projectGroupCourse", projectGroup.course)
                            if (projectGroup.course.contains(currentUserCourse)) {
                                groupId = dataSnapshot.key
                                Log.d("currentUserCourse2", currentUserCourse)
                                loadGroupDetails()
                            }
                        }
                        //show the buttons
                        updateButtonsVisibility(true)
                    } else {
                        updateUI(null)
                        //hide the buttons
                        updateButtonsVisibility(false)
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

    private fun loadGroupDetails() {
        if (groupId != null) {
            database.child("projectGroups").child(groupId!!)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        currentGroup = snapshot.getValue(ProjectGroup::class.java)
                        updateUI(currentGroup)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(
                            this@GroupProjectDashboardActivity,
                            "Failed to load group details",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }
    }

    private fun updateUI(group: ProjectGroup?) {
        if (group != null) {
            // If a group exists, update the UI to reflect the group's information
            promptMessage.visibility = View.GONE
            groupDescription.visibility = View.VISIBLE
            dashboardTitle.text = group.name
            groupDescription.text = group.description

        } else {
            // If no group exists, update the UI to prompt the user to create or join one
            promptMessage.visibility = View.VISIBLE
            groupDescription.visibility = View.GONE
            dashboardTitle.text = getString(R.string.group_dashboard_title)
        }
    }
    private fun updateButtonsVisibility(isMember:Boolean){
        if(isMember){
            leaveGroupButton.isVisible = true
            buttonsLinearLayout.isVisible = true
        } else {
            leaveGroupButton.isVisible = false
            buttonsLinearLayout.isVisible = false
        }
    }

    private fun showLeaveGroupConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Leave Group")
            .setMessage("Are you sure you want to leave this group?")
            .setPositiveButton("Leave") { _, _ -> leaveGroup() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun leaveGroup() {
        if (groupId != null && currentUserId.isNotEmpty()) {
            val groupRef = database.child("projectGroups").child(groupId!!)
            groupRef.child("members").child(currentUserId).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(this, "You have left the group", Toast.LENGTH_SHORT).show()
                    loadGroups() // Reload groups to update the UI
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to leave group", Toast.LENGTH_SHORT).show()
                    Log.e("GroupProjectDashboard", "Failed to leave group", it)
                }
        }
    }

    private fun loadGroupInvites() {
        database.child("groupInvites").orderByChild("userId").equalTo(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    groupInviteList.clear()
                    if (snapshot.exists()) {
                        val totalInvites = snapshot.childrenCount.toInt()
                        var processedInvites = 0

                        for (inviteSnapshot in snapshot.children) {
                            val inviteId = inviteSnapshot.key ?: ""
                            val groupId = inviteSnapshot.child("groupId").getValue(String::class.java) ?: ""

                            // Retrieve group details for each invite
                            database.child("projectGroups").child(groupId)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(groupSnapshot: DataSnapshot) {
                                        val group = groupSnapshot.getValue(ProjectGroup::class.java)
                                        if (group != null) {
                                            val invitedBy = group.creator
                                            val invitedByUserNameRef = database.child("users").child(invitedBy).child("name")
                                            invitedByUserNameRef.get().addOnSuccessListener { userNameSnapshot ->
                                                val invitedByUserName = userNameSnapshot.getValue(String::class.java) ?: ""

                                                val groupInvite = GroupInvite(
                                                    inviteId = inviteId,
                                                    groupId = groupId,
                                                    invitedBy = invitedBy,
                                                    groupName = group.name,
                                                    invitedByUserName = invitedByUserName,
                                                    status = "Pending"
                                                )
                                                groupInviteList.add(groupInvite)

                                                // Show invites if all are loaded
                                                processedInvites++
                                                if (processedInvites == totalInvites) {
                                                    showGroupInvitesDialog()
                                                }
                                            }.addOnFailureListener {
                                                Toast.makeText(
                                                    this@GroupProjectDashboardActivity,
                                                    "failed to load the invited by",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Toast.makeText(
                                            this@GroupProjectDashboardActivity,
                                            "failed to load group details",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                })
                        }
                    } else {
                        showGroupInvitesDialog()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@GroupProjectDashboardActivity,
                        "Failed to load invites",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun showGroupInvitesDialog() {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle("Group Invites")

        if (groupInviteList.isEmpty()) {
            builder.setMessage("No pending group invites.")
            builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        } else {
            val inviteNames = groupInviteList.map {
                "${it.groupName} by ${it.invitedByUserName}"
            }.toTypedArray()

            builder.setItems(inviteNames) { _, which ->
                val selectedInvite = groupInviteList[which]
                showAcceptOrRejectDialog(selectedInvite)
            }
        }

        builder.show()
    }

    private fun showAcceptOrRejectDialog(invite: GroupInvite) {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle("Invite to ${invite.groupName} by ${invite.invitedByUserName}")
        builder.setMessage("Do you want to accept or reject this invitation?")

        builder.setPositiveButton("Accept") { dialog, _ ->
            acceptGroupInvite(invite)
            dialog.dismiss()
        }

        builder.setNegativeButton("Reject") { dialog, _ ->
            rejectGroupInvite(invite)
            dialog.dismiss()
        }

        builder.show()
    }
    private fun acceptGroupInvite(invite: GroupInvite) {
        // Update projectGroups
        val groupRef = database.child("projectGroups").child(invite.groupId)
        groupRef.child("members").child(currentUserId).setValue(true)
            .addOnSuccessListener {
                groupRef.child("invitedUsers").child(currentUserId).removeValue()
                    .addOnSuccessListener {
                        // Update user
                        database.child("groupInvites").child(invite.inviteId).removeValue()
                            .addOnSuccessListener {
                                Toast.makeText(this, "Joined group successfully", Toast.LENGTH_SHORT).show()
                                loadGroups()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Failed to remove invite", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this,"failed to remove the user from the invitedUsers",Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to join group", Toast.LENGTH_SHORT).show()
            }
    }

    private fun rejectGroupInvite(invite: GroupInvite) {
        database.child("groupInvites").child(invite.inviteId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Invite rejected", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to reject invite", Toast.LENGTH_SHORT).show()
            }
    }
}