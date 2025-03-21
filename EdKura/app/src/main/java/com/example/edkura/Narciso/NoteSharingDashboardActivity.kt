package com.example.edkura.Narciso

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.edkura.R

class NoteSharingDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_sharing_dashboard) // Set the layout

        // Initialize UI elements using findViewById
        val dashboardTitle: TextView = findViewById(R.id.dashboardTitle)
        val noteListContainer: LinearLayout = findViewById(R.id.noteListContainer)
        val backButton: Button = findViewById(R.id.backButton)
        // Set title.
        dashboardTitle.text = "Note Sharing Dashboard"

        // Set an onclick listener to the back button.
        backButton.setOnClickListener{
            finish()
        }
        // Add more UI elements and logic here.
    }
}