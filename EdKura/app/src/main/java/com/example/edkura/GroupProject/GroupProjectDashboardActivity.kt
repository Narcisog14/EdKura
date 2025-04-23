package com.example.edkura.GroupProject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import com.example.edkura.R
import com.example.edkura.Deadlines.DeadlinesActivity
import com.example.edkura.GroupFileSharing.GroupNoteSharingDashboard
import com.example.edkura.models.ProjectGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class GroupProjectDashboardActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private val auth    = FirebaseAuth.getInstance()
    private val me      = auth.currentUser?.uid ?: ""

    private lateinit var titleTv: TextView
    private lateinit var createJoinBtn: FloatingActionButton
    private lateinit var promptMsg: TextView
    private lateinit var descriptionTv: TextView
    private lateinit var leaveBtn: FloatingActionButton
    private lateinit var buttonsLayout: LinearLayout
    private lateinit var deadlinesCard: CardView
    private lateinit var noteShareBtn: TextView
    private lateinit var membersBtn: TextView
    private lateinit var chatBtn: TextView

    private var myCourse: String = ""
    private var groupId: String? = null
    private var currentGroup: ProjectGroup? = null

    private val pendingInvites = mutableListOf<GroupInvite>()

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

        titleTv         = findViewById(R.id.dashboardTitle)
        createJoinBtn   = findViewById(R.id.addUserItem)
        promptMsg       = findViewById(R.id.promptMessage)
        descriptionTv   = findViewById(R.id.groupDescription)
        leaveBtn        = findViewById(R.id.leaveGroupButton)
        buttonsLayout   = findViewById(R.id.buttonsLinearLayout)
        deadlinesCard   = findViewById(R.id.cardViewDeadlines)
        noteShareBtn    = findViewById(R.id.buttonProjectFiles)
        membersBtn      = findViewById(R.id.buttonMembers)
        chatBtn         = findViewById(R.id.buttonGroupChat)

        myCourse = intent.getStringExtra("courseName") ?: ""
        database = FirebaseDatabase
            .getInstance("https://edkura-81d7c-default-rtdb.firebaseio.com")
            .reference

        // initial UI
        promptMsg.visibility      = View.GONE
        descriptionTv.visibility  = View.GONE
        leaveBtn.visibility       = View.GONE
        buttonsLayout.visibility  = View.GONE

        leaveBtn.setOnClickListener { confirmLeaveGroup() }
        createJoinBtn.setOnClickListener { showCreateOrJoinDialog() }
        deadlinesCard.setOnClickListener {
            startActivity(Intent(this, DeadlinesActivity::class.java))
        }

        loadGroups()
    }

    private fun showCreateOrJoinDialog() {
        val b = AlertDialog.Builder(this)
            .setTitle("Create or Join a Group")
            .setPositiveButton("Create Group") { d, _ ->
                CreateGroupDialogFragment.newInstance(myCourse)
                    .show(supportFragmentManager, "CreateDialog")
                d.dismiss()
            }
            .setNegativeButton("Join Group") { d, _ ->
                loadGroupInvites()
                d.dismiss()
            }
        if (groupId != null) {
            b.setNeutralButton("Invite to Group") { d, _ ->
                InviteMoreDialogFragment.newInstance(groupId!!)
                    .show(supportFragmentManager, "InviteMore")
                d.dismiss()
            }
        }
        b.show()
    }

    private fun loadGroups() {
        database.child("projectGroups")
            .orderByChild("members/$me")
            .equalTo(true)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    if (snap.exists()) {
                        snap.children.forEach { child ->
                            val pg = child.getValue(ProjectGroup::class.java) ?: return@forEach
                            if (pg.course.contains(myCourse)) {
                                groupId = child.key
                                loadGroupDetails()
                            }
                        }
                        updateButtons(true)
                    } else {
                        updateUI(null)
                        updateButtons(false)
                    }
                }
                override fun onCancelled(err: DatabaseError) {
                    Toast.makeText(this@GroupProjectDashboardActivity, "Error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadGroupDetails() {
        groupId?.let { gid ->
            database.child("projectGroups").child(gid)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        currentGroup = snapshot.getValue(ProjectGroup::class.java)
                        updateUI(currentGroup)
                    }
                    override fun onCancelled(err: DatabaseError) { }
                })
        }
    }

    private fun updateUI(group: ProjectGroup?) {
        if (group != null) {
            promptMsg.visibility      = View.GONE
            descriptionTv.visibility  = View.VISIBLE
            titleTv.text              = group.name
            descriptionTv.text        = group.description
            noteShareBtn.setOnClickListener {
                val mems = ArrayList(group.members.keys)
                startActivity(Intent(this, GroupNoteSharingDashboard::class.java).apply {
                    putStringArrayListExtra("memberUidList", mems)
                    putExtra("courseName", myCourse)
                    putExtra("USER_ID", me)
                    putExtra("GROUP_ID", groupId)
                })
            }
            membersBtn.setOnClickListener {
                groupId?.let { gid ->
                    startActivity(Intent(this, GroupMembersActivity::class.java)
                        .putExtra("GROUP_ID", gid))
                }
            }
            chatBtn.setOnClickListener {
                groupId?.let { gid ->
                    startActivity(Intent(this, GroupChatActivity::class.java)
                        .putExtra("GROUP_ID", gid))
                }
            }
        } else {
            promptMsg.visibility      = View.VISIBLE
            descriptionTv.visibility  = View.GONE
            titleTv.text              = getString(R.string.group_dashboard_title)
        }
    }

    private fun updateButtons(isMember: Boolean) {
        leaveBtn.isVisible       = isMember
        buttonsLayout.isVisible  = isMember
    }

    private fun confirmLeaveGroup() {
        AlertDialog.Builder(this)
            .setTitle("Leave Group")
            .setMessage("Are you sure?")
            .setPositiveButton("Leave") { _, _ -> leaveGroup() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun leaveGroup() {
        groupId?.let { gid ->
            database.child("projectGroups").child(gid)
                .child("members").child(me)
                .removeValue()
                .addOnSuccessListener {
                    Toast.makeText(this, "Left group", Toast.LENGTH_SHORT).show()
                    loadGroups()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error leaving", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // --- JOIN INVITES ---

    private fun loadGroupInvites() {
        database.child("groupInvites")
            .orderByChild("inviteeId")
            .equalTo(me)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snap: DataSnapshot) {
                    pendingInvites.clear()
                    if (!snap.exists()) {
                        showInvitesDialog()
                        return
                    }
                    snap.children.forEach { inv ->
                        val status = inv.child("status").getValue(String::class.java) ?: return@forEach
                        if (status != "pending") return@forEach
                        val iid = inv.key ?: return@forEach
                        val gid = inv.child("groupId").getValue(String::class.java) ?: return@forEach
                        val by  = inv.child("invitedBy").getValue(String::class.java) ?: return@forEach
                        val byName = inv.child("invitedByName").getValue(String::class.java) ?: ""

                        // fetch group name
                        database.child("projectGroups").child(gid).child("name")
                            .get().addOnSuccessListener { ds ->
                                val gName = ds.getValue(String::class.java) ?: ""
                                pendingInvites.add(GroupInvite(iid, gid, by, gName, byName, "pending"))
                                if (pendingInvites.size == snap.childrenCount.toInt()) {
                                    showInvitesDialog()
                                }
                            }
                    }
                }
                override fun onCancelled(err: DatabaseError) { }
            })
    }

    private fun showInvitesDialog() {
        val b = MaterialAlertDialogBuilder(this)
            .setTitle("Group Invites")

        if (pendingInvites.isEmpty()) {
            b.setMessage("No invites").setPositiveButton("OK", null).show()
            return
        }

        val items = pendingInvites.map { "${it.groupName} by ${it.invitedByUserName}" }.toTypedArray()
        b.setItems(items) { _, idx ->
            val iv = pendingInvites[idx]
            acceptGroupInvite(iv)
        }
        b.setNegativeButton("Reject All") { _, _ ->
            pendingInvites.forEach { rejectGroupInvite(it) }
        }
        b.show()
    }

    private fun acceptGroupInvite(invite: GroupInvite) {
        val updates = mapOf(
            "/projectGroups/${invite.groupId}/members/$me" to true,
            "/groupInvites/${invite.inviteId}/status" to "accepted"
        )
        database.updateChildren(updates)
            .addOnSuccessListener { Toast.makeText(this, "Joined", Toast.LENGTH_SHORT).show(); loadGroups() }
            .addOnFailureListener { Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show() }
    }

    private fun rejectGroupInvite(invite: GroupInvite) {
        database.child("groupInvites").child(invite.inviteId)
            .removeValue()
    }
}
