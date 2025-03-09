package com.example.edkura
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.Jiankai.classManagement
import com.example.edkura.Jiankai.Student
import CourseAdapter
import com.example.edkura.Narciso.CourseDetailActivity

class DashboardActivity : AppCompatActivity() {
    private lateinit var student: Student // 声明 Student 对象
    private lateinit var recyclerView: RecyclerView
    private lateinit var courseAdapter: CourseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.Dashboard)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val buttonSetting: Button = findViewById(R.id.buttonSetting)
        recyclerView = findViewById(R.id.recyclerViewCourses)
        recyclerView.layoutManager = LinearLayoutManager(this)

        if (!::student.isInitialized) {
            student = Student(this)
        }

        val addedClasses = intent.getStringArrayListExtra("updatedClasses") ?: arrayListOf()
        if (student.addedClasses.isEmpty()) {
            student.addedClasses.addAll(addedClasses)
        }

        // ✅ 初始化适配器，并添加点击事件
        courseAdapter = CourseAdapter(
            listOf(),
            { position -> showDeleteDialog(position) }, // 长按删除
            { course -> goToCourseDetail(course) } // 点击跳转
        )
        recyclerView.adapter = courseAdapter

        updateCourseList()

        buttonSetting.setOnClickListener {
            val intent = Intent(this, classManagement::class.java)
            startActivity(intent)
        }
    }

    private fun updateCourseList() {
        val courseList = student.addedClasses.map { course ->
            val parts = course.split(" ", limit = 2) // 分割 Subject 和 Course
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
                updateCourseList() // 刷新列表
                val resultIntent = Intent()
                resultIntent.putStringArrayListExtra("updatedClasses", student.addedClasses)
                setResult(RESULT_OK, resultIntent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun goToCourseDetail(course: Pair<String, String>) {
        val intent = Intent(this, CourseDetailActivity::class.java)
        intent.putExtra("subject", course.first) // 传递专业
        intent.putExtra("courseName", course.second) // 传递课程名
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        student.loadCourses()
        updateCourseList()
    }
}