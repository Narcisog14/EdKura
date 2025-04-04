package com.example.edkura.Rao

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.example.edkura.R
import com.example.edkura.Rao.RequestsAdapter


class spmatching : AppCompatActivity(), RequestsAdapter.OnRequestActionListener {

    private lateinit var database: DatabaseReference
    private lateinit var requestsRecyclerView: RecyclerView
    private lateinit var adapter: RequestsAdapter
    private lateinit var userSpinner: Spinner
    private lateinit var sendRequestButton: Button

    private val eligibleUsers = mutableListOf<Pair<String, String>>()

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.br_spmatching)


        database = FirebaseDatabase.getInstance().reference


        userSpinner = findViewById(R.id.userSpinner)
        sendRequestButton = findViewById(R.id.sendRequestButton)
        requestsRecyclerView = findViewById(R.id.requestsRecyclerView)
        requestsRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RequestsAdapter(emptyList(), this)
        requestsRecyclerView.adapter = adapter

        ensureUserExistsAndLoadEligibleUsers()

        sendRequestButton.setOnClickListener {
            val position = userSpinner.selectedItemPosition
            if (position < 0 || position >= eligibleUsers.size) {
                Toast.makeText(this, "Please select a valid user", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedUserId = eligibleUsers[position].first
            sendStudyPartnerRequest(selectedUserId)
        }

        listenForIncomingRequests()
    }

    private fun ensureUserExistsAndLoadEligibleUsers() {
        database.child("users").child(currentUserId).child("email").setValue(FirebaseAuth.getInstance().currentUser?.email)
        database.child("users").child(currentUserId).child("courses").get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                database.child("users").child(currentUserId).child("courses").setValue(listOf("Comp210", "Math250"))
            }
            loadEligibleUsers()
        }
    }

    private fun loadEligibleUsers() {
        database.child("users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                eligibleUsers.clear() // Clear previous data clearly

                val currentUserCourses = snapshot.child(currentUserId).child("courses")
                    .children.mapNotNull { it.getValue(String::class.java) }.toSet()

                snapshot.children.forEach { userSnap ->
                    val userId = userSnap.key ?: return@forEach
                    if (userId != currentUserId) {
                        val userCourses = userSnap.child("courses")
                            .children.mapNotNull { it.getValue(String::class.java) }.toSet()
                        val userName = userSnap.child("name").getValue(String::class.java) ?: "Unknown"

                        if (currentUserCourses.intersect(userCourses).isNotEmpty()) {
                            eligibleUsers.add(Pair(userId, userName))
                        }
                    }
                }

                val spinnerAdapter = ArrayAdapter(
                    this@spmatching,
                    android.R.layout.simple_spinner_item,
                    eligibleUsers.map { it.second }
                )
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                userSpinner.adapter = spinnerAdapter

                if (eligibleUsers.isEmpty()) {
                    Toast.makeText(this@spmatching, "No eligible users found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@spmatching, "Error loading users: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun sendStudyPartnerRequest(receiverId: String) {
        database.child("study_partner_requests")
            .orderByChild("senderId").equalTo(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val duplicate = snapshot.children.any {
                        it.child("receiverId").getValue(String::class.java) == receiverId &&
                                (it.child("status").getValue(String::class.java) in listOf("pending", "accepted"))
                    }

                    if (duplicate) {
                        Toast.makeText(this@spmatching, "Already requested or connected", Toast.LENGTH_SHORT).show()
                    } else {
                        // First retrieve sender's name clearly
                        database.child("users").child(currentUserId).child("name").get()
                            .addOnSuccessListener { nameSnapshot ->
                                val senderName = nameSnapshot.getValue(String::class.java) ?: "Unknown"

                                val newRequestRef = database.child("study_partner_requests").push()
                                val requestData = StudyPartnerRequest(
                                    id = newRequestRef.key ?: "",
                                    senderId = currentUserId,
                                    receiverId = receiverId,
                                    status = "pending",
                                    senderName = senderName  // Now correctly sets sender's name
                                )
                                newRequestRef.setValue(requestData).addOnSuccessListener {
                                    Toast.makeText(this@spmatching, "Request sent", Toast.LENGTH_SHORT).show()
                                }.addOnFailureListener {
                                    Toast.makeText(this@spmatching, "Request failed", Toast.LENGTH_SHORT).show()
                                }
                            }.addOnFailureListener {
                                Toast.makeText(this@spmatching, "Could not retrieve sender name", Toast.LENGTH_SHORT).show()

                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                    Toast.makeText(this@spmatching, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun listenForIncomingRequests() {
        database.child("study_partner_requests").orderByChild("receiverId").equalTo(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val requests = mutableListOf<StudyPartnerRequest>()

                    snapshot.children.forEach { child ->
                        val request = child.getValue(StudyPartnerRequest::class.java)
                        if (request != null && request.status == "pending") {
                            database.child("users").child(request.senderId).child("name")
                                .get().addOnSuccessListener { nameSnapshot ->
                                    request.senderName = nameSnapshot.getValue(String::class.java) ?: "Unknown"
                                    adapter.notifyDataSetChanged()
                                }
                            requests.add(request)
                        }
                    }


                    adapter.updateRequests(requests)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@spmatching, "Error fetching requests", Toast.LENGTH_SHORT).show()

                }
            })
    }

    override fun onAccept(request: StudyPartnerRequest) {
        database.child("study_partner_requests").child(request.id)
            .child("status").setValue("accepted")
            .addOnSuccessListener {
                Toast.makeText(this, "Accepted ${request.senderName}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Acceptance failed", Toast.LENGTH_SHORT).show()

            }
    }

    override fun onDecline(request: StudyPartnerRequest) {
        database.child("study_partner_requests").child(request.id)
            .child("status").setValue("declined")
            .addOnSuccessListener {
                Toast.makeText(this, "Declined ${request.senderName}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Decline failed", Toast.LENGTH_SHORT).show()
            }
    }


}