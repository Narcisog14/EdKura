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
import android.content.ContentValues.TAG
import android.util.Log
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Toast
import com.example.edkura.Narciso.CourseDetailActivity
import com.example.edkura.auth.LoginActivity
import com.example.edkura.Jiankai.ProfileActivity
import com.example.edkura.chat.AllChatsActivity
import com.example.edkura.chat.ChatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging

class DashboardActivity : AppCompatActivity() {
    private lateinit var student: Student  // Student object that holds courses locally
    private lateinit var recyclerView: RecyclerView
    private lateinit var courseAdapter: CourseAdapter
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        setContentView(R.layout.jk_dashboard)

        auth = FirebaseAuth.getInstance()

        recyclerView = findViewById(R.id.recyclerViewCourses)
        recyclerView.layoutManager = LinearLayoutManager(this)

        student = Student(this)
        // Get the saved courses passed via intent if available:
        student.addedClasses = intent.getStringArrayListExtra("updatedClasses") ?: arrayListOf()

        // Initialize adapter with empty data; set up listeners in callbacks
        courseAdapter = CourseAdapter(listOf(),
            onLongClick = { position -> showDeleteDialog(position) },
            onItemClick = { course -> goToCourseDetail(course) }
        )
        recyclerView.adapter = courseAdapter

        updateCourseList()

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log and toast
            val msg = getString(R.string.msg_token_fmt, token)
            Log.d(TAG, msg)
            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
        })

        // Chat button
        val buttonChat: ImageButton = findViewById(R.id.buttonChat)
        buttonChat.setOnClickListener {
            startActivity(Intent(this, AllChatsActivity::class.java))
        }

        // Navigate to class management
        val buttonSetting: ImageButton = findViewById(R.id.buttonSetting)
        buttonSetting.setOnClickListener {
            startActivity(Intent(this, classManagement::class.java))
        }

        // Profile button with popup menu for profile and logout
        val buttonProfile: ImageButton = findViewById(R.id.buttonProfile)
        buttonProfile.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.profile_menu, popup.menu)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_profile -> {
                        startActivity(Intent(this, ProfileActivity::class.java))
                        true
                    }
                    R.id.menu_logout -> {
                        auth.signOut()
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    // Update the list in the adapter based on the student's addedClasses
    private fun updateCourseList() {
        val courseList = student.addedClasses.map { course ->
            val parts = course.split(" ", limit = 2)
            val subject = if (parts.size > 1) parts[0] else "Unknown"
            val courseName = if (parts.size > 1) parts[1] else course
            Pair(subject, courseName)
        }
        courseAdapter.updateData(courseList)
    }

    // Show a confirmation dialog to remove a course.
    private fun showDeleteDialog(position: Int) {
        val courseToDelete = student.addedClasses[position]
        AlertDialog.Builder(this)
            .setTitle("Delete Course")
            .setMessage("Are you sure you want to delete: $courseToDelete?")
            .setPositiveButton("Delete") { _, _ ->
                // Remove from local list and update UI
                student.removeCourseAt(position)
                updateCourseList()
                // Now update Firebase: remove the course from the user's courses node.
                val userId = auth.currentUser?.uid
                userId?.let {
                    FirebaseDatabase.getInstance().reference.child("users")
                        .child(it)
                        .child("courses")
                        .setValue(student.addedClasses)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Course removed successfully", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error removing course: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Open CourseDetailActivity with the selected course's data.
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