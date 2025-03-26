package com.example.edkura.Jiankai

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.edkura.R
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileActivity : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val user = auth.currentUser!!
        val userId = user.uid

        // Fetch name from Firebase
        FirebaseDatabase.getInstance().reference
            .child("users")
            .child(userId)
            .child("name")
            .get()
            .addOnSuccessListener { snapshot ->
                findViewById<TextView>(R.id.textUserName).text = snapshot.getValue(String::class.java) ?: user.email
            }
            .addOnFailureListener {
                findViewById<TextView>(R.id.textUserName).text = user.email
            }

        findViewById<TextView>(R.id.textUserEmail).text = user.email

        findViewById<Button>(R.id.buttonChangePassword).setOnClickListener {
            val old = findViewById<EditText>(R.id.editOldPassword).text.toString()
            val new = findViewById<EditText>(R.id.editNewPassword).text.toString()
            val confirm = findViewById<EditText>(R.id.editConfirmPassword).text.toString()

            if(new != confirm) return@setOnClickListener showToast("Passwords do not match")

            val credential = EmailAuthProvider.getCredential(user.email!!, old)
            user.reauthenticate(credential)
                .addOnSuccessListener {
                    user.updatePassword(new)
                        .addOnSuccessListener { showToast("Password changed") }
                        .addOnFailureListener { showToast("Error: ${it.message}") }
                }
                .addOnFailureListener { showToast("Current password incorrect") }
        }

        findViewById<Button>(R.id.buttonBackDashboard).setOnClickListener { finish() }
    }

    private fun showToast(msg:String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}