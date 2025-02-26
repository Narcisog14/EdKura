package com.example.firebaseusernameapp

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


class MainActivity : AppCompatActivity() {
    private var userId: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val usersLinearLayout = findViewById<LinearLayout>(R.id.usersLinearLayout)
        val mainUserTextView = findViewById<TextView>(R.id.mainUserTextView)

        // Get a reference to the database
        val database = Firebase.database
        val usersRef = database.getReference("users")
        val presenceRef = database.getReference("presence")

        // Delete all previous users
        deleteAllUsers()
        // Delete all previous presence
        deleteAllPresence()

        // Generate a unique user ID
        userId = usersRef.push().key

        // Set user presence to true when connected
        if (userId != null) {
            presenceRef.child(userId!!).setValue(true)
            // Set user presence to false when disconnected
            presenceRef.child(userId!!).onDisconnect().setValue(false)
            // Add a name to the user
            addUser("Default User")
        }

        // Add a listener to read all users
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Clear the linear layout
                usersLinearLayout.removeAllViews()
                // Iterate through all users
                for (userSnapshot in dataSnapshot.children) {
                    val name = userSnapshot.child("name").getValue(String::class.java)
                    val userId = userSnapshot.key
                    if (name != null && userId != null) {
                        // Check if the user is the main user
                        if (userId == this@MainActivity.userId) {
                            // Display the main user in the bottom right corner
                            presenceRef.child(userId).get().addOnSuccessListener {
                                val isOnline = it.getValue(Boolean::class.java) ?: false
                                val status = if (isOnline) " (Online)" else " (Offline)"
                                mainUserTextView.text = "$name$status"
                            }
                        } else {
                            // Create a CardView for the user
                            val cardView = CardView(this@MainActivity)
                            val layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            layoutParams.setMargins(16, 16, 16, 16)
                            cardView.layoutParams = layoutParams
                            cardView.radius = 15F
                            cardView.cardElevation = 10F

                            // Create a TextView for the user's name
                            val userNameTextView = TextView(this@MainActivity)
                            userNameTextView.text = name
                            userNameTextView.textSize = 18F
                            userNameTextView.setPadding(32, 32, 32, 32)

                            // Add the TextView to the CardView
                            cardView.addView(userNameTextView)

                            // Add the CardView to the LinearLayout
                            usersLinearLayout.addView(cardView)

                            // Set a click listener for the CardView
                            cardView.setOnClickListener {
                                presenceRef.child(userId).get().addOnSuccessListener {
                                    val isOnline = it.getValue(Boolean::class.java) ?: false
                                    val status = if (isOnline) "Online" else "Offline"
                                    // Display the status
                                    userNameTextView.text = "$name is $status"
                                }
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
            }
        })
    }
    private fun addUser(userName: String) {
        val database = Firebase.database
        val usersRef = database.getReference("users")
        if (userId != null) {
            usersRef.child(userId!!).child("name").setValue(userName)
        }
    }
    private fun deleteAllUsers() {
        val database = Firebase.database
        val usersRef = database.getReference("users")
        usersRef.removeValue()
    }
    private fun deleteAllPresence() {
        val database = Firebase.database
        val presenceRef = database.getReference("presence")
        presenceRef.removeValue()
    }
}