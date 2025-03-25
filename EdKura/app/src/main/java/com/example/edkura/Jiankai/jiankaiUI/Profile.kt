package com.example.edkura.Jiankai.jiankaiUI

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.edkura.R
import com.example.edkura.Rao.StudyPartnerRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class RequestReceiverActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userId = "user2"

        // 初始化Firebase Realtime Database
        database = FirebaseDatabase.getInstance().reference
        // 监听接收到的请求
        listenForStudyPartnerRequests()
    }

    private fun listenForStudyPartnerRequests() {
        // 监听 study_partner_requests 节点中的变化，查看是否有新的请求发给当前用户
        database.child("study_partner_requests").orderByChild("receiverId").equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val requests = mutableListOf<StudyPartnerRequest>()
                    snapshot.children.forEach { child ->
                        val request = child.getValue(StudyPartnerRequest::class.java)
                        if (request != null && request.status == "pending") {
                            request.id = child.key ?: ""
                            requests.add(request)
                        }
                    }
                    if (requests.isNotEmpty()) {
                        // 当检测到请求时，发送信号（例如通知或者更新UI）
                        notifyUserAboutRequest(requests)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@RequestReceiverActivity, "Error fetching requests: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun notifyUserAboutRequest(requests: List<StudyPartnerRequest>) {

        Toast.makeText(this, "You have new requests!", Toast.LENGTH_SHORT).show()

    }
}