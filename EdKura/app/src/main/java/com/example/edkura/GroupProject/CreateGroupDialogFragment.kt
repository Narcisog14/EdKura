package com.example.edkura.GroupProject

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.edkura.R
import com.example.edkura.models.ProjectGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.UUID

class CreateGroupDialogFragment : DialogFragment() {

    private lateinit var groupNameEditText: EditText
    private lateinit var groupDescriptionEditText: EditText
    private lateinit var createGroupButton: Button
    private lateinit var database: DatabaseReference
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: ""
    private var currentUserCourses: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_create_group, container, false)
        groupNameEditText = view.findViewById(R.id.groupNameEditText)
        groupDescriptionEditText = view.findViewById(R.id.groupDescriptionEditText)
        createGroupButton = view.findViewById(R.id.createGroupButton)
        database = FirebaseDatabase
            .getInstance("https://edkura-81d7c-default-rtdb.firebaseio.com")
            .reference
        loadCurrentUserCourses()
        createGroupButton.setOnClickListener {
            createNewGroup()
        }

        return view
    }

    private fun createNewGroup() {
        val groupName = groupNameEditText.text.toString().trim()
        val groupDescription = groupDescriptionEditText.text.toString().trim()

        if (groupName.isEmpty() || groupDescription.isEmpty()) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }
        //generate a group id
        val newGroupId = UUID.randomUUID().toString()
        val newGroup = ProjectGroup(
            groupId = newGroupId,
            name = groupName,
            description = groupDescription,
            creator = currentUserId,
            course = currentUserCourses
        )
        newGroup.members =
            mutableMapOf(currentUserId to true) // Add current user to members

        val groupsRef = database.child("projectGroups")
        groupsRef.child(newGroupId).setValue(newGroup)
            .addOnSuccessListener {
                Toast.makeText(context, "Group created successfully", Toast.LENGTH_SHORT).show()
                dismiss() // Close the dialog after creating the group
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to create group", Toast.LENGTH_SHORT).show()
            }
    }
    private fun loadCurrentUserCourses() {
        currentUserCourses = arguments?.getString("courseId") ?: ""
        Log.d("currentUserCourses",currentUserCourses )
    }
    companion object {
        fun newInstance(course: String): CreateGroupDialogFragment {
            val fragment = CreateGroupDialogFragment()
            val args = Bundle()
            args.putString("courseId", course)
            fragment.arguments = args
            return fragment
        }
    }
}