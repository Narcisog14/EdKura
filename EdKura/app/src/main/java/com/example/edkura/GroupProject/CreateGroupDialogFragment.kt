package com.example.edkura.GroupProject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.R
import com.example.edkura.adapters.UserSearchAdapter
import com.example.edkura.models.ProjectGroup
import com.example.edkura.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID

class CreateGroupDialogFragment : DialogFragment() {

    private lateinit var database: DatabaseReference
    private val users = mutableListOf<User>()
    private lateinit var userSearchAdapter: UserSearchAdapter
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: ""
    private lateinit var groupNameEditText: EditText
    private lateinit var groupDescriptionEditText: EditText
    private lateinit var membersRecyclerView: RecyclerView
    private lateinit var createGroupButton: Button
    private lateinit var searchView: SearchView
    private val selectedUsers = mutableListOf<User>()
    private var currentUserCourses = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_create_group, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database =
            FirebaseDatabase.getInstance("https://edkura-81d7c-default-rtdb.firebaseio.com").reference
        //get the views
        groupNameEditText = view.findViewById(R.id.groupNameEditText)
        groupDescriptionEditText = view.findViewById(R.id.groupDescriptionEditText)
        membersRecyclerView = view.findViewById(R.id.membersRecyclerView)
        createGroupButton = view.findViewById(R.id.createGroupButton)
        searchView = view.findViewById(R.id.searchView)
        userSearchAdapter = UserSearchAdapter(users, selectedUsers)
        membersRecyclerView.adapter = userSearchAdapter
        membersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        // Get the current user courses from Firebase
        loadCurrentUserCourses()
        //set up the searchview
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) {
                    filterUsers(newText)
                }
                return true
            }
        })
        createGroupButton.setOnClickListener {
            createGroup()
        }
    }

    private fun filterUsers(query: String) {
        val filteredUsers = users.filter {
            it.name.contains(query, ignoreCase = true)
        }
        userSearchAdapter.updateData(filteredUsers)
    }

    private fun loadAllUsers() {
        database.child("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                users.clear()
                for (dataSnapshot in snapshot.children) {
                    val user = dataSnapshot.getValue(User::class.java)
                    if (user != null && user.userId != currentUserId) {
                        users.add(user)
                    }
                }
                userSearchAdapter.updateData(users)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Failed to load users",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun createGroup() {
        //get the inputs
        val groupName = groupNameEditText.text.toString()
        val groupDescription = groupDescriptionEditText.text.toString()
        //validation
        if (groupName.isEmpty() || groupDescription.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }
        //generate a group id
        val groupId = UUID.randomUUID().toString()
        //add current user to the members list
        val members = mutableMapOf<String, Boolean>()
        members[currentUserId] = true
        // Create ProjectGroup object
        val projectGroup = ProjectGroup(
            groupId = groupId,
            name = groupName,
            description = groupDescription,
            course = currentUserCourses[0],
            members = members,
            invitedUsers = selectedUsers.associate { it.userId to true }.toMutableMap()
        )
        //add the group to the users invited groups
        for (user in selectedUsers) {
            val groupReference =
                database.child("users").child(user.userId).child("invitedToGroups")
                    .child(groupId)
            groupReference.setValue(true)
        }
        //save the group
        database.child("projectGroups").child(groupId).setValue(projectGroup)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Group created", Toast.LENGTH_SHORT).show()
                dismiss()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to create group", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun loadCurrentUserCourses() {
        database.child("users").child(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentUser = snapshot.getValue(User::class.java)
                    if (currentUser != null) {
                        currentUserCourses = currentUser.courses
                        loadUsersByCourses(currentUserCourses)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        requireContext(),
                        "Failed to load current user",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
    private fun loadUsersByCourses(courses: List<String>) {
        database.child("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                users.clear()
                for (dataSnapshot in snapshot.children) {
                    val user = dataSnapshot.getValue(User::class.java)
                    if (user != null && user.userId != currentUserId && user.courses.any { courses.contains(it) }) {
                        users.add(user)
                    }
                }
                userSearchAdapter.updateData(users)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Failed to load users by course",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}