package com.example.edkura.Narciso

import android.view.View
import android.widget.Button
import android.widget.LinearLayout

class StudyPartner(
    private val courseDetailsContainer: LinearLayout,
    private val studyPartnerDashboardContainer: LinearLayout,
    private val backButton: Button
) {

    init {
        backButton.setOnClickListener {
            showCourseDetails()
        }
    }

    fun showStudyPartnerDashboard() {
        courseDetailsContainer.visibility = View.GONE
        studyPartnerDashboardContainer.visibility = View.VISIBLE
    }

    private fun showCourseDetails() {
        courseDetailsContainer.visibility = View.VISIBLE
        studyPartnerDashboardContainer.visibility = View.GONE
    }
}