package com.example.edkura.Jiankai

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.edkura.R
import com.google.firebase.auth.FirebaseAuth

// com.example.edkura.Jiankai.ProfileActivity.kt

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val user = FirebaseAuth.getInstance().currentUser
        findViewById<TextView>(R.id.textUserEmail).text = user?.email ?: "No email available"

        findViewById<Button>(R.id.buttonBackDashboard).setOnClickListener {
            finish()
        }
    }
}