package com.example.edkura.models

import java.util.Date

data class Task(
    val taskId: String = "",
    val taskName: String = "",
    val deadline: Date? = null,
    val groupId: String = ""
)

