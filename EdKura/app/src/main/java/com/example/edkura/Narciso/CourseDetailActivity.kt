package com.example.edkura.Narciso

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.edkura.R
import com.example.edkura.Rao.spmatching

class CourseDetailActivity : AppCompatActivity() {
    // Removed studyPartner object usage if we are using spmatching now.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.course_detail)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.course_detail_activity)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val textsubject: TextView = findViewById(R.id.textsubject)
        val textCourseName: TextView = findViewById(R.id.textCourseName)
        val studyPartnerButton: Button = findViewById(R.id.studyPartnerButton)
        val courseDetailsContainer: LinearLayout = findViewById(R.id.courseDetailsContainer)
        val studyPartnerDashboardContainer: LinearLayout = findViewById(R.id.studyPartnerDashboardContainer)
        val backButton: Button = findViewById(R.id.backButton)
        val addUserItem: CardView = findViewById(R.id.addUserItem)

        val subject = intent.getStringExtra("subject") ?: "Unknown subject"
        val courseName = intent.getStringExtra("courseName") ?: "Unknown Course"

        textsubject.text = "subject: $subject"
        textCourseName.text = "Course: $courseName"

        // The studyPartnerButton is still available if needed.
        studyPartnerButton.setOnClickListener {
            // Optionally, you could hide/show containers or add extra logic here.
            studyPartnerDashboardContainer.visibility = LinearLayout.VISIBLE
            courseDetailsContainer.visibility = LinearLayout.GONE
        }

        // When the addUserItem (plus icon card) is clicked, start the spmatching activity.
        addUserItem.setOnClickListener {
            val intent = Intent(this, spmatching::class.java)
            startActivity(intent)
        }

        // The backButton could be used to return to the course details view.
        backButton.setOnClickListener {
            studyPartnerDashboardContainer.visibility = LinearLayout.GONE
            courseDetailsContainer.visibility = LinearLayout.VISIBLE
        }
    }
}