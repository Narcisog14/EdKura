package com.example.edkura.Narciso

data class Student(
    val id: String? = null,
    val name: String? = null,
    //val course: String? = null,
    val status: String? = null,
    val blockedBy: String? = null,
    val courses: String = ""
)
