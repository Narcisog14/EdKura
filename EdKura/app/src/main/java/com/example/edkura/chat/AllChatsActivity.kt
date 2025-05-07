package com.example.edkura.chat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.R
import com.example.edkura.Rao.StudyPartnerRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

/** A small model just for display in the "All Chats" list. */


data class ChatPartner(
    val id: String? = null,
    val name: String? = null,
    val courses: List<String> ? = null,
    val group: String ? =null,
    var groupId: List<String>? = null,
    val groupMembersId: List<String> ? = null
)


class AllChatsActivity : AppCompatActivity() {


    private lateinit var recyclerView: RecyclerView
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val db = FirebaseDatabase.getInstance().reference

    // will hold our final list of unique partners + their courses
    private val partnerList = mutableListOf<ChatPartner>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_chats)
        recyclerView = findViewById(R.id.recyclerViewAllChats)
        recyclerView.layoutManager = LinearLayoutManager(this)
        loadAllPartners()
        loadUserGroups()
    }


    private fun loadAllPartners() {
        // 1) grab all "accepted" study_partner_requests
        db.child("study_partner_requests")
            .orderByChild("status")
            .equalTo("accepted")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // temp map: partnerId -> set of course names
                    val coursesByPartner = mutableMapOf<String, MutableSet<String>>()

                    snapshot.children.forEach { child ->
                        val req = child.getValue(StudyPartnerRequest::class.java) ?: return@forEach
                        // only keep those where I'm sender or receiver
                        val involved = req.senderId == currentUserId || req.receiverId == currentUserId
                        if (!involved) return@forEach

                        val partnerId =
                            if (req.senderId == currentUserId) req.receiverId else req.senderId


                        // collect the course for this partner
                        coursesByPartner
                            .getOrPut(partnerId) { mutableSetOf() }
                            .add(req.course)
                    }

                    // now fetch each partner's name and build ChatPartner list
                    partnerList.clear()
                    coursesByPartner.forEach { (partnerId, coursesSet) ->
                        db.child("users")
                            .child(partnerId)
                            .child("name")
                            .get()
                            .addOnSuccessListener { nameSnap ->
                                val partnerName = nameSnap.getValue(String::class.java) ?: "Unknown"
                                partnerList.add(
                                    ChatPartner(
                                        id = partnerId,
                                        name = partnerName,
                                        courses = coursesSet.toList().sorted()
                                    )
                                )
                                // refresh adapter as we load each one
                                recyclerView.adapter = GlobalChatAdapter(partnerList)
                            }
                    }
                }


                override fun onCancelled(error: DatabaseError) {
                    // you might log or toast here
                }
            })
    }
    private fun loadUserGroups() {
        db.child("projectGroups")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (groupSnapshot in snapshot.children) {
                        val groupId = groupSnapshot.key ?: continue // get groupId
                        val membersSnapshot = groupSnapshot.child("members")
                        val groupName = groupSnapshot.child("name").getValue(String::class.java) ?: "Unknown Group"
                        val groupMembersId = mutableListOf<String>()

                        for (member in membersSnapshot.children) {
                            val memberId = member.key
                            if (memberId != null) {
                                groupMembersId.add(memberId)
                            }
                        }

                        if (membersSnapshot.hasChild(currentUserId)) {
                            partnerList.add(
                                ChatPartner(
                                    groupId = listOf(groupId),
                                    group = groupName,
                                    groupMembersId = groupMembersId
                                )
                            )
                        }
                    }
                    recyclerView.adapter?.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@AllChatsActivity,
                        "Failed to load groups",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }



    inner class GlobalChatAdapter(private val items: List<ChatPartner>) :
        RecyclerView.Adapter<GlobalChatAdapter.Holder>() {


        inner class Holder(view: ViewGroup) : RecyclerView.ViewHolder(view) {
            val nameBtn: Button = view.findViewById(R.id.buttonPartnerName)
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = layoutInflater.inflate(R.layout.item_partner, parent, false) as ViewGroup
            return Holder(view)
        }


        override fun getItemCount() = items.size


        override fun onBindViewHolder(holder: Holder, position: Int) {
            val partner = items[position]
            val coursesText = partner.courses?.joinToString(", ")
            holder.nameBtn.text = "${partner.name} ($coursesText)"
            if (partner.name == null) {
                holder.nameBtn.text = "${partner.group}"
            }
            holder.nameBtn.setOnClickListener {
                startActivity(
                    Intent(this@AllChatsActivity, ChatActivity::class.java).apply {
                        putExtra("partnerId", partner.id)
                        putExtra("partnerName", partner.name)
                        putExtra("group", partner.group)
                        putStringArrayListExtra(
                            "groupId",
                            ArrayList(partner.groupId ?: emptyList())
                        )
                        Log.d("groupId", "groupId: ${partner.groupId}")
                        putStringArrayListExtra(
                            "groupMembersId",
                            ArrayList(partner.groupMembersId ?: emptyList())
                        )
                    }
                )
            }
        }
    }
}
