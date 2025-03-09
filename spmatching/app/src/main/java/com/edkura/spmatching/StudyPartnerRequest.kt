package com.edkura.spmatching

data class StudyPartnerRequest(
    var id: String = "",
    var senderId: String = "",
    var receiverId: String = "",
    var status: String = "pending"
)
