package com.example.edkura.models

data class User(
    var userId: String = "",
    var name: String = "",
    var courses: MutableList<String> = mutableListOf(), // New field
    var invitedToGroups: MutableMap<String, Boolean> = mutableMapOf()
)