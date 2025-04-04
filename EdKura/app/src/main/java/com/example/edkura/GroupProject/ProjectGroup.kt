package com.example.edkura.models

data class ProjectGroup(
    var groupId: String = "",
    var name: String = "",
    var description: String = "",
    var course: String = "", // New field
    var members: MutableMap<String, Boolean> = mutableMapOf(),
    var invitedUsers: MutableMap<String, Boolean> = mutableMapOf()
)