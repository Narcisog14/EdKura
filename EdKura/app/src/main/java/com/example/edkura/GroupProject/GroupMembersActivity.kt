package com.example.edkura.GroupProject

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

data class Member(val id: String, val name: String)

class GroupMembersActivity : AppCompatActivity() {
    private lateinit var db: DatabaseReference
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MemberAdapter
    private val members = mutableListOf<Member>()
    private lateinit var groupId: String
    private val reportReasons = arrayOf(
        "Spam",
        "Hate Speech",
        "Harassment",
        "Inappropriate Content",
        "Other"
    )
    private var selectedReason: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_members)

        groupId = intent.getStringExtra("GROUP_ID") ?: return

        recyclerView = findViewById(R.id.recyclerViewMembers)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MemberAdapter(members)
        recyclerView.adapter = adapter

        db = Firebase.database(
            "https://edkura-81d7c-default-rtdb.firebaseio.com"
        ).reference

        // Set the report click listener
        adapter.onReportClick = { member ->
            showReportConfirmationDialog(member)
        }

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

    private fun showReportConfirmationDialog(member: Member) {
        AlertDialog.Builder(this)
            .setTitle("Report User")
            .setMessage("Would you like to report ${member.name}?")
            .setPositiveButton("Report") { _, _ ->
                showReportReasonDialog(member)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showReportReasonDialog(member: Member) {
        selectedReason = null
        AlertDialog.Builder(this)
            .setTitle("Select Report Reason")
            .setSingleChoiceItems(reportReasons, -1) { _, which ->
                selectedReason = reportReasons[which]
            }
            .setPositiveButton("Report") { _, _ ->
                if (selectedReason != null) {
                    createReport(member, selectedReason!!)
                } else {
                    Toast.makeText(this, "Please select a reason", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createReport(member: Member, reason: String) {
        val reportedUserId = member.id
        val reporterUserId = Firebase.auth.currentUser?.uid // Assuming you use Firebase Authentication
        if (reporterUserId == null) {
            Log.e("GroupMembersActivity", "User not authenticated")
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }
        val reportsRef = db.child("reports").child(groupId).child(reportedUserId)
        // Store the reason for the report
        val reportData = mapOf(
            "reporterId" to reporterUserId,
            "reason" to reason
        )

        reportsRef.push().setValue(reportData)
            .addOnSuccessListener {
                Log.d("GroupMembersActivity", "Report created successfully")
                Toast.makeText(this, "Report created successfully", Toast.LENGTH_SHORT).show()
                checkReports(reportedUserId)
            }
            .addOnFailureListener { e ->
                Log.e("GroupMembersActivity", "Failed to create report", e)
                Toast.makeText(this, "Failed to create report", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkReports(reportedUserId: String) {
        // Get a reference to the members in this group
        val membersRef = db.child("projectGroups").child(groupId).child("members")
        membersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(memberSnapshot: DataSnapshot) {
                val memberCount = memberSnapshot.childrenCount // Count the total number of members
                // Get a reference to the reports for the specific reported user in this group
                val reportsRef = db.child("reports").child(groupId).child(reportedUserId)
                reportsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(reportSnapshot: DataSnapshot) {
                        val reportCount = reportSnapshot.childrenCount // Count the number of reports
                        Log.d(
                            "GroupMembersActivity",
                            "Report count for $reportedUserId: $reportCount"
                        )
                        Log.d("GroupMembersActivity", "Member count for group $groupId: $memberCount")
                        // Check if the report count is greater than half the number of members
                        if (reportCount > memberCount / 2) {
                            Log.d(
                                "GroupMembersActivity",
                                "Majority report reached for $reportedUserId"
                            )
                            kickUser(reportedUserId) // Kick the user
                        } else {
                            Log.d(
                                "GroupMembersActivity",
                                "Not enough reports to kick $reportedUserId"
                            )
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("GroupMembersActivity", "Failed to check reports", error.toException())
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GroupMembersActivity", "Failed to get member count", error.toException())
            }
        })
    }

    private fun kickUser(userId: String) {
        // Remove user from group members
        db.child("projectGroups").child(groupId).child("members").child(userId)
            .removeValue()
            .addOnSuccessListener {
                Log.d("GroupMembersActivity", "User $userId kicked from group $groupId")
                Toast.makeText(this, "User kicked from group", Toast.LENGTH_SHORT).show()
                clearReports(userId)
            }
            .addOnFailureListener { e ->
                Log.e("GroupMembersActivity", "Failed to kick user $userId", e)
                Toast.makeText(this, "Failed to kick user", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearReports(reportedUserId: String) {
        db.child("reports").child(groupId).child(reportedUserId).removeValue()
            .addOnSuccessListener {
                Log.d("GroupMembersActivity", "Reports for $reportedUserId cleared")
            }
            .addOnFailureListener { e ->
                Log.e("GroupMembersActivity", "Failed to clear reports for $reportedUserId", e)
            }
    }
}