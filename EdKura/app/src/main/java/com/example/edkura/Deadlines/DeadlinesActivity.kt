package com.example.edkura.Deadlines

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.R
import com.example.edkura.models.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*
import java.util.*

class DeadlinesActivity : AppCompatActivity() {
    private lateinit var addTaskFab: FloatingActionButton
    private lateinit var tasksRecyclerView: RecyclerView
    private lateinit var tasksAdapter: TaskAdapter
    private lateinit var database: DatabaseReference
    private lateinit var groupId: String
    private lateinit var sortButton: Button
    private var currentSortOrder = SortOrder.NEAR_FIRST

    enum class SortOrder {
        NEAR_FIRST, FAR_FIRST
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        addTaskFab = findViewById(R.id.addTaskFab)
        tasksRecyclerView = findViewById(R.id.tasksRecyclerView)
        sortButton = findViewById(R.id.sortButton)
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
        addTaskFab.setOnClickListener {
            showAddTaskDialog()
        }
        sortButton.setOnClickListener {
            // Toggle the sort order
            currentSortOrder = when (currentSortOrder) {
                SortOrder.NEAR_FIRST -> SortOrder.FAR_FIRST
                SortOrder.FAR_FIRST -> SortOrder.NEAR_FIRST
            }
            // Reload and resort tasks
            loadTasks()
        }
        loadTasks()
    }

    private fun showAddTaskDialog() {
        val addTaskDialog = AddTaskDialogFragment.newInstance(groupId)
        addTaskDialog.show(supportFragmentManager, "AddTaskDialog")
    }

    private fun loadTasks() {
        Log.d("DeadlinesActivity", "Loading all tasks")

        database.child("projectGroups").child(groupId).child("tasks")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val tasks = mutableListOf<Task>()
                    for (taskSnapshot in snapshot.children) {
                        val task = taskSnapshot.getValue(Task::class.java)
                        if (task != null) {
                            tasks.add(task)
                        }
                    }
                    sortTasks(tasks)
                    Log.d("DeadlinesActivity", "Found ${tasks.size} tasks")
                    updateUI(tasks)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("DeadlinesActivity", "Failed to load tasks", error.toException())
                }
            })
    }

    private fun sortTasks(tasks: MutableList<Task>) {
        when (currentSortOrder) {
            SortOrder.NEAR_FIRST -> tasks.sortWith(compareBy { it.deadline })
            SortOrder.FAR_FIRST -> tasks.sortWith(compareByDescending { it.deadline })
        }
        // Update button text after sorting
        sortButton.text = when (currentSortOrder) {
            SortOrder.NEAR_FIRST -> getString(R.string.sort_by_far)
            SortOrder.FAR_FIRST -> getString(R.string.sort_by_near)
        }
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
        val taskId = task.taskId

        if (taskId != null) {
            database.child("projectGroups")
                .child(groupId)
                .child("tasks")
                .child(taskId)
                .removeValue()
                .addOnSuccessListener {
                    Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show()
                    loadTasks()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to delete task", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Handle the case where taskId is null.
            Toast.makeText(this, "Task ID is null. Cannot delete.", Toast.LENGTH_SHORT).show()
        }
    }
}