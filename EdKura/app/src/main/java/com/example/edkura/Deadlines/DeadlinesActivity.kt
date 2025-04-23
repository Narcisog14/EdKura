package com.example.edkura.Deadlines

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.widget.CalendarView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.R
import com.example.edkura.models.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class DeadlinesActivity : AppCompatActivity() {
    private lateinit var calendarView: CalendarView
    private lateinit var addTaskFab: FloatingActionButton
    private lateinit var tasksRecyclerView: RecyclerView
    private lateinit var tasksAdapter: TaskAdapter
    private lateinit var database: DatabaseReference
    private lateinit var groupId: String
    private var selectedDate: Date? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        calendarView = findViewById(R.id.calendarView)
        addTaskFab = findViewById(R.id.addTaskFab)
        tasksRecyclerView = findViewById(R.id.tasksRecyclerView)

        groupId = intent.getStringExtra("groupId") ?: ""
        database = FirebaseDatabase
            .getInstance("https://edkura-81d7c-default-rtdb.firebaseio.com")
            .reference

        // Set up RecyclerView
        tasksAdapter = TaskAdapter(mutableListOf())
        tasksRecyclerView.layoutManager = LinearLayoutManager(this)
        tasksRecyclerView.adapter = tasksAdapter

        // Handle long-press to delete
        tasksAdapter.onLongClick = { task ->
            showDeleteConfirmationDialog(task)
        }

        selectedDate = Calendar.getInstance().time

        addTaskFab.setOnClickListener {
            showAddTaskDialog()
        }

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            selectedDate = calendar.time
            loadTasksForSelectedDate()
        }

        loadTasksForSelectedDate()
    }

    private fun showAddTaskDialog() {
        val addTaskDialog = AddTaskDialogFragment.newInstance(groupId)
        addTaskDialog.show(supportFragmentManager, "AddTaskDialog")
    }

    private fun loadTasksForSelectedDate() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(selectedDate!!)

        database.child("projectGroups").child(groupId).child("tasks")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val tasks = mutableListOf<Task>()
                    for (taskSnapshot in snapshot.children) {
                        val task = taskSnapshot.getValue(Task::class.java)
                        if (task != null) {
                            val taskFormattedDate = dateFormat.format(task.deadline)
                            if (taskFormattedDate == formattedDate) {
                                tasks.add(task)
                            }
                        }
                    }
                    updateUI(tasks)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("DeadlinesActivity", "Failed to load tasks", error.toException())
                }
            })
    }

    private fun updateUI(tasks: List<Task>) {
        tasksAdapter.updateTasks(tasks)
    }

    private fun showDeleteConfirmationDialog(task: Task) {
        AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete the task \"${task.taskName}\"?")
            .setPositiveButton("Delete") { _, _ ->
                deleteTask(task)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTask(task: Task) {
        database.child("projectGroups")
            .child(groupId)
            .child("tasks")
            .child(task.taskId)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show()
                loadTasksForSelectedDate() // Refresh after delete
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete task", Toast.LENGTH_SHORT).show()
            }
    }
}
