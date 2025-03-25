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
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.example.edkura.chat.ChatActivity

class CourseDetailActivity : AppCompatActivity() {
    private lateinit var db: DatabaseReference
    private lateinit var courseDetailsContainer: LinearLayout
    private lateinit var studyPartnerDashboardContainer: View
    private lateinit var backButton: Button
    private lateinit var studyPartnerButton: Button
    private lateinit var addUserItem: CardView
    private lateinit var studentsRecyclerView: RecyclerView
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.course_detail)

        db = FirebaseDatabase.getInstance().reference

        courseDetailsContainer = findViewById(R.id.courseDetailsContainer)
        studyPartnerDashboardContainer = findViewById(R.id.studyPartnerDashboardContainer)
        backButton = findViewById(R.id.backButton)
        studyPartnerButton = findViewById(R.id.studyPartnerButton)
        addUserItem = findViewById(R.id.addUserItem)
        studentsRecyclerView = findViewById(R.id.studentsRecyclerView)
        studentsRecyclerView.layoutManager = LinearLayoutManager(this)

        studyPartnerButton.setOnClickListener {
            courseDetailsContainer.visibility = View.GONE
            studyPartnerDashboardContainer.visibility = View.VISIBLE
            loadAcceptedStudyPartners()
        }

        addUserItem.setOnClickListener {
            startActivity(Intent(this, spmatching::class.java))
        }

        backButton.setOnClickListener {
            studyPartnerDashboardContainer.visibility = View.GONE
            courseDetailsContainer.visibility = View.VISIBLE
        }
    }

    private fun loadAcceptedStudyPartners() {
        db.child("study_partner_requests")
            .orderByChild("status")
            .equalTo("accepted")
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val partnerList = mutableListOf<Student>()

                    snapshot.children.forEach { child ->
                        val request = child.getValue(StudyPartnerRequest::class.java) ?: return@forEach
                        val partnerId = if(request.senderId == currentUserId) request.receiverId else request.senderId

                        db.child("users").child(partnerId).child("name")
                            .get().addOnSuccessListener { nameSnap ->
                                val partnerName = nameSnap.getValue(String::class.java) ?: "Unknown"
                                partnerList.add(Student(id = partnerId, name = partnerName, course = ""))

                                studentsRecyclerView.adapter = StudentAdapter(partnerList) { partner ->
                                    startActivity(Intent(this@CourseDetailActivity, ChatActivity::class.java).apply {
                                        putExtra("partnerId", partner.id)
                                        putExtra("partnerName", partner.name)
                                    })
                                }
                                studentsRecyclerView.visibility = View.VISIBLE
                            }
                    }

                    if(partnerList.isEmpty()) {
                        studentsRecyclerView.visibility = View.GONE
                    }
                }
                override fun onCancelled(error: DatabaseError) { }
            })
    }

    inner class PartnerAdapter(private val items: List<Pair<String,String>>) :
        RecyclerView.Adapter<PartnerAdapter.Holder>() {

        inner class Holder(view: View): RecyclerView.ViewHolder(view) {
            val nameTv: TextView = view.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(
                LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: Holder, pos: Int) {
            val (name,id)=items[pos]
            holder.nameTv.text = name
            holder.itemView.setOnClickListener {
                startActivity(Intent(this@CourseDetailActivity, ChatActivity::class.java)
                    .putExtra("partnerId", id)
                    .putExtra("partnerName", name))
            }
        }
    }
}
