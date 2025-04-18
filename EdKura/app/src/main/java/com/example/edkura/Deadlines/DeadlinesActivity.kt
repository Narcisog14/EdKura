package com.example.edkura.Deadlines

import android.os.Bundle
import android.util.Log
import android.widget.CalendarView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.GroupProject.GroupProjectDashboardActivity
import com.example.edkura.R
import com.example.edkura.models.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
        Log.d("DeadlinesActivity", "onCreate: groupId received = [$groupId]")

        database = FirebaseDatabase
            .getInstance("https://edkura-81d7c-default-rtdb.firebaseio.com")
            .reference


        // Set up RecyclerView
        tasksAdapter = TaskAdapter(mutableListOf())
        tasksRecyclerView.layoutManager = LinearLayoutManager(this)
        tasksRecyclerView.adapter = tasksAdapter

        // Initialize the selected date
        selectedDate = Calendar.getInstance().time

        // Set up the listener for the fab
        addTaskFab.setOnClickListener {
            val addTaskDialog = AddTaskDialogFragment.newInstance(groupId)
            addTaskDialog.show(supportFragmentManager, "AddTaskDialog")
        }


        // Set the listener for the calender
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            selectedDate = calendar.time
            loadTasksForSelectedDate()
        }

        // Load tasks initially
        loadTasksForSelectedDate()
    }
    private fun showAddTaskDialog() {
        Log.d("DeadlinesActivity", "Launching dialog with groupId = $groupId")
        val addTaskDialog = AddTaskDialogFragment.newInstance(groupId)
        addTaskDialog.show(supportFragmentManager, "AddTaskDialog")
    }
    private fun loadTasksForSelectedDate() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(selectedDate!!)

        database.child("projectGroups").child(groupId).child("tasks")
            .addValueEventListener(object : ValueEventListener {
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
}