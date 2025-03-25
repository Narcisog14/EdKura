package com.example.edkura.Jiankai
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.edkura.DashboardActivity
import com.example.edkura.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class classManagement : AppCompatActivity() {
    private lateinit var student: Student
    private lateinit var addedClassesAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        setContentView(R.layout.jk_classmanagement)

        val editTextsubject: EditText = findViewById(R.id.editTextsubject)
        val listViewsubject: ListView = findViewById(R.id.listViewsubject)
        val editTextClass: EditText = findViewById(R.id.editTextClass)
        val listViewClass: ListView = findViewById(R.id.listViewClass)
        val buttonNext: Button = findViewById(R.id.buttonNext)
        val buttonAdd: Button = findViewById(R.id.buttonAdd)
        val listViewAddedClasses: ListView = findViewById(R.id.listViewAddedClasses)

        val subjectList = listOf("Computer Science", "Mathematics", "Physics", "Biology", "Chemistry")
        val coursesMap = mapOf(
            "Computer Science" to listOf("Comp100", "Comp112", "Comp210", "Comp250", "Comp300"),
            "Mathematics" to listOf("Math121", "Math210", "Math250", "Math310", "Math400"),
            "Physics" to listOf("Phys101", "Phys202", "Phys303", "Phys404", "Phys505"),
            "Biology" to listOf("Bio101", "Bio202", "Bio303", "Bio404", "Bio505"),
            "Chemistry" to listOf("Chem101", "Chem202", "Chem303", "Chem404", "Chem505")
        )

        val subjectAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, subjectList)
        listViewsubject.adapter = subjectAdapter

        student = Student(this)
        addedClassesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, student.getNumberedCourseList())
        listViewAddedClasses.adapter = addedClassesAdapter

        // search major
        editTextsubject.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val filteredSubjects = if (s.isNullOrEmpty()) {
                    subjectList
                } else {
                    subjectList.filter { it.contains(s, ignoreCase = true) }
                }
                listViewsubject.adapter = ArrayAdapter(this@classManagement, android.R.layout.simple_list_item_1, filteredSubjects)
                listViewsubject.visibility = if (filteredSubjects.isEmpty()) View.GONE else View.VISIBLE
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // select class
        listViewsubject.setOnItemClickListener { parent, _, position, _ ->
            val selectedSubject = parent.getItemAtPosition(position).toString()
            editTextsubject.setText(selectedSubject)
            listViewsubject.visibility = View.GONE

            val availableCourses = coursesMap[selectedSubject] ?: emptyList()
            listViewClass.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, availableCourses)
            listViewClass.visibility = if (availableCourses.isEmpty()) View.GONE else View.VISIBLE
        }

        // search class
        editTextClass.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val filteredCourses = if (s.isNullOrEmpty()) {
                    emptyList()
                } else {
                    coursesMap.values.flatten().filter { it.contains(s, ignoreCase = true) }
                }
                listViewClass.adapter = ArrayAdapter(this@classManagement, android.R.layout.simple_list_item_1, filteredCourses)
                listViewClass.visibility = if (filteredCourses.isEmpty()) View.GONE else View.VISIBLE
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // select class
        listViewClass.setOnItemClickListener { parent, _, position, _ ->
            val selectedCourse = parent.getItemAtPosition(position).toString()
            editTextClass.setText(selectedCourse)
            listViewClass.visibility = View.GONE
        }

        // add class
        buttonAdd.setOnClickListener {
            val subject = editTextsubject.text.toString()
            val course = editTextClass.text.toString()

            if (subject.isNotEmpty() && course.isNotEmpty()) {
                val courseToAdd = Course(subject, course)

                if (!student.addedClasses.contains(courseToAdd)) {
                    student.addCourse(subject, course)

                    // 更新 ArrayAdapter 数据源，并刷新 ListView
                    addedClassesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, student.getNumberedCourseList())
                    listViewAddedClasses.adapter = addedClassesAdapter // 重新设置适配器
                    addedClassesAdapter.notifyDataSetChanged() // 刷新 ListView 显示
                } else {
                    Toast.makeText(this, "This course has already been added.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter a valid subject and course.", Toast.LENGTH_SHORT).show()
            }
        }

        // delete class
        listViewAddedClasses.setOnItemLongClickListener { _, _, position, _ ->
            val courseToDelete = student.addedClasses[position]

            AlertDialog.Builder(this)
                .setTitle("Delete Course")
                .setMessage("Are you sure you want to delete: ${courseToDelete.subject} ${courseToDelete.course}?")
                .setPositiveButton("Delete") { _, _ ->
                    student.removeCourseAt(position)
                    addedClassesAdapter.notifyDataSetChanged()
                }
                .setNegativeButton("Cancel", null)
                .show()

            true
        }

        // go to Dashboard
        buttonNext.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            val userCoursesRef = FirebaseDatabase.getInstance().reference.child("users").child(userId).child("courses")


            val courseStrings = student.addedClasses.map { "${it.subject} ${it.course}" }

            // 将课程数据上传到 Firebase
            // Upload course data to Firebase
            userCoursesRef.setValue(courseStrings).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 如果上传成功，继续跳转到 DashboardActivity
                    // If upload is successful, continue to DashboardActivity
                    val intent = Intent(this, DashboardActivity::class.java)
                    intent.putStringArrayListExtra("updatedClasses", ArrayList(courseStrings))
                    setResult(RESULT_OK, intent)
                    finish()
                } else {
                    // 如果上传失败，显示错误消息
                    // If upload fails, show error message
                    Toast.makeText(this, "Error saving courses", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1 && resultCode == RESULT_OK) {
            val updatedClasses = data?.getStringArrayListExtra("updatedClasses")
            if (updatedClasses != null) {
                student.clearCourses()
                updatedClasses.forEach { courseStr ->
                    val parts = courseStr.split(" ", limit = 2)
                    if (parts.size == 2) {
                        student.addCourse(parts[0], parts[1])
                    }
                }
                addedClassesAdapter.notifyDataSetChanged()
            }
        }
    }
}
