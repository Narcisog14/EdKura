package com.example.edkura.GroupProject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.R
import com.example.edkura.adapters.UserSearchAdapter
import com.example.edkura.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.UUID

class InviteMoreDialogFragment : DialogFragment() {

    private lateinit var database: DatabaseReference
    private val users = mutableListOf<User>()
    private lateinit var userSearchAdapter: UserSearchAdapter
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private lateinit var membersRecyclerView: RecyclerView
    private lateinit var inviteUsersButton: Button
    private lateinit var searchView: SearchView
    private val selectedUsers = mutableListOf<User>()
    private val currentUserCourses = mutableListOf<String>()
    private var groupId: String? = null

    companion object {
        private const val ARG_GROUP_ID = "groupId"
        fun newInstance(groupId: String): InviteMoreDialogFragment {
            return InviteMoreDialogFragment().apply {
                arguments = Bundle().apply { putString(ARG_GROUP_ID, groupId) }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupId = arguments?.getString(ARG_GROUP_ID)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_invite_more, container, false)
        database = FirebaseDatabase.getInstance().reference

        membersRecyclerView = view.findViewById(R.id.membersRecyclerView)
        inviteUsersButton   = view.findViewById(R.id.inviteUsersButton)
        searchView          = view.findViewById(R.id.userSearchView)

        membersRecyclerView.layoutManager = LinearLayoutManager(context)
        userSearchAdapter = UserSearchAdapter(users, selectedUsers)
        membersRecyclerView.adapter = userSearchAdapter

        // 1) load my courses properly (the *values* from the list)
        database.child("users")
            .child(currentUserId)
            .child("courses")
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    currentUserCourses.clear()
                    snapshot.children.forEach { c ->
                        c.getValue(String::class.java)
                            ?.takeIf { it.isNotBlank() }
                            ?.let { currentUserCourses.add(it) }
                    }
                    // once we know my courses, load only matching users:
                    loadPotentialMembers()
                }
                override fun onCancelled(e: DatabaseError) {
                    Toast.makeText(context, "Failed to load your courses", Toast.LENGTH_SHORT).show()
                }
            })

        setupSearchView()
        inviteUsersButton.setOnClickListener { inviteUsersToGroup() }
        return view
    }

    private fun loadPotentialMembers() {
        users.clear()
        database.child("users").addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { us ->
                    val uid = us.key ?: return@forEach
                    if (uid == currentUserId) return@forEach

                    // read their courses list values
                    val theirCourses = us.child("courses").children.mapNotNull {
                        it.getValue(String::class.java)
                    }.toSet()

                    // only show if they share at least one course
                    if (theirCourses.intersect(currentUserCourses).isNotEmpty()) {
                        val name = us.child("name").getValue(String::class.java) ?: "Unknown"
                        users.add(User(name = name, userId = uid))
                    }
                }
                userSearchAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(e: DatabaseError) {
                Toast.makeText(context, "Failed to load users", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(text: String?): Boolean {
                userSearchAdapter.updateData(users.filter {
                    it.name.contains(text ?: "", true)
                })
                return true
            }
        })
    }

    private fun inviteUsersToGroup() {
        val inviterName = FirebaseAuth.getInstance().currentUser
            ?.displayName
            ?: "Unknown"

        selectedUsers.forEach { user ->
            val inviteId = UUID.randomUUID().toString()
            val data = mapOf(
                "inviteId"       to inviteId,
                "groupId"        to groupId,
                "invitedBy"      to currentUserId,
                "invitedByName"  to inviterName,
                "inviteeId"      to user.userId,
                "inviteeName"    to user.name,
                "status"         to "pending"
            )
            database
                .child("groupInvites")
                .child(inviteId)
                .setValue(data)
        }

        Toast
            .makeText(context, "Invites sent", Toast.LENGTH_SHORT)
            .show()
        dismiss()
    }
}