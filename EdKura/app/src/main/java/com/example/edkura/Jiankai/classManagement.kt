package com.example.edkura.Jiankai
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.edkura.DashboardActivity
import com.example.edkura.R

class classManagement : AppCompatActivity() {
    private lateinit var student: Student
    private lateinit var addedClassesAdapter: ArrayAdapter<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.classmanagement)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.classManagement)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        val editTextSubject: EditText = findViewById(R.id.editTextSubject)
        val listViewSubject: ListView = findViewById(R.id.listViewSubject)
        val editTextClass: EditText = findViewById(R.id.editTextClass)
        val listViewClass: ListView = findViewById(R.id.listViewClass)
        val buttonNext: Button = findViewById(R.id.buttonNext)
        val buttonAdd: Button = findViewById(R.id.buttonAdd)
        val listViewAddedClasses: ListView = findViewById(R.id.listViewAddedClasses)

        val subjects = listOf("Computer Science", "Mathematics", "Physics", "Biology", "Chemistry")
        val courses = mapOf(
            "Computer Science" to listOf("Comp100", "Comp112", "Comp210", "Comp250", "Comp300"),
            "Mathematics" to listOf("Math121", "Math210", "Math250", "Math310", "Math400"),
            "Physics" to listOf("Phys101", "Phys202", "Phys303", "Phys404", "Phys505"),
            "Biology" to listOf("Bio101", "Bio202", "Bio303", "Bio404", "Bio505"),
            "Chemistry" to listOf("Chem101", "Chem202", "Chem303", "Chem404", "Chem505")
        )

        val subjectAdapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, subjects.toMutableList())
        listViewSubject.adapter = subjectAdapter

        student = Student(this)
        addedClassesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, student.getNumberedCourseList())
        listViewAddedClasses.adapter = addedClassesAdapter
        // 监听 EditTextSubject 输入，动态筛选专业
        editTextSubject.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    listViewSubject.visibility = View.GONE  // 输入为空时隐藏 ListView
                    listViewClass.visibility = View.GONE  // 隐藏课程列表
                } else {
                    listViewSubject.visibility = View.VISIBLE  // 输入时显示 ListView
                    // 筛选专业列表，只显示匹配的专业
                    val filteredSubjects = subjects.filter { it.contains(s, ignoreCase = true) }
                    val SubjectAdapter = ArrayAdapter(
                        this@classManagement,
                        android.R.layout.simple_list_item_1,
                        filteredSubjects
                    )
                    listViewSubject.adapter = subjectAdapter
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 监听 ListViewSubject 点击，填充 EditText 并显示对应的课程
        listViewSubject.setOnItemClickListener { parent, view, position, id ->
            val selectedSubject = parent.getItemAtPosition(position).toString()  // 通过 Item 获取点击的专业名称
            editTextSubject.setText(selectedSubject)  // 设置选中的专业到 EditText
            editTextSubject.setSelection(editTextSubject.text.length)  // 设置光标到文本末尾
            listViewSubject.visibility = View.GONE  // 选完后隐藏 ListView

            // 更新 ListViewClass，显示对应专业的课程
            val filteredCourses = courses[selectedSubject] ?: emptyList()
            val courseAdapter =
                ArrayAdapter(this, android.R.layout.simple_list_item_1, filteredCourses)
            listViewClass.adapter = courseAdapter
            listViewClass.visibility = View.VISIBLE  // 显示课程列表
            editTextClass.requestFocus()  // 将焦点设置到课程输入框
            editTextClass.setSelection(editTextClass.text.length)  // 设置光标到课程输入框末尾
        }

        // 监听 EditTextClass 输入，动态筛选课程
        editTextClass.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    listViewClass.visibility = View.GONE  // 输入为空时隐藏 ListView
                } else {
                    listViewClass.visibility = View.VISIBLE  // 输入时显示 ListView
                    // 筛选课程列表，只显示匹配的课程
                    val filteredCourses =
                        courses.values.flatten().filter { it.contains(s, ignoreCase = true) }
                    val courseAdapter = ArrayAdapter(
                        this@classManagement,
                        android.R.layout.simple_list_item_1,
                        filteredCourses
                    )
                    listViewClass.adapter = courseAdapter
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 监听 ListViewClass 点击，填充 EditText
        listViewClass.setOnItemClickListener { parent, view, position, id ->
            val selectedCourse = listViewClass.getItemAtPosition(position).toString()
            editTextClass.setText(selectedCourse)  // 设置选中的课程到 EditText
            editTextClass.setSelection(editTextClass.text.length)  // 设置光标到文本末尾
            listViewClass.visibility = View.GONE  // 隐藏课程列表
        }
        buttonAdd.setOnClickListener {
            val subject = editTextSubject.text.toString()
            val course = editTextClass.text.toString()

            if (subject.isNotEmpty() && course.isNotEmpty()) {
                student.addCourse(subject, course)
                // Update the display with numbered format
                addedClassesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, student.getNumberedCourseList())
                listViewAddedClasses.adapter = addedClassesAdapter
            }
        }

        buttonNext.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.putStringArrayListExtra("updatedClasses", student.addedClasses) // 传递课程数据
            setResult(RESULT_OK, intent)
            finish()
        }

    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1 && resultCode == RESULT_OK) { // 判断请求码和返回结果是否正确
            val updatedClasses = data?.getStringArrayListExtra("updatedClasses") // 获取返回的课程列表
            if (updatedClasses != null) {
                student.addedClasses.clear() // 清空旧课程
                student.addedClasses.addAll(updatedClasses) // 添加新课程
                addedClassesAdapter.notifyDataSetChanged() // 刷新 ListView 显示
            }
        }
    }
}
