package com.example.edkura
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.edkura.Jiankai.classManagement
import com.example.edkura.Jiankai.Student

class DashboardActivity : AppCompatActivity() {
    private lateinit var student: Student // 声明 Student 对象
    private lateinit var coursesAdapter: ArrayAdapter<String>

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

        // 获取从 MainActivity 传来的课程数据，并初始化 Student 对象
        if (!::student.isInitialized) {
            student = Student(this) // 创建 Student 对象
        }

        val addedClasses = intent.getStringArrayListExtra("addedClasses") ?: arrayListOf()

        // 如果 student.addedClasses 为空才将传来的课程数据添加进去，避免重复添加
        if (student.addedClasses.isEmpty()) {
            student.addedClasses.addAll(addedClasses) // 将传来的课程数据加入到 Student 中
        }

        // 设置 ListView 显示课程数据
        val listViewCourses: ListView = findViewById(R.id.listViewCourses)
        coursesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, student.getNumberedCourseList())
        listViewCourses.adapter = coursesAdapter

        // 设置长按监听器，当长按项时显示删除按钮
        listViewCourses.setOnItemLongClickListener { _, _, position, _ ->
            val actualCourse = student.addedClasses[position] // Get the non-numbered version
            val displayedCourse = student.getNumberedCourseList()[position] // Get the numbered version for display

            AlertDialog.Builder(this)
                .setTitle("Delete Course")
                .setMessage("Are you sure you want to delete the course: $displayedCourse?")
                .setPositiveButton("Delete") { _, _ ->
                    student.removeCourseAt(position) // Delete by position to avoid string matching issues

                    // Refresh the adapter with updated numbered list
                    coursesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, student.getNumberedCourseList())
                    listViewCourses.adapter = coursesAdapter

                    val resultIntent = Intent()
                    resultIntent.putStringArrayListExtra("updatedClasses", student.addedClasses)
                    setResult(RESULT_OK, resultIntent)
                }
                .setNegativeButton("Cancel", null)
                .show()

            true
        }
        buttonSetting.setOnClickListener {
            val intent = Intent(this, classManagement::class.java)
            startActivity(intent)
        }
    }

    override fun onBackPressed() {
        student.saveCourses() // 退出前保存数据
        super.onBackPressed() // 让系统处理返回操作
    }

    private fun updateCourseList() {
        val numberedList = student.addedClasses.mapIndexed { index, course -> "${index + 1}. $course" }
        coursesAdapter.clear()
        coursesAdapter.addAll(numberedList)
        coursesAdapter.notifyDataSetChanged()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1 && resultCode == RESULT_OK) {
            val updatedClasses = data?.getStringArrayListExtra("updatedClasses")
            if (updatedClasses != null) {
                student.addedClasses.clear() // 清空旧课程
                student.addedClasses.addAll(updatedClasses) // 添加新课程
                coursesAdapter.notifyDataSetChanged() // 刷新 ListView 显示
            }
        }
    }
    override fun onResume() {
        super.onResume()
        student.loadCourses() // 确保加载最新的课程数据
        updateCourseList()    // 刷新课程列表
    }
}
