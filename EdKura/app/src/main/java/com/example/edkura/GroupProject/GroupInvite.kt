package com.example.edkura.GroupProject

data class GroupInvite(

    var groupId: String = "",
    var inviteId: String = "",
    var invitedBy: String = "",
    var invitedByName: String = "",
    var GroupName: String = "",
    var inviteeId: String = "",
    var inviteeName: String = "",
    var senderName: String = "",
    var status:String = "pending",
)
