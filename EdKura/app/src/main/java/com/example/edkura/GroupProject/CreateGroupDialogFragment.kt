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
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: ""
    private var currentUserCourse: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_create_group, container, false)
        groupNameEditText = view.findViewById(R.id.groupNameEditText)
        groupDescriptionEditText = view.findViewById(R.id.groupDescriptionEditText)
        createGroupButton = view.findViewById(R.id.createGroupButton)
        database = FirebaseDatabase
            .getInstance("https://edkura-81d7c-default-rtdb.firebaseio.com")
            .reference

        // grab the courseId passed in
        currentUserCourse = arguments?.getString("courseId") ?: ""
        Log.d("CreateGroup", "Course = $currentUserCourse")

        createGroupButton.setOnClickListener { createNewGroup() }
        return view
    }

    private fun createNewGroup() {
        val name = groupNameEditText.text.toString().trim()
        val desc = groupDescriptionEditText.text.toString().trim()
        if (name.isEmpty() || desc.isEmpty()) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }
        val newId = UUID.randomUUID().toString()
        val group = ProjectGroup(
            groupId = newId,
            name = name,
            description = desc,
            creator = currentUserId,
            course = currentUserCourse
        ).apply {
            members = mutableMapOf(currentUserId to true)
        }

        database.child("projectGroups").child(newId)
            .setValue(group)
            .addOnSuccessListener {
                Toast.makeText(context, "Group created", Toast.LENGTH_SHORT).show()
                dismiss()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error creating group", Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        fun newInstance(courseId: String): CreateGroupDialogFragment {
            return CreateGroupDialogFragment().apply {
                arguments = Bundle().apply { putString("courseId", courseId) }
            }
        }
    }
}
