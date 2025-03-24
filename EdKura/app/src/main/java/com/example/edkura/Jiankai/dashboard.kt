package com.example.edkura

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.Jiankai.classManagement
import com.example.edkura.Jiankai.Student
import CourseAdapter
import android.view.WindowManager
import android.widget.ImageButton
import com.example.edkura.Narciso.CourseDetailActivity
import com.example.edkura.Narciso.UserProfileActivity
import com.example.edkura.Narciso.User

class DashboardActivity : AppCompatActivity() {
    private lateinit var student: Student
    private lateinit var recyclerView: RecyclerView
    private lateinit var courseAdapter: CourseAdapter

    // Pre-defined user data (add major here)
    private val preDefinedUsers = listOf(
        User("user1", "Alice Johnson", "alice@example.com", "Computer Science"),
        User("user2", "Bob Williams", "bob@example.com", "Engineering"),
        User("user3", "Charlie Brown", "charlie@example.com", "Business")
        // Add more users here
    )

    // Select the default user
    private var currentUserId: String = preDefinedUsers[0].id //user1 as default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        setContentView(R.layout.jk_dashboard)

        val buttonSetting: ImageButton = findViewById(R.id.buttonSetting)
        val profileButton: ImageButton = findViewById(R.id.profileButton)
        recyclerView = findViewById(R.id.recyclerViewCourses)
        recyclerView.layoutManager = LinearLayoutManager(this)

        if (!::student.isInitialized) {
            student = Student(this)
        }

        val addedClasses = intent.getStringArrayListExtra("updatedClasses") ?: arrayListOf()
        if (student.addedClasses.isEmpty()) {
            student.addedClasses.addAll(addedClasses)
        }

        courseAdapter = CourseAdapter(
            listOf(),
            { position -> showDeleteDialog(position) },
            { course -> goToCourseDetail(course) }
        )
        recyclerView.adapter = courseAdapter

        updateCourseList()

        buttonSetting.setOnClickListener {
            val intent = Intent(this, classManagement::class.java)
            startActivity(intent)
        }
        profileButton.setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            intent.putExtra("USER_ID", currentUserId) // Pass user ID
            startActivity(intent)
        }

    }

    private fun updateCourseList() {
        val courseList = student.addedClasses.map { course ->
            val parts = course.split(" ", limit = 2)
            val subject = if (parts.size > 1) parts[0] else "Unknown"
            val courseName = if (parts.size > 1) parts[1] else course
            Pair(subject, courseName)
        }

        courseAdapter.updateData(courseList)
    }

    private fun showDeleteDialog(position: Int) {
        val courseToDelete = student.addedClasses[position]
        AlertDialog.Builder(this)
            .setTitle("Delete Course")
            .setMessage("Are you sure you want to delete: $courseToDelete?")
            .setPositiveButton("Delete") { _, _ ->
                student.removeCourseAt(position)
                updateCourseList()
                val resultIntent = Intent()
                resultIntent.putStringArrayListExtra("updatedClasses", student.addedClasses)
                setResult(RESULT_OK, resultIntent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun goToCourseDetail(course: Pair<String, String>) {
        val intent = Intent(this, CourseDetailActivity::class.java)
        intent.putExtra("subject", course.first)
        intent.putExtra("courseName", course.second)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        student.loadCourses()
        updateCourseList()
    }
}