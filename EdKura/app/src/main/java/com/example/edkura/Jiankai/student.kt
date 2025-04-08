package com.example.edkura.Jiankai

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase


class Student {
    var addedClasses = ArrayList<String>()

    fun addCourse(subject: String, course: String) {
        val courseInfo = "$subject $course"
        // 确保添加的课程信息符合预期格式
        if (courseInfo.isNotEmpty()) {
            addedClasses.add(courseInfo)
        }
    }

    fun removeCourseAt(index: Int) {
        if (index >= 0 && index < addedClasses.size) {
            addedClasses.removeAt(index)
        }
    }

    fun getNumberedCourseList(): ArrayList<String> {
        val numberedList = ArrayList<String>()
        for (i in addedClasses.indices) {
            numberedList.add("${i + 1}. ${addedClasses[i]}")
        }
        return numberedList
    }

    fun loadCoursesFromFirebase(onComplete: (() -> Unit)? = null) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.w("Student", "No user is logged in")
            return
        }

        val userId = user.uid
        val databaseRef = FirebaseDatabase.getInstance().reference
            .child("users")
            .child(userId)
            .child("courses")

        databaseRef.get()
            .addOnSuccessListener { snapshot ->
                addedClasses.clear()
                for (courseSnapshot in snapshot.children) {
                    val course = courseSnapshot.getValue(String::class.java)
                    if (course != null) {
                        addedClasses.add(course)
                    }
                }
                onComplete?.invoke()
            }
            .addOnFailureListener { error ->
                Log.e("Student", "Failed to load courses from Firebase: ${error.message}")
            }
    }
}