package com.example.edkura.Jiankai

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class Course(val subject: String, val course: String)
class Student(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("StudentPrefs", Context.MODE_PRIVATE)

    // 存储课程的 ArrayList
    val addedClasses = ArrayList<Course>()

    init {
        loadCourses() // 初始化时加载数据
    }
    fun addCourse(subject: String, course: String) {
        val newCourse = Course(subject, course)
        addedClasses.add(newCourse)
        saveCourses()
    }
    fun getNumberedCourseList(): ArrayList<String> {
        val numberedList = ArrayList<String>()
        for (i in addedClasses.indices) {
            val course = addedClasses[i]
            numberedList.add("${i + 1}. ${course.subject} - ${course.course}")
        }
        return numberedList
    }

    // 🧹 清空课程
    fun clearCourses() {
        addedClasses.clear()
        saveCourses()
    }

    // 💾 保存课程
    fun saveCourses() {
        val jsonArray = JSONArray()
        for (course in addedClasses) {
            val jsonObject = JSONObject()
            jsonObject.put("subject", course.subject)
            jsonObject.put("course", course.course)
            jsonArray.put(jsonObject)
        }
        sharedPreferences.edit().putString("courses", jsonArray.toString()).apply()
    }
    fun removeCourseAt(index: Int) {
        if (index >= 0 && index < addedClasses.size) {
            addedClasses.removeAt(index)
            saveCourses() // 重新保存更改
        }
    }

    // 📥 加载课程
    fun loadCourses() {
        addedClasses.clear()
        val jsonString = sharedPreferences.getString("courses", "[]")
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val subject = jsonObject.getString("subject")
            val course = jsonObject.getString("course")
            addedClasses.add(Course(subject, course))
        }
    }
}