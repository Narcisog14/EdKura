package com.example.edkura.Narciso

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.edkura.R

class CourseDetailActivity : AppCompatActivity() {
    private lateinit var studyPartner: StudyPartner
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
        val studyPartnerButton: Button = findViewById(R.id.studyPartnerButton)
        val courseDetailsContainer: LinearLayout = findViewById(R.id.courseDetailsContainer)
        val studyPartnerDashboardContainer: LinearLayout = findViewById(R.id.studyPartnerDashboardContainer)
        val backButton: Button = findViewById(R.id.backButton)

        val major = intent.getStringExtra("major") ?: "Unknown Major"
        val courseName = intent.getStringExtra("courseName") ?: "Unknown Course"

        textMajor.text = "Major: $major"
        textCourseName.text = "Course: $courseName"

        // Create an instance of the StudyPartner class
        studyPartner = StudyPartner(courseDetailsContainer, studyPartnerDashboardContainer, backButton)

        studyPartnerButton.setOnClickListener {
            studyPartner.showStudyPartnerDashboard()
        }
    }
}