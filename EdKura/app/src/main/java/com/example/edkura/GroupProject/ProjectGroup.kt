package com.example.edkura.models

data class ProjectGroup(
    val groupId: String = "",
    val name: String = "",
    val description: String = "",
    val course: String = "",
    var members: MutableMap<String, Boolean> = mutableMapOf(),
    var invitedUsers: MutableMap<String, Boolean> = mutableMapOf(),
    val creator: String = ""
)