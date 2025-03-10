package com.example.edkura.Narciso

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.R
import com.example.edkura.Rao.spmatching
import com.example.edkura.Rao.StudyPartnerRequest
import com.google.firebase.database.*
import com.example.edkura.Narciso.Student
import com.example.edkura.Narciso.StudentAdapter
import android.content.Intent

class CourseDetailActivity : AppCompatActivity() {

    private lateinit var db: DatabaseReference
    private lateinit var courseDetailsContainer: LinearLayout
    private lateinit var studyPartnerDashboardContainer: LinearLayout
    private lateinit var backButton: Button
    private lateinit var studyPartnerButton: Button
    private lateinit var addUserItem: CardView
    private lateinit var studentsRecyclerView: RecyclerView
    private val currentUserId = "user1"  // Adjust this as needed

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.course_detail)

        // Set up window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.course_detail_activity)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Realtime Database
        db = FirebaseDatabase.getInstance().reference

        // Initialize views
        val textsubject: TextView = findViewById(R.id.textsubject)
        val textCourseName: TextView = findViewById(R.id.textCourseName)
        studyPartnerButton = findViewById(R.id.studyPartnerButton)
        courseDetailsContainer = findViewById(R.id.courseDetailsContainer)
        studyPartnerDashboardContainer = findViewById(R.id.studyPartnerDashboardContainer)
        backButton = findViewById(R.id.backButton)
        addUserItem = findViewById(R.id.addUserItem)
        studentsRecyclerView = findViewById(R.id.studentsRecyclerView)
        studentsRecyclerView.layoutManager = LinearLayoutManager(this)

        val subject = intent.getStringExtra("subject") ?: "Unknown subject"
        val courseName = intent.getStringExtra("courseName") ?: "Unknown Course"
        textsubject.text = "subject: $subject"
        textCourseName.text = "Course: $courseName"

        // Show dashboard when study partner button is tapped and load accepted study partners.
        studyPartnerButton.setOnClickListener {
            courseDetailsContainer.visibility = View.GONE
            studyPartnerDashboardContainer.visibility = View.VISIBLE
            loadAcceptedStudyPartners()
        }

        // Launch spmatching activity when add button is tapped.
        addUserItem.setOnClickListener {
            val intent = Intent(this, spmatching::class.java)
            startActivity(intent)
        }

        // Back button returns to course details.
        backButton.setOnClickListener {
            studyPartnerDashboardContainer.visibility = View.GONE
            courseDetailsContainer.visibility = View.VISIBLE
        }
    }

    /**
     * Loads accepted study partner requests (i.e. added partners) for the current user.
     * The query fetches accepted requests and then filters for ones where the current user
     * is either the sender or receiver. The other party is then displayed using StudentAdapter.
     */
    private fun loadAcceptedStudyPartners() {
        db.child("study_partner_requests")
            .orderByChild("status")
            .equalTo("accepted")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val partnerList = mutableListOf<Student>()
                    snapshot.children.forEach { child ->
                        val request = child.getValue(StudyPartnerRequest::class.java)
                        if (request != null) {
                            // Check if current user is involved in the accepted request.
                            if (request.senderId == currentUserId) {
                                // Current user sent the request; partner is receiver.
                                partnerList.add(Student(id = request.receiverId, name = request.receiverId, course = ""))
                            } else if (request.receiverId == currentUserId) {
                                // Current user received the request; partner is sender.
                                partnerList.add(Student(id = request.senderId, name = request.senderId, course = ""))
                            }
                        }
                    }
                    // Update the RecyclerView with accepted study partners.
                    if (partnerList.isNotEmpty()) {
                        val adapter = StudentAdapter(partnerList)
                        studentsRecyclerView.adapter = adapter
                        studentsRecyclerView.visibility = View.VISIBLE
                    } else {
                        studentsRecyclerView.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Optionally handle errors here.
                }
            })
    }
}