package com.example.edkura.Rao

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.example.edkura.R

class spmatching : AppCompatActivity(), RequestsAdapter.OnRequestActionListener {

    private lateinit var database: DatabaseReference
    private lateinit var requestsRecyclerView: RecyclerView
    private lateinit var adapter: RequestsAdapter
    private lateinit var userSpinner: Spinner
    private lateinit var sendRequestButton: Button

    // Predefined users list
    private val predefinedUsers = listOf("user1", "user2", "user3", "user4")
    // Assume current user is user1
    private val currentUserId = "user3"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.br_spmatching)

        // Initialize Firebase Realtime Database reference
        database = FirebaseDatabase.getInstance().reference

        // Initialize views
        userSpinner = findViewById(R.id.userSpinner)
        sendRequestButton = findViewById(R.id.sendRequestButton)
        requestsRecyclerView = findViewById(R.id.requestsRecyclerView)
        requestsRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RequestsAdapter(emptyList(), this)
        requestsRecyclerView.adapter = adapter

        // Set up Spinner with predefined users excluding current user
        val usersToSend = predefinedUsers.filter { it != currentUserId }
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, usersToSend)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        userSpinner.adapter = spinnerAdapter

        // Send request button click
        sendRequestButton.setOnClickListener {
            val selectedUser = userSpinner.selectedItem as String
            sendStudyPartnerRequest(selectedUser)
        }

        // Listen for incoming study partner requests for the current user
        listenForIncomingRequests()
    }

    private fun sendStudyPartnerRequest(receiverId: String) {
        // Query the entire study_partner_requests node to check for any existing relationship.
        database.child("study_partner_requests")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var duplicateFound = false
                    snapshot.children.forEach { child ->
                        val request = child.getValue(StudyPartnerRequest::class.java)
                        if (request != null) {
                            // Outgoing: Check if a request from current user to receiver exists with status pending or accepted.
                            if (request.senderId == currentUserId && request.receiverId == receiverId &&
                                (request.status == "pending" || request.status == "accepted")
                            ) {
                                duplicateFound = true
                                return@forEach
                            }
                            // Incoming: Check if a request from receiver to current user exists and is accepted.
                            if (request.senderId == receiverId && request.receiverId == currentUserId &&
                                request.status == "accepted"
                            ) {
                                duplicateFound = true
                                return@forEach
                            }
                        }
                    }
                    if (duplicateFound) {
                        Toast.makeText(
                            this@spmatching,
                            "User $receiverId is already added",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // No duplicate found; create a new request node with an auto-generated key.
                        val newRequestRef = database.child("study_partner_requests").push()
                        val requestData = StudyPartnerRequest(
                            id = newRequestRef.key ?: "",
                            senderId = currentUserId,
                            receiverId = receiverId,
                            status = "pending"
                        )
                        newRequestRef.setValue(requestData)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this@spmatching,
                                    "Request sent to $receiverId",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this@spmatching,
                                    "Failed to send request: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@spmatching,
                        "Error checking existing requests: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun listenForIncomingRequests() {
        // Query for requests where receiverId equals currentUserId
        database.child("study_partner_requests").orderByChild("receiverId").equalTo(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val requests = mutableListOf<StudyPartnerRequest>()
                    snapshot.children.forEach { child ->
                        val request = child.getValue(StudyPartnerRequest::class.java)
                        // Only add if request status is pending
                        if (request != null && request.status == "pending") {
                            request.id = child.key ?: ""
                            requests.add(request)
                        }
                    }
                    adapter.updateRequests(requests)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@spmatching, "Error fetching requests: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onAccept(request: StudyPartnerRequest) {
        // Update request status to accepted
        database.child("study_partner_requests").child(request.id)
            .child("status").setValue("accepted")
            .addOnSuccessListener {
                Toast.makeText(this, "Request accepted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to accept request: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDecline(request: StudyPartnerRequest) {
        // Update request status to declined
        database.child("study_partner_requests").child(request.id)
            .child("status").setValue("declined")
            .addOnSuccessListener {
                Toast.makeText(this, "Request declined", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to decline request: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}