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
import com.google.firebase.database.*

class GroupInvitesFragment : Fragment(), GroupInviteAdapter.OnInviteActionListener {

    private lateinit var database: DatabaseReference
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GroupInviteAdapter
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: ""
    private val invites = mutableListOf<GroupProjectDashboardActivity.GroupInvite>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_group_invites, container, false)
        recyclerView = view.findViewById(R.id.invitesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = GroupInviteAdapter(invites, this)
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
        database.child("groupInvites")
            .orderByChild("inviteeId")
            .equalTo(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    invites.clear()
                    if (!snapshot.exists()) {
                        adapter.notifyDataSetChanged()
                        return
                    }
                    snapshot.children.forEach { child ->
                        val status = child.child("status").getValue(String::class.java)
                        if (status != "pending") return@forEach

                        val inviteId      = child.key ?: return@forEach
                        val groupId       = child.child("groupId").getValue(String::class.java) ?: return@forEach
                        val inviterId     = child.child("invitedBy").getValue(String::class.java) ?: return@forEach
                        val inviterName   = child.child("invitedByName").getValue(String::class.java) ?: ""

                        // now fetch group name
                        database.child("projectGroups")
                            .child(groupId)
                            .child("name")
                            .get()
                            .addOnSuccessListener { ds ->
                                val groupName = ds.getValue(String::class.java) ?: ""
                                invites.add(
                                    GroupProjectDashboardActivity.GroupInvite(
                                        inviteId,
                                        groupId,
                                        inviterId,
                                        groupName,
                                        inviterName,
                                        "pending"
                                    )
                                )
                                adapter.notifyDataSetChanged()
                            }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("GroupInvitesFragment", "Error loading invites", error.toException())
                    Toast.makeText(context, "Failed to load invites", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun acceptInvite(invite: GroupProjectDashboardActivity.GroupInvite) {
        val updates = mapOf(
            "/projectGroups/${invite.groupId}/members/$currentUserId" to true,
            "/groupInvites/${invite.inviteId}/status" to "accepted"
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

    private fun rejectInvite(invite: GroupProjectDashboardActivity.GroupInvite) {
        database.child("groupInvites").child(invite.inviteId)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(context, "Invite rejected", Toast.LENGTH_SHORT).show()
                loadGroupInvites()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to reject invite", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onAcceptClicked(invite: GroupProjectDashboardActivity.GroupInvite) {
        acceptInvite(invite)
    }

    override fun onDeclineClicked(invite: GroupProjectDashboardActivity.GroupInvite) {
        rejectInvite(invite)
    }
}
