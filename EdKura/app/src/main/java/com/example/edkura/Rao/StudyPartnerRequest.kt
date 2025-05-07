package com.example.edkura.Rao

data class StudyPartnerRequest(
    var id: String = "",
    var senderId: String = "",
    var receiverId: String = "",
    var status: String = "pending",
    var senderName: String = "",
    var blockedBy: String? = null,
    var course: String = "",

)

