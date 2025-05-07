package com.example.edkura.models

import java.util.Date

data class Task(
    var taskId: String = "",
    var taskName: String = "",
    var deadline: Date? = null,
) {
    // No-argument constructor (required by Firebase)
    constructor() : this("", "", null)
}