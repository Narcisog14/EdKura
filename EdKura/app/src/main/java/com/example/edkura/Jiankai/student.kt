package com.example.edkura.Jiankai

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

class Student(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("StudentPrefs", Context.MODE_PRIVATE)
    val addedClasses = ArrayList<String>()

    init {
        loadCourses()
    }

    // Add course without numbering in stored data
    fun addCourse(subject: String, course: String) {
        addedClasses.add("$subject $course")
        saveCourses()
    }

    // Remove course by index
    fun removeCourseAt(index: Int) {
        if (index >= 0 && index < addedClasses.size) {
            addedClasses.removeAt(index)
            saveCourses()
        }
    }

    // Remove course by content
    fun removeCourse(course: String) {
        addedClasses.remove(course)
        saveCourses()
    }

    // Get formatted course list for display
    fun getNumberedCourseList(): ArrayList<String> {
        val numberedList = ArrayList<String>()
        for (i in addedClasses.indices) {
            numberedList.add("${i + 1}. ${addedClasses[i]}")
        }
        return numberedList
    }

    fun clearCourses() {
        addedClasses.clear()
        saveCourses()
    }

    fun saveCourses() {
        val jsonArray = JSONArray(addedClasses)
        sharedPreferences.edit().putString("courses", jsonArray.toString()).apply()
    }

    fun loadCourses() {
        addedClasses.clear()
        val jsonString = sharedPreferences.getString("courses", "[]")
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            addedClasses.add(jsonArray.getString(i))
        }
    }
}