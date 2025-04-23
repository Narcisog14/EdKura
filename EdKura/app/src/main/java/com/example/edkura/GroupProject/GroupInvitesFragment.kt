package com.example.edkura.GroupProject

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.R
import com.example.edkura.adapters.GroupInviteAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class GroupInvitesFragment : Fragment(), GroupInviteAdapter.OnInviteActionListener {

    private lateinit var database: DatabaseReference
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GroupInviteAdapter
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: ""
    private var groupInviteList: MutableList<GroupProjectDashboardActivity.GroupInvite> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_group_invites, container, false)
        recyclerView = view.findViewById(R.id.invitesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = GroupInviteAdapter(groupInviteList, this)
        recyclerView.adapter = adapter
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = FirebaseDatabase
            .getInstance("https://edkura-81d7c-default-rtdb.firebaseio.com")
            .reference
        loadGroupInvites()
    }

    private fun loadGroupInvites() {
        val invitesRef = database.child("groupInvites")

        invitesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val invites = mutableListOf<GroupProjectDashboardActivity.GroupInvite>()
                for (inviteSnapshot in snapshot.children) {
                    val inviteId = inviteSnapshot.key ?: continue
                    val groupId = inviteSnapshot.child("groupId").getValue(String::class.java) ?: ""
                    val invitedBy = inviteSnapshot.child("invitedBy").getValue(String::class.java) ?: ""
                    val userIdToInvite = inviteSnapshot.child("userIdToInvite").getValue(String::class.java) ?: ""
                    val status = inviteSnapshot.child("status").getValue(String::class.java) ?: ""
                    if(userIdToInvite == currentUserId){
                        // Load group name and invited by user details
                        loadGroupAndUserDetails(groupId, invitedBy, inviteId,status) { groupInvite ->
                            invites.add(groupInvite)
                        }
                    }
                }
                updateInvitesUI(invites)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GroupProjectDashboard", "Failed to load invites", error.toException())
                Toast.makeText(context, "Failed to load invites", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadGroupAndUserDetails(groupId: String, invitedBy: String, inviteId:String, status:String, callback: (GroupProjectDashboardActivity.GroupInvite) -> Unit) {
        val groupRef = database.child("projectGroups").child(groupId)
        val userRef = database.child("users").child(invitedBy)
        var groupName = ""
        var username = ""

        groupRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                groupName = snapshot.child("name").getValue(String::class.java) ?: ""
                userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(userSnapshot: DataSnapshot) {
                        username = userSnapshot.child("username").getValue(String::class.java) ?: ""

                        val groupInvite = GroupProjectDashboardActivity.GroupInvite(inviteId, groupId, invitedBy, groupName, username, status)
                        callback(groupInvite)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("GroupProjectDashboard", "Failed to load user details", error.toException())
                    }
                })

            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GroupProjectDashboard", "Failed to load group name", error.toException())
            }
        })
    }
    private fun updateInvitesUI(invites: List<GroupProjectDashboardActivity.GroupInvite>){
        groupInviteList.clear()
        groupInviteList.addAll(invites)
        adapter.notifyDataSetChanged()
    }

    private fun acceptInvite(inviteId: String, groupId: String) {
        val updates = mapOf(
            "/projectGroups/$groupId/members/$currentUserId" to true,
            "/groupInvites/$inviteId/status" to "accepted",
        )
        database.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(context, "Invite accepted", Toast.LENGTH_SHORT).show()
                loadGroupInvites()
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to accept invite", Toast.LENGTH_SHORT).show()
            }

    }

    private fun rejectInvite(inviteId: String) {
        val updates = mapOf(
            "/groupInvites/$inviteId/status" to "rejected"
        )

        database.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(context, "Invite rejected", Toast.LENGTH_SHORT).show()
                loadGroupInvites()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to reject invite", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onAcceptClicked(invite: GroupProjectDashboardActivity.GroupInvite) {
        acceptInvite(invite.inviteId, invite.groupId)
    }

    override fun onDeclineClicked(invite: GroupProjectDashboardActivity.GroupInvite) {
        rejectInvite(invite.inviteId)
    }
}