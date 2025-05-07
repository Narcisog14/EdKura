package com.example.edkura
import android.Manifest
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
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.edkura.GroupProject.GroupInvite
import com.example.edkura.Narciso.CourseDetailActivity
import com.example.edkura.auth.LoginActivity
import com.example.edkura.Jiankai.ProfileActivity
import com.example.edkura.Jiankai.jiankaiUI.CustomCanvasView
import com.example.edkura.Rao.StudyPartnerRequest
import com.example.edkura.chat.AllChatsActivity
import com.example.edkura.chat.ChatActivity.ChatMessage
import com.example.edkura.chat.ChatMessageAdapter
import com.example.edkura.utils.NotificationUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DashboardActivity : AppCompatActivity(), CustomRequestsAdapter.OnRequestActionListener {

    companion object {
        private const val REQUEST_NOTIF = 1001
    }

    private lateinit var student: Student  // Student object that holds courses locally
    private lateinit var recyclerViewCourse: RecyclerView
    private lateinit var courseAdapter: CourseAdapter
    private lateinit var authUser: FirebaseAuth

    //Number of requests



    // these two point at the little red badges
    private lateinit var chatBadge: TextView

    // counters
    private var unreadChatCount = 0
    private var requestId = ""

    // remember which message‐IDs we already notified on
    private val seenMessages = mutableSetOf<String>()

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
    private lateinit var acceptButton: ImageButton
    private lateinit var declineButton: ImageButton

    private val prefs by lazy {
        getSharedPreferences("chat_prefs", MODE_PRIVATE)
    }

    /** When did I last read this room? (millis) */
    private fun getLastRead(roomId: String): Long =
        prefs.getLong("last_read_$roomId", 0L)

    /** Update the “last read” marker for this room */
    private fun setLastRead(roomId: String, ts: Long) =
        prefs.edit().putLong("last_read_$roomId", ts).apply()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        setContentView(R.layout.jk_dashboard)

        authUser = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        chatBadge = findViewById(R.id.chatBadge)

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

        // Navigate to all chats
        findViewById<ImageButton>(R.id.buttonChat).setOnClickListener {
            // reset counter + hide badge
            unreadChatCount = 0
            updateBadge(chatBadge, 0)
            // also clear our seen‐set so future chats can notify again

            startActivity(
                Intent(this, AllChatsActivity::class.java)
            )
        }

        //sidebar
        customCanvasView = findViewById(R.id.customCanvasView)
        sidebarView = findViewById(R.id.profileContent)
        settings = findViewById(R.id.sidebar_setting)
        userName = findViewById(R.id.username)
        userEmail = findViewById(R.id.useremail)
        logoutButton = findViewById(R.id.logout)

        requestRecyclerView = findViewById(R.id.requestRecyclerView)
        requestRecyclerView.layoutManager = LinearLayoutManager(this)

        requestsAdapter = CustomRequestsAdapter(this)
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIF
                )
            }
        }

        listenForIncomingRequests()
        listenForIncomingChats()
        listenForIncomingGroupInvites()
        listenForIncomingGroupChats()
        loadRequests()
    }

    // Listen for incoming study-partner requests
    private fun listenForIncomingRequests() {
        val me = authUser.currentUser?.uid ?: return
        database.child("study_partner_requests")
            .orderByChild("receiverId")
            .equalTo(me)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    // if you still want the popup—
                    snapshot.children.forEach {
                        val req = it.getValue(StudyPartnerRequest::class.java) ?: return@forEach
                        if (req.status == "pending") {
                            NotificationUtils.sendNotification(
                                this@DashboardActivity,
                                "New study-partner request",
                                "${req.senderName} wants to connect"
                            )
                        }
                    }
                }
                override fun onCancelled(e: DatabaseError) {}
            })
    }

    private fun listenForIncomingGroupInvites() {
        val me = authUser.currentUser?.uid ?: return
        database.child("groupInvites")
            .orderByChild("inviteeId")
            .equalTo(me)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snap: DataSnapshot, prev: String?) {
                    val invite = snap.getValue(GroupInvite::class.java) ?: return
                    if (invite.status == "pending") {
                        // fetch the group’s name if you like, here we assume invite.groupName was already set
                        NotificationUtils.sendNotification(
                            this@DashboardActivity,
                            "New group invitation",
                            "You’ve been invited to join ${invite.GroupName}"
                        )
                    }
                }
                override fun onChildChanged(s: DataSnapshot, p3: String?) {}
                override fun onChildRemoved(s: DataSnapshot) {}
                override fun onChildMoved(s: DataSnapshot, p3: String?) {}
                override fun onCancelled(err: DatabaseError) {}
            })
    }

        private fun listenForIncomingChats() {
        val me = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val chatsRef = database.child("chats")

        chatsRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(roomSnap: DataSnapshot, prev: String?) {
                val roomId = roomSnap.key ?: return
                if (!roomId.contains(me)) return

                // for each new message in that room…
                roomSnap.child("messages").ref
                    .addChildEventListener(object : ChildEventListener {
                        override fun onChildAdded(msgSnap: DataSnapshot, p2: String?) {
                            val sender = msgSnap.child("senderId").getValue(String::class.java) ?: return
                            if (sender == me) return

                            val ts = msgSnap.child("timestamp").getValue(Long::class.java) ?: return
                            val lastRead = getLastRead(roomId)
                            if (ts <= lastRead) {
                                // I’ve already “read” up to here
                                return
                            }

                            // bump our in-app badge
                            unreadChatCount++
                            updateBadge(chatBadge, unreadChatCount)

                            // fire a system notification
                            val text = msgSnap.child("text").getValue(String::class.java) ?: ""
                            database.child("users").child(sender).child("name")
                                .get().addOnSuccessListener { nameSnap ->
                                    val who = nameSnap.getValue(String::class.java) ?: sender
                                    NotificationUtils.sendNotification(
                                        this@DashboardActivity,
                                        "New message from $who",
                                        text
                                    )
                                }
                        }
                        override fun onChildChanged(s: DataSnapshot, p2: String?) {}
                        override fun onChildRemoved(s: DataSnapshot) {}
                        override fun onChildMoved(s: DataSnapshot, p2: String?) {}
                        override fun onCancelled(err: DatabaseError) {}
                    })
            }

            override fun onChildChanged(s: DataSnapshot, p: String?) {}
            override fun onChildRemoved(s: DataSnapshot) {}
            override fun onChildMoved(s: DataSnapshot, p: String?) {}
            override fun onCancelled(err: DatabaseError) {}
        })
    }

    private fun listenForIncomingGroupChats() {
        val me = authUser.currentUser?.uid ?: return

        // 1) first load all the group IDs where I'm a member
        database.child("projectGroups")
            .orderByChild("members/$me")
            .equalTo(true)
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // for each group I'm in, listen for new messages
                    snapshot.children.forEach { groupSnap ->
                        val groupId = groupSnap.key ?: return@forEach
                        val groupName = groupSnap.child("name").getValue(String::class.java) ?: groupId

                        database.child("groupChats")
                            .child(groupId)
                            .child("messages")
                            .addChildEventListener(object: ChildEventListener {
                                override fun onChildAdded(msgSnap: DataSnapshot, previousChildName: String?) {
                                    // only notify when *they* send
                                    val sender = msgSnap.child("senderId").getValue(String::class.java)
                                    val text   = msgSnap.child("text").getValue(String::class.java) ?: ""
                                    if (sender != null && sender != me) {
                                        NotificationUtils.sendNotification(
                                            this@DashboardActivity,
                                            "New message in $groupName",
                                            text
                                        )
                                    }
                                }
                                override fun onChildChanged(s: DataSnapshot, p2: String?) {}
                                override fun onChildRemoved(s: DataSnapshot) {}
                                override fun onChildMoved(s: DataSnapshot, p2: String?) {}
                                override fun onCancelled(err: DatabaseError) {}
                            })
                    }
                }
                override fun onCancelled(error: DatabaseError) { /* ignore */ }
            })
    }

    // handle the POST_NOTIFICATIONS permission result (optional)
    private fun updateBadge(badge: TextView, count: Int) {
        if (count > 0) {
            badge.text = count.toString()
            badge.visibility = View.VISIBLE
        } else {
            badge.visibility = View.GONE
        }
    }


    // (optional) handle user’s response to the POST_NOTIFICATIONS permission request
    override fun onRequestPermissionsResult(
        requestCode: Int, perms: Array<out String>, results: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, perms, results)
        if (requestCode == REQUEST_NOTIF &&
            results.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
        }
    }


    private fun updateSidebarLayout(rectWidth: Float) {
        if (rectWidth <= 100f) {

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

            userEmail.text = email

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

        // Lists to store both types of requests
        val studyPartnerRequests = mutableListOf<StudyPartnerRequest>()
        val groupInvites = mutableListOf<GroupInvite>()

        // Counters to track async operations
        var studyRequestsComplete = false
        var groupInvitesComplete = false

        // 1. Load StudyPartnerRequests
        val studyRef = FirebaseDatabase.getInstance().getReference("study_partner_requests")
            .orderByChild("receiverId").equalTo(userId)

        studyRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Clear the list for a fresh reload
                studyPartnerRequests.clear()

                // Track how many sender names we need to resolve
                val totalRequests = snapshot.childrenCount
                var processedRequests = 0L

                if (totalRequests == 0L) {
                    // No study partner requests, mark as complete
                    studyRequestsComplete = true
                    checkAndUpdateAdapter(studyPartnerRequests, groupInvites, studyRequestsComplete, groupInvitesComplete)
                    return
                }

                for (child in snapshot.children) {
                    val request = child.getValue(StudyPartnerRequest::class.java)
                    if (request != null && request.status == "pending") {
                        studyPartnerRequests.add(request)

                        // Fetch sender name asynchronously
                        FirebaseDatabase.getInstance().getReference("users")
                            .child(request.senderId)
                            .child("name")
                            .get()
                            .addOnSuccessListener { nameSnapshot ->
                                request.senderName = nameSnapshot.getValue(String::class.java) ?: "Unknown"
                                processedRequests++

                                // Check if we've processed all requests
                                if (processedRequests == totalRequests) {
                                    studyRequestsComplete = true
                                    checkAndUpdateAdapter(studyPartnerRequests, groupInvites,
                                        studyRequestsComplete, groupInvitesComplete)
                                }
                            }
                            .addOnFailureListener {
                                processedRequests++
                                // Still count failed requests
                                if (processedRequests == totalRequests) {
                                    studyRequestsComplete = true
                                    checkAndUpdateAdapter(studyPartnerRequests, groupInvites,
                                        studyRequestsComplete, groupInvitesComplete)
                                }
                            }
                    } else {
                        // Skip this request but count it as processed
                        processedRequests++
                        if (processedRequests == totalRequests) {
                            studyRequestsComplete = true
                            checkAndUpdateAdapter(studyPartnerRequests, groupInvites,
                                studyRequestsComplete, groupInvitesComplete)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DashboardActivity, "Failed to load study partner requests", Toast.LENGTH_SHORT).show()
                studyRequestsComplete = true
                checkAndUpdateAdapter(studyPartnerRequests, groupInvites, studyRequestsComplete, groupInvitesComplete)
            }
        })

        // 2. Load GroupInvites
        val groupInvitesRef = database.child("groupInvites")
        groupInvitesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Clear the list for a fresh reload
                groupInvites.clear()

                // Track how many group names we need to resolve
                val totalInvites = snapshot.childrenCount
                var processedInvites = 0L

                if (totalInvites == 0L) {
                    // No group invites, mark as complete
                    groupInvitesComplete = true
                    checkAndUpdateAdapter(studyPartnerRequests, groupInvites, studyRequestsComplete, groupInvitesComplete)
                    return
                }

                for (child in snapshot.children) {
                    val invite = child.getValue(GroupInvite::class.java)
                    if (invite != null && invite.inviteeId == userId && invite.status == "pending") {
                        groupInvites.add(invite)
                        requestId = invite.inviteId
                        Log.d("GroupInvite1", "Loaded invite: $invite")

                        // Fetch group name asynchronously
                        database.child("projectGroups")
                            .orderByChild("groupId")
                            .equalTo(invite.groupId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(pgSnapshot: DataSnapshot) {
                                    val groupRef = database.child("projectGroups").child(invite.groupId).child("name")
                                    groupRef.get().addOnSuccessListener { dataSnapshot ->
                                        val groupName = dataSnapshot.getValue(String::class.java) ?: "Unknown"
                                        database.child("groupInvites").child(requestId)
                                            .child("GroupName").setValue(groupName)
                                        Log.d("GroupName", "Group name is: $groupName")
                                        Log.d("DataSync", "StudyRequests: ${studyPartnerRequests.size}, GroupInvites: ${groupInvites.size}")

                                    }

                                    processedInvites++
                                    if (processedInvites == totalInvites) {
                                        groupInvitesComplete = true
                                        checkAndUpdateAdapter(studyPartnerRequests, groupInvites,
                                            studyRequestsComplete, groupInvitesComplete)
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    processedInvites++
                                    if (processedInvites == totalInvites) {
                                        groupInvitesComplete = true
                                        checkAndUpdateAdapter(studyPartnerRequests, groupInvites,
                                            studyRequestsComplete, groupInvitesComplete)
                                    }
                                }
                            })
                    } else {
                        // Skip this invite but count it as processed
                        processedInvites++
                        if (processedInvites == totalInvites) {
                            groupInvitesComplete = true
                            checkAndUpdateAdapter(studyPartnerRequests, groupInvites,
                                studyRequestsComplete, groupInvitesComplete)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DashboardActivity, "Failed to load group invites", Toast.LENGTH_SHORT).show()
                groupInvitesComplete = true
                checkAndUpdateAdapter(studyPartnerRequests, groupInvites, studyRequestsComplete, groupInvitesComplete)
            }
        })
    }

    // Helper method to check if both loads are complete and update adapter
    fun checkAndUpdateAdapter(
        studyPartnerRequests: List<StudyPartnerRequest>,
        groupInvites: List<GroupInvite>,
        studyRequestsComplete: Boolean,
        groupInvitesComplete: Boolean,
    ) {
        if (studyRequestsComplete || groupInvitesComplete) {
            // Both loads are complete, update the adapter
            requestsAdapter.updateData(studyPartnerRequests, groupInvites)
        }
    }

    // Updated handler methods
    override fun onAccept(request: StudyPartnerRequest) {
        if (request.id.isNotEmpty()) {
            database.child("study_partner_requests").child(request.id)
                .child("status").setValue("accepted")
                .addOnSuccessListener {
                    Toast.makeText(
                        this,
                        "Accepted ${request.senderName} for ${request.course}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Acceptance failed", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onGroupAccept(invite: GroupInvite) {
        if (invite.groupId.isNotEmpty()) {
            database.child("groupInvites").child(requestId)
                .child("status").setValue("accepted")
                .addOnSuccessListener {
                    Toast.makeText(
                        this,
                        "Accepted invitation to join ${invite.groupId}",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Add the user to the group members
                    val userId = authUser.currentUser?.uid ?: return@addOnSuccessListener
                    database.child("projectGroups").child(invite.groupId)
                        .child("members").child(userId).setValue(true)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Group acceptance failed", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDecline(request: StudyPartnerRequest) {
        database.child("study_partner_requests").child(request.id)
            .child("status").setValue("declined")
            .addOnSuccessListener {
                Toast.makeText(this, "Declined request from ${request.senderName}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Decline failed", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onGroupDecline(invite: GroupInvite) {
        database.child("groupInvites").child(requestId)
            .child("status").setValue("declined")
            .addOnSuccessListener {
                Toast.makeText(this, "Declined invitation to ${invite.groupId}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Decline failed", Toast.LENGTH_SHORT).show()
            }
    }


}