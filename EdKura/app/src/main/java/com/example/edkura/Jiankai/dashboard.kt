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
import com.example.edkura.Jiankai.Course
import com.example.edkura.Jiankai.jiankaiUI.CustomCanvasView
import com.example.edkura.Narciso.CourseDetailActivity

class DashboardActivity : AppCompatActivity() {
    private lateinit var student: Student
    private lateinit var recyclerView: RecyclerView
    private lateinit var courseAdapter: CourseAdapter
    private lateinit var customCanvasView: CustomCanvasView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        setContentView(R.layout.jk_dashboard)

        val buttonSetting: ImageButton = findViewById(R.id.buttonSetting)
        recyclerView = findViewById(R.id.recyclerViewCourses)
        customCanvasView = findViewById(R.id.customCanvasView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize student
        if (!::student.isInitialized) {
            student = Student(this)
        }

        // 处理从 classManagement 传递的数据
        //Passing data
        val addedClasses = intent.getSerializableExtra("updatedClasses") as? ArrayList<Course>
        if (addedClasses != null && student.addedClasses.isEmpty()) {
            student.addedClasses.addAll(addedClasses)
        }

        // 初始化适配器，添加点击和长按事件
        //long click
        courseAdapter = CourseAdapter(
            listOf(),
            { position -> showDeleteDialog(position) }, // 长按删除
            { course -> goToCourseDetail(course) } // 点击跳转
        )
        recyclerView.adapter = courseAdapter

        updateCourseList()

        // 跳转到课程管理页面
        //jump to class management
        buttonSetting.setOnClickListener {
            val intent = Intent(this, classManagement::class.java)
            startActivity(intent)
        }
    }

    // 更新课程列表
    // Update course list
    private fun updateCourseList() {
        val courseList = student.addedClasses.map { course ->
            Pair(course.subject, course.course) // 转换为 Pair 以适配 adapter
        }
        courseAdapter.updateData(courseList)
    }private fun showDeleteDialog(position: Int) {
        val courseToDelete = student.addedClasses[position]

        AlertDialog.Builder(this)
            .setTitle("Delete Course")
            .setMessage("Are you sure you want to delete: ${courseToDelete.subject} - ${courseToDelete.course}?")
            .setPositiveButton("Delete") { _, _ ->
                student.removeCourseAt(position) // 删除对应的课程
                updateCourseList() // 刷新列表
                val resultIntent = Intent()
                resultIntent.putExtra("updatedClasses", student.addedClasses)
                setResult(RESULT_OK, resultIntent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    //  进入课程详情
    //  Go to course detail
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
        customCanvasView.resetRectangle()
    }
}