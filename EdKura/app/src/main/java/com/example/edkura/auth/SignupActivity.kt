package com.example.edkura.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.edkura.DashboardActivity
import com.example.edkura.R
import com.example.edkura.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SignupActivity : AppCompatActivity()
    {
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var signupButton: Button
    private lateinit var loginButton: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?)
        {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        emailEditText = findViewById(R.id.editTextEmail)
        passwordEditText = findViewById(R.id.editTextPassword)
        signupButton = findViewById(R.id.buttonSignup)
        loginButton = findViewById(R.id.buttonLogin)

        signupButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val nameEditText: EditText = findViewById(R.id.editTextName)
            val name = nameEditText.text.toString().trim()

            if (!email.endsWith("myci.csuci.edu")) {
                Toast.makeText(this, "Please use an myci.csuci.edu email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email and Password cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (name.isEmpty()) {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                        val userRef = FirebaseDatabase.getInstance().reference.child("users").child(userId)

                        val userData = mapOf(
                            "email" to email,
                            "name" to name,
                            "courses" to listOf<String>() // empty courses initially
                        )

                        userRef.setValue(userData).addOnCompleteListener {
                            if (it.isSuccessful) {
                                startActivity(Intent(this, DashboardActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(this, "Database Error: ${it.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Signup Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
