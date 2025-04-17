package com.example.edkura.GroupProject

import android.os.Bundle
import android.util.Log
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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
    private var currentUserCourses = mutableListOf<String>()
    private var groupId: String? = null

    companion object {
        private const val ARG_GROUP_ID = "groupId"

        fun newInstance(groupId: String? = null): InviteMoreDialogFragment {
            val fragment = InviteMoreDialogFragment()
            val args = Bundle().apply {
                putString(ARG_GROUP_ID, groupId)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            groupId = it.getString(ARG_GROUP_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_invite_more, container, false)

        database = FirebaseDatabase
            .getInstance("https://edkura-81d7c-default-rtdb.firebaseio.com")
            .reference

        membersRecyclerView = view.findViewById(R.id.membersRecyclerView)
        inviteUsersButton = view.findViewById(R.id.inviteUsersButton)
        searchView = view.findViewById(R.id.userSearchView)

        membersRecyclerView.layoutManager = LinearLayoutManager(context)
        userSearchAdapter = UserSearchAdapter(users, selectedUsers)
        membersRecyclerView.adapter = userSearchAdapter

        // Retrieve and store the current user's courses
        loadCurrentUserCourses()

        // Set up the search functionality
        setupSearchView()

        // Set up the invite button click listener
        setupInviteButton()

        return view
    }

    private fun loadCurrentUserCourses() {
        val userCoursesRef = database.child("users").child(currentUserId).child("courses")
        userCoursesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentUserCourses.clear()
                for (courseSnapshot in snapshot.children) {
                    val courseId = courseSnapshot.key
                    courseId?.let {
                        currentUserCourses.add(it)
                    }
                }
                // Now that we have the courses, we can load the potential members
                loadPotentialMembers()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load courses", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadPotentialMembers() {
        users.clear()
        val allUsersRef = database.child("users")
        allUsersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (userSnapshot in dataSnapshot.children) {
                    val userId = userSnapshot.key // Get the key (which is the user ID)
                    if (userId != null && userId != currentUserId) {
                        val email = if (userSnapshot.hasChild("email")) {
                            userSnapshot.child("email").getValue(String::class.java) ?: ""
                        } else {
                            "" // Or handle it differently, e.g., set to null, use a default value, or log an error.
                        }

                        val user = User(
                            name = userSnapshot.child("name").getValue(String::class.java) ?: "",
                        )
                        if (userSnapshot.hasChild("courses")) {
                            val userCourses = mutableListOf<String>()
                            val coursesSnapshot = userSnapshot.child("courses")
                            for (courseSnapshot in coursesSnapshot.children) {
                                val courseId = courseSnapshot.key
                                courseId?.let {
                                    userCourses.add(it)
                                }
                            }

                            if (userCourses.intersect(currentUserCourses).isNotEmpty()) {
                                users.add(user)
                            }
                        }
                    }
                }
                userSearchAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(context, "Failed to load users", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    filterUsers(it)
                }
                return true
            }
        })
    }

    private fun filterUsers(query: String) {
        val filteredUsers = users.filter { user ->
            user.name.lowercase().contains(query.lowercase())
        }
        userSearchAdapter.updateData(filteredUsers) // Corrected line: Call updateData instead of updateUsers
    }

    private fun setupInviteButton() {
        inviteUsersButton.setOnClickListener {
            if (selectedUsers.isNotEmpty()) {
                inviteUsersToGroup()
            } else {
                Toast.makeText(context, "Select at least one user to invite.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun inviteUsersToGroup() {
        selectedUsers.forEach { user ->
            database.child("users").orderByChild("name").equalTo(user.name)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            for (userSnapshot in snapshot.children) {
                                val userId = userSnapshot.key ?: ""
                                checkIfInviteExists(userId, groupId ?: "") { inviteExists ->
                                    if (!inviteExists) {
                                        // Now we are sure that we need to create the invite
                                        sendInvite(userId, groupId ?: "", user)
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Invite already sent to ${user.name}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(context, "Failed to find user.", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }
    private fun sendInvite(userId: String, groupId: String, user: User){
        val inviteId = UUID.randomUUID().toString()
        val inviteData = mapOf(
            "userId" to userId,
            "groupId" to groupId,
            "status" to "pending"
        )
        groupId?.let {
            database.child("groupInvites").child(inviteId)
                .setValue(inviteData)
                .addOnSuccessListener {
                    database.child("projectGroups").child(groupId)
                        .child("invitedUsers").child(userId).setValue(true)
                        .addOnSuccessListener {
                            Toast.makeText(
                                context,
                                "Invite sent to ${user.name}",
                                Toast.LENGTH_SHORT
                            ).show()
                            dialog?.dismiss()
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                context,
                                "failed to save the invited user to the project group",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(
                        context,
                        "Failed to send invite",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

    }
    private fun checkIfInviteExists(userId: String, groupId: String, callback: (Boolean) -> Unit) {
        database.child("groupInvites")
            .orderByChild("userId")
            .equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var exists = false
                    for (inviteSnapshot in snapshot.children) {
                        val inviteGroupId =
                            inviteSnapshot.child("groupId").getValue(String::class.java)
                        if (inviteGroupId == groupId) {
                            exists = true
                            break
                        }
                    }
                    callback(exists)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("checkIfInviteExists", "Error checking invite: ${error.message}")
                    callback(false) // In case of error, assume no invite exists (or you can change this to true, depending on how you want to handle errors)
                }
            })
    }
}

