package com.example.edkura.Narciso

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.R
import com.example.edkura.Rao.StudyPartnerRequest
import com.example.edkura.Rao.spmatching
import com.example.edkura.chat.ChatActivity
import com.example.edkura.FileSharing.NoteSharingDashboardActivity
import com.example.edkura.GroupProject.GroupProjectDashboardActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CourseDetailActivity : AppCompatActivity() {

    private val db = FirebaseDatabase.getInstance().reference
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Read current course from Intent extras:
    private val currentCourseName: String by lazy {
        intent.getStringExtra("courseName") ?: ""
    }

    private lateinit var courseDetailsContainer: LinearLayout
    private lateinit var studyPartnerDashboardContainer: View
    private lateinit var backButton: Button
    private lateinit var studyPartnerButton: Button
    private lateinit var groupProjectButton: Button
    private lateinit var addUserItem: CardView
    private lateinit var studentsRecyclerView: RecyclerView
    private lateinit var goToNoteSharingDashboardButton: Button // New button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.course_detail)

        val subject = intent.getStringExtra("subject") ?: "Unknown subject"
        val courseName = intent.getStringExtra("courseName") ?: "Unknown course"
        findViewById<TextView>(R.id.textsubject).text = "Subject: $subject"
        findViewById<TextView>(R.id.textCourseName).text = "Course: $currentCourseName"

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.course_detail_activity)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        courseDetailsContainer = findViewById(R.id.courseDetailsContainer)
        studyPartnerDashboardContainer = findViewById(R.id.studyPartnerDashboardContainer)
        backButton = findViewById(R.id.backButton)
        studyPartnerButton = findViewById(R.id.studyPartnerButton)
        goToNoteSharingDashboardButton =
            findViewById(R.id.goToNoteSharingDashboardButton) // Find the new button
        groupProjectButton = findViewById(R.id.groupProjectButton)
        addUserItem = findViewById(R.id.addUserItem)
        studentsRecyclerView = findViewById(R.id.studentsRecyclerView)
        studentsRecyclerView.layoutManager = LinearLayoutManager(this)

        studyPartnerButton.setOnClickListener {
            courseDetailsContainer.visibility = View.GONE
            studyPartnerDashboardContainer.visibility = View.VISIBLE
            loadPartners()
        }
        goToNoteSharingDashboardButton.setOnClickListener {
            startActivity(Intent(this, NoteSharingDashboardActivity::class.java))
        }
        groupProjectButton.setOnClickListener {
            startActivity(Intent(this, GroupProjectDashboardActivity::class.java))
        }
        addUserItem.setOnClickListener {
            startActivity(Intent(this, spmatching::class.java))
        }
        backButton.setOnClickListener {
            studyPartnerDashboardContainer.visibility = View.GONE
            courseDetailsContainer.visibility = View.VISIBLE

        }
    }

    private fun loadPartners() {
        db.child("study_partner_requests")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val partnerList = mutableListOf<Student>()
                    snapshot.children.forEach { child ->
                        val request =
                            child.getValue(StudyPartnerRequest::class.java) ?: return@forEach
                        // Check if current user is involved and the request is for the current course.
                        if ((request.senderId == currentUserId || request.receiverId == currentUserId)
                            && (request.status == "accepted" || (request.status == "blocked" && request.blockedBy == currentUserId))
                        ) {
                            val partnerId =
                                if (request.senderId == currentUserId) request.receiverId else request.senderId
                            // If the partner is already in our map, add course (if not duplicate); otherwise, initialize.
                            if (partnerMap.containsKey(partnerId)) {
                                if (!partnerMap[partnerId]!!.contains(request.course)) {
                                    partnerMap[partnerId]!!.add(request.course)
                                }
                        }
                    }

                    // Now build a list of Student objects with aggregated courses.
                    val partnerList = mutableListOf<Student>()
                    for ((partnerId, courses) in partnerMap) {
                        db.child("users").child(partnerId).child("name")
                            .get().addOnSuccessListener { nameSnap ->
                                val partnerName = nameSnap.getValue(String::class.java) ?: "Unknown"
                                // For this course context, courses list is likely one element.
                                // If you expect multiple courses, join them into a comma-separated string.
                                val courseStr = courses.joinToString(", ")
                                partnerList.add(
                                    Student(
                                        id = partnerId,
                                        name = "$partnerName ($courseStr)",
                                        status = "accepted", // or "blocked" depending on the request – here you might need extra logic if mixed.
                                        blockedBy = null // In this aggregation, you might store blockedBy if needed.
                                    )
                                )
                                // Update adapter after each addition.
                                studentsRecyclerView.adapter = PartnerAdapter(partnerList)
                                studentsRecyclerView.visibility =
                                    if (partnerList.isEmpty()) View.GONE else View.VISIBLE
                            }
                    }
                    if (partnerMap.isEmpty()) {
                        studentsRecyclerView.adapter = PartnerAdapter(emptyList())
                        studentsRecyclerView.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    inner class PartnerAdapter(private val items: List<Student>) :
        RecyclerView.Adapter<PartnerAdapter.Holder>() {

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val nameBtn: Button = view.findViewById(R.id.buttonPartnerName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = layoutInflater.inflate(R.layout.item_partner, parent, false)
            return Holder(view)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val partner = items[position]
            val blocked = partner.status == "blocked"
            holder.nameBtn.text = if (blocked) "${partner.name} [BLOCKED]" else partner.name

            holder.nameBtn.setOnClickListener {
                if (!blocked) {
                    startActivity(
                        Intent(
                            this@CourseDetailActivity,
                            ChatActivity::class.java
                        ).apply {
                            putExtra("partnerId", partner.id)
                            putExtra("partnerName", partner.name)
                        })
                } else {
                    showToast("Partner is blocked. Long press to unblock or remove.")
                }
            }
            holder.nameBtn.setOnLongClickListener {
                if (blocked && partner.blockedBy == currentUserId) showBlockedPrompt(partner)
                else if (!blocked) showRemoveOrBlockPrompt(partner)
                else showRemoveOnlyPrompt(partner)
                true
            }
        }
    }

    private fun showRemoveOrBlockPrompt(partner: Student) = AlertDialog.Builder(this)
        .setTitle("Warning")
        .setMessage("Remove or Block this partner? You won’t be able to chat until unblocked.")
        .setPositiveButton("Remove") { _, _ -> removePartner(partner) }
        .setNeutralButton("Block") { _, _ -> blockPartner(partner) }
        .setNegativeButton("Cancel", null)
        .show()

    private fun showBlockedPrompt(partner: Student) = AlertDialog.Builder(this)
        .setTitle("Warning")
        .setMessage("This partner is blocked. Unblock or Remove?")
        .setPositiveButton("Unblock") { _, _ -> unblockPartner(partner) }
        .setNegativeButton("Remove") { _, _ -> removePartner(partner) }
        .show()

    private fun showRemoveOnlyPrompt(partner: Student) = AlertDialog.Builder(this)
        .setTitle("Warning")
        .setMessage("This partner blocked you. Remove from your list?")
        .setPositiveButton("Remove") { _, _ -> removePartner(partner) }
        .setNegativeButton("Cancel", null)
        .show()

    private fun removePartner(p: Student) {
        val pid = p.id ?: return
        db.child("study_partner_requests").orderByChild("status").equalTo(p.status)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    s.children.forEach { c ->
                        val r = c.getValue(StudyPartnerRequest::class.java) ?: return@forEach
                        if ((r.senderId == currentUserId && r.receiverId == pid) || (r.receiverId == currentUserId && r.senderId == pid)) {
                            c.ref.removeValue()
                                .addOnSuccessListener { showToast("Removed"); loadPartners() }
                        }
                    }
                }

                override fun onCancelled(e: DatabaseError) {}
            })
    }

    private fun blockPartner(p: Student) {
        val pid = p.id ?: return
        db.child("study_partner_requests")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    s.children.forEach { c ->
                        val r = c.getValue(StudyPartnerRequest::class.java) ?: return@forEach
                        if (isThisRequest(r, pid)) {
                            c.ref.child("status").setValue("blocked")
                            c.ref.child("blockedBy").setValue(currentUserId)
                                .addOnSuccessListener { showToast("Blocked"); loadPartners() }
                        }
                    }
                }

                override fun onCancelled(e: DatabaseError) {}
            })
    }

    private fun unblockPartner(p: Student) {
        val pid = p.id ?: return
        db.child("study_partner_requests")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    s.children.forEach { c ->
                        val r = c.getValue(StudyPartnerRequest::class.java) ?: return@forEach
                        if (isThisRequest(r, pid) && r.blockedBy == currentUserId) {
                            c.ref.child("status").setValue("accepted")
                            c.ref.child("blockedBy").removeValue()
                                .addOnSuccessListener { showToast("Unblocked"); loadPartners() }
                        }
                    }
                }

                override fun onCancelled(e: DatabaseError) {}
            })
    }

    private fun isThisRequest(r: StudyPartnerRequest, pid: String) =
        (r.senderId == currentUserId && r.receiverId == pid) || (r.receiverId == currentUserId && r.senderId == pid)

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
