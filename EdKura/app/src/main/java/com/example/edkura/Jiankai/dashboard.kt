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
import android.widget.PopupMenu
import com.example.edkura.Narciso.CourseDetailActivity
import com.example.edkura.auth.LoginActivity
import com.example.edkura.Jiankai.ProfileActivity
import com.google.firebase.auth.FirebaseAuth

class DashboardActivity : AppCompatActivity() {
    private lateinit var student: Student // 声明 Student 对象
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
        student.addedClasses = intent.getStringArrayListExtra("updatedClasses") ?: arrayListOf()

        courseAdapter = CourseAdapter(listOf(),
            { position -> showDeleteDialog(position) },
            { course -> goToCourseDetail(course) }

        )
        recyclerView.adapter = courseAdapter
        updateCourseList()

        val buttonSetting: ImageButton = findViewById(R.id.buttonSetting)
        buttonSetting.setOnClickListener {
            startActivity(Intent(this, classManagement::class.java))
        }

        // Profile Button Integration
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
        student.removeCourseAt(position)
        updateCourseList()
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