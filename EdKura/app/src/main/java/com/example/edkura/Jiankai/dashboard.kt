package com.example.edkura
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.Jiankai.classManagement
import com.example.edkura.Jiankai.Student
import com.example.edkura.Jiankai.CustomRequestsAdapter
import CourseAdapter
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.edkura.Narciso.CourseDetailActivity
import com.example.edkura.auth.LoginActivity
import com.example.edkura.Jiankai.ProfileActivity
import com.example.edkura.Jiankai.jiankaiUI.CustomCanvasView
import com.example.edkura.Rao.StudyPartnerRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DashboardActivity : AppCompatActivity(), CustomRequestsAdapter.OnRequestActionListener {
    private lateinit var student: Student  // Student object that holds courses locally
    private lateinit var recyclerViewCourse: RecyclerView
    private lateinit var courseAdapter: CourseAdapter
    private lateinit var authUser: FirebaseAuth

    private lateinit var customCanvasView: CustomCanvasView
    private lateinit var sidebarView: View

    private lateinit var userName: TextView
    private lateinit var userEmail: TextView
    private lateinit var chat:ImageButton
    private lateinit var studyPartner:ImageButton
    private lateinit var settings: ImageButton
    private lateinit var logoutButton: ImageButton

    private lateinit var database: DatabaseReference

    private lateinit var requestRecyclerView: RecyclerView
    private lateinit var requestsAdapter: CustomRequestsAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        setContentView(R.layout.jk_dashboard)

        authUser = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        recyclerViewCourse = findViewById(R.id.recyclerViewCourses)
        recyclerViewCourse.layoutManager = LinearLayoutManager(this)

        student = Student()

        // Initialize adapter with empty data; set up listeners in callbacks
        courseAdapter = CourseAdapter(listOf(),
            onLongClick = { position -> showDeleteDialog(position) },
            onItemClick = { course -> goToCourseDetail(course) }
        )
        recyclerViewCourse.adapter = courseAdapter
        updateCourseList()

        // Navigate to class management
        val buttonSetting: ImageButton = findViewById(R.id.buttonSetting)
        buttonSetting.setOnClickListener {
            startActivity(Intent(this, classManagement::class.java))
        }

        //sidebar
        customCanvasView = findViewById(R.id.customCanvasView)
        sidebarView = findViewById(R.id.profileContent)
        settings = findViewById(R.id.sidebar_setting)
        userName = findViewById(R.id.username)
        userEmail = findViewById(R.id.useremail)
        studyPartner = findViewById(R.id.sp_icon)
        logoutButton = findViewById(R.id.logout)

        requestRecyclerView = findViewById(R.id.requestRecyclerView)
        requestRecyclerView.layoutManager = LinearLayoutManager(this)

        requestsAdapter = CustomRequestsAdapter(listOf(), this)
        requestRecyclerView.adapter = requestsAdapter

        // setting button->password change
        settings.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        logoutButton.setOnClickListener {
            authUser.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Set up listener for rectWidth changes
        customCanvasView.setOnRectWidthChangeListener(object : CustomCanvasView.OnRectWidthChangeListener {
            override fun onRectWidthChanged(rectWidth: Float) {
                updateSidebarLayout(rectWidth)
            }
        })

        // Initial setup
        sidebarView.post {
            updateSidebarLayout(customCanvasView.getCurrentRectWidth())
        }

        fetchUserData()
        // Load requests from Firebase
        loadRequests()

        // Set up custom canvas width listener
        customCanvasView.setOnRectWidthChangeListener(object : CustomCanvasView.OnRectWidthChangeListener {
            override fun onRectWidthChanged(rectWidth: Float) {
                updateSidebarLayout(rectWidth)
            }
        })
    }

    private fun updateSidebarLayout(rectWidth: Float) {
        if (rectWidth <= 20f) {

            sidebarView.visibility = View.GONE


            val params = sidebarView.layoutParams as ConstraintLayout.LayoutParams
            params.width = 0
            sidebarView.layoutParams = params
            return
        }

        // 否则显示它
        //Otherwise display it
        sidebarView.visibility = View.VISIBLE

        val params = sidebarView.layoutParams as ConstraintLayout.LayoutParams
        params.width = rectWidth.toInt()
        params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        params.endToEnd = ConstraintLayout.LayoutParams.UNSET
        sidebarView.layoutParams = params
        sidebarView.requestLayout()
    }

    // Update the list in the adapter based on the student's addedClasses
    private fun updateCourseList() {

        val courseList = student.addedClasses.map { course ->
            if (course.startsWith("Computer Science")) {
                // only for computer science
                val subject = "Computer Science"

                val courseName = course.substring("Computer Science".length).trim()
                Pair(subject, courseName)
            } else {

                val parts = course.split(" ", limit = 2)  // 按空格分割课程信息
                val subject = if (parts.size > 1) parts[0] else "Unknown"  // 如果分割成功，取第一个部分为科目
                val courseName = if (parts.size > 1) parts[1] else course  // 第二部分为课程名
                Pair(subject, courseName)
            }
        }

        courseAdapter.updateData(courseList)
    }

    // Show a confirmation dialog to remove a course.
    private fun showDeleteDialog(position: Int) {
        val courseToDelete = student.addedClasses[position]
        AlertDialog.Builder(this)
            .setTitle("Delete Course")
            .setMessage("Are you sure you want to delete: $courseToDelete?")
            .setPositiveButton("Delete") { _, _ ->
                // Remove from local list and update UI
                student.removeCourseAt(position)
                updateCourseList()
                // Now update Firebase: remove the course from the user's courses node.
                val userId = authUser.currentUser?.uid
                userId?.let {
                    FirebaseDatabase.getInstance().reference.child("users")
                        .child(it)
                        .child("courses")
                        .setValue(student.addedClasses)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Course removed successfully", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error removing course: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Open CourseDetailActivity with the selected course's data.
    private fun goToCourseDetail(course: Pair<String, String>) {
        val intent = Intent(this, CourseDetailActivity::class.java)
        intent.putExtra("subject", course.first)
        intent.putExtra("courseName", course.second)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        student.loadCoursesFromFirebase {
            updateCourseList()
        }
    }

    private fun fetchUserData() {
        val user = authUser.currentUser
        if (user != null) {
            val email = user.email ?: "No Email"
            val userId = user.uid

            userEmail.text = email  // 更新邮箱 TextView

            FirebaseDatabase.getInstance().reference
                .child("users")
                .child(userId)
                .child("name")
                .get()
                .addOnSuccessListener { snapshot ->
                    val name = snapshot.getValue(String::class.java) ?: userId
                    userName.text = "Name: $name"
                    userEmail.text = "Email: $email"
                }
                .addOnFailureListener {
                    userName.text = "Name: $userId"
                }
        }
    }
    private fun loadRequests() {
        val userId = authUser.currentUser?.uid ?: return

        val ref = FirebaseDatabase.getInstance().getReference("study_partner_requests")
            .orderByChild("receiverId").equalTo(userId)

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val requestList = mutableListOf<StudyPartnerRequest>()

                for (child in snapshot.children) {
                    val request = child.getValue(StudyPartnerRequest::class.java)
                    if (request != null && request.status == "pending") {
                        requestList.add(request)

                        if (request.senderName.isNullOrEmpty() || request.senderName == "Unknown") {
                            FirebaseDatabase.getInstance().getReference("users")
                                .child(request.senderId)
                                .child("name")
                                .get()
                                .addOnSuccessListener { nameSnapshot ->
                                    request.senderName = nameSnapshot.getValue(String::class.java) ?: "Unknown"
                                    requestsAdapter.notifyDataSetChanged()
                                }
                        }
                    }
                }

                // 更新适配器数据
                // Update adapter data
                requestsAdapter.updateRequests(requestList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DashboardActivity, "Failed to load requests", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Handle request actions
    override fun onAccept(request: StudyPartnerRequest) {
        database.child("study_partner_requests").child(request.id)
            .child(request.id)
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
            .child(request.id)
            .child("status").setValue("declined")
            .addOnSuccessListener {
                Toast.makeText(this, "Declined ${request.senderName}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Decline failed", Toast.LENGTH_SHORT).show()
            }
    }
}