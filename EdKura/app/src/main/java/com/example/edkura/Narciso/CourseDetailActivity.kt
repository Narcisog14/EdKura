package com.example.edkura.Narciso
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.edkura.R

class CourseDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.course_detail)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.course_detail_activity)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val textMajor: TextView = findViewById(R.id.textMajor)
        val textCourseName: TextView = findViewById(R.id.textCourseName)

        val major = intent.getStringExtra("major") ?: "Unknown Major"
        val courseName = intent.getStringExtra("courseName") ?: "Unknown Course"

        textMajor.text = "Major: $major"
        textCourseName.text = "Course: $courseName"
    }
}