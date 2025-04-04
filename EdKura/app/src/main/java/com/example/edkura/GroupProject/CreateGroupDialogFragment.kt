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
    private var groupId: String? = null
    private var isInviteMoreMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            groupId = it.getString(ARG_GROUP_ID)
            isInviteMoreMode = groupId != null // If groupId is not null, then it's in invite more mode
        }
    }

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
            if (isInviteMoreMode) {
                inviteMoreUsers()
            } else {
                createGroup()
            }
        }
        if (isInviteMoreMode) {
            loadGroupInfo()
        }
    }
    private fun loadGroupInfo() {
        groupId?.let { groupId ->
            database.child("projectGroups").child(groupId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val group = snapshot.getValue(ProjectGroup::class.java)
                        groupNameEditText.setText(group?.name ?: "")
                        groupDescriptionEditText.setText(group?.description ?: "")
                        groupNameEditText.isEnabled = false
                        groupDescriptionEditText.isEnabled = false
                        createGroupButton.text = "Invite Users"
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(
                            requireContext(),
                            "Failed to load group info",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }
    }
    private fun filterUsers(query: String) {
        val filteredUsers = users.filter {
            it.name.contains(query, ignoreCase = true)
        }
        userSearchAdapter.updateData(filteredUsers)
    }
    private fun inviteMoreUsers() {
        groupId?.let { groupId ->
            //get the users to invite
            for (user in selectedUsers) {
                val groupReference =
                    database.child("users").child(user.userId).child("invitedToGroups")
                        .child(groupId)
                groupReference.setValue(true)
            }
            //update the projectGroup
            val invitedUsersMap = selectedUsers.associate { it.userId to true }.toMutableMap()
            database.child("projectGroups").child(groupId).child("invitedUsers").updateChildren(invitedUsersMap as Map<String, Any>)
            Toast.makeText(requireContext(), "Users invited", Toast.LENGTH_SHORT).show()
            dismiss()
        }
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
        val newGroupId = UUID.randomUUID().toString()
        //add current user to the members list
        val members = mutableMapOf<String, Boolean>()
        members[currentUserId] = true
        // Create ProjectGroup object
        val projectGroup = ProjectGroup(
            groupId = newGroupId,
            name = groupName,
            description = groupDescription,
            course = currentUserCourses[0],
            members = members,
            invitedUsers = selectedUsers.associate { it.userId to true }.toMutableMap(),
            creator = currentUserId
        )
        //add the group to the users invited groups
        for (user in selectedUsers) {
            val groupReference =
                database.child("users").child(user.userId).child("invitedToGroups")
                    .child(newGroupId)
            groupReference.setValue(true)
        }
        //save the group
        database.child("projectGroups").child(newGroupId).setValue(projectGroup)
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
                        loadEligibleUsersForGroup() // Call the new method here
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

    private fun loadEligibleUsersForGroup() {
        database.child("users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                users.clear() // Clear previous data clearly
                val currentUserCoursesSet = currentUserCourses.toSet()
                // Get the already invited or joined members
                val existingMembers = mutableListOf<String>()
                if (isInviteMoreMode && groupId != null) {
                    val groupSnapshot = snapshot.child("projectGroups").child(groupId!!)
                    groupSnapshot.child("members").children.forEach {
                        val memberId = it.key
                        if (memberId != null) {
                            existingMembers.add(memberId)
                        }
                    }
                    groupSnapshot.child("invitedUsers").children.forEach {
                        val invitedId = it.key
                        if (invitedId != null) {
                            existingMembers.add(invitedId)
                        }
                    }
                }

                snapshot.children.forEach { userSnap ->
                    val userId = userSnap.key ?: return@forEach
                    if (userId != currentUserId && !existingMembers.contains(userId)) {
                        val userCourses = userSnap.child("courses")
                            .children.mapNotNull { it.getValue(String::class.java) }.toSet()
                        val userName = userSnap.child("name").getValue(String::class.java) ?: "Unknown"

                        if (currentUserCoursesSet.intersect(userCourses).isNotEmpty()) {
                            val newUser = User(userId = userId, name = userName, courses = userCourses.toMutableList())
                            users.add(newUser)
                        }
                    }
                }

                userSearchAdapter.updateData(users)

                if (users.isEmpty()) {
                    Toast.makeText(requireContext(), "No eligible users found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error loading users: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    companion object {
        private const val ARG_GROUP_ID = "groupId"

        fun newInstance(groupId: String? = null): CreateGroupDialogFragment {
            val fragment = CreateGroupDialogFragment()
            val args = Bundle().apply {
                putString(ARG_GROUP_ID, groupId)
            }
            fragment.arguments = args
            return fragment
        }
    }
}