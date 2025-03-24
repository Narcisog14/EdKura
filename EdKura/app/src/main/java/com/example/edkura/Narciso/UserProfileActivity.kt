package com.example.edkura.Narciso

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.edkura.R

class UserProfileActivity : AppCompatActivity() {

    // Pre-defined user data (same as in DashboardActivity)
    private val preDefinedUsers = listOf(
        User("user1", "Alice Johnson", "alice@example.com", "Computer Science"),
        User("user2", "Bob Williams", "bob@example.com", "Engineering"),
        User("user3", "Charlie Brown", "charlie@example.com", "Business")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        // Get the user ID from the Intent
        val userId = intent.getStringExtra("USER_ID")

        // Find the user in the list
        val user = preDefinedUsers.find { it.id == userId }

        // Display user data
        if (user != null) {
            displayUserData(user)
        }
    }

    private fun displayUserData(user: User) {
        val nameTextView: TextView = findViewById(R.id.name)
        val emailTextView: TextView = findViewById(R.id.email)
        val majorTextView: TextView = findViewById(R.id.major)

        nameTextView.text = user.name
        emailTextView.text = user.email
        majorTextView.text = user.major
    }
}