package com.example.edkura.Deadlines

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.edkura.R
import com.example.edkura.models.Task
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class AddTaskDialogFragment : DialogFragment() {

    private lateinit var taskNameEditText: EditText
    private lateinit var deadlineEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var database: DatabaseReference
    private var selectedDeadline: Date? = null
    private lateinit var groupId: String

    companion object {
        private const val ARG_GROUP_ID = "groupId"

        fun newInstance(groupId: String): AddTaskDialogFragment {
            val fragment = AddTaskDialogFragment()
            val args = Bundle()
            args.putString(ARG_GROUP_ID, groupId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupId = arguments?.getString(ARG_GROUP_ID) ?: ""
        Log.d("AddTaskDialog", "onCreate: groupId = [$groupId]") // Should NOT be empty
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_add_task, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = FirebaseDatabase
            .getInstance("https://edkura-81d7c-default-rtdb.firebaseio.com")
            .reference

        taskNameEditText = view.findViewById(R.id.taskNameEditText)
        deadlineEditText = view.findViewById(R.id.deadlineEditText)
        saveButton = view.findViewById(R.id.saveButton)

        deadlineEditText.setOnClickListener {
            showDatePickerDialog()
        }

        saveButton.setOnClickListener {
            saveTask()
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
                selectedDeadline = selectedCalendar.time

                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                deadlineEditText.setText(dateFormat.format(selectedDeadline!!))
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun saveTask() {
        val taskName = taskNameEditText.text.toString()

        if (taskName.isBlank() || selectedDeadline == null) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val task = Task(
            taskId = UUID.randomUUID().toString(),
            taskName = taskName,
            deadline = selectedDeadline,
            groupId = groupId
        )

        database.child("projectGroups")
            .child(groupId)
            .child("tasks")
            .child(task.taskId)
            .setValue(task)
            .addOnSuccessListener {
                Log.d("AddTaskDialog", "Task saved successfully")
                Toast.makeText(context, "Task saved successfully", Toast.LENGTH_SHORT).show()
                dismiss()
            }
            .addOnFailureListener { e ->
                Log.e("AddTaskDialog", "Failed to save task", e)
                Toast.makeText(context, "Failed to save task", Toast.LENGTH_SHORT).show()
            }
    }
}

