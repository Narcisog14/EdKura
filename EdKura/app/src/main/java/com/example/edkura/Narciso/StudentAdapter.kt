package com.example.edkura.Narciso

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.R

class StudentAdapter(
    private val students: List<Student>,
    private val onClick: (Student) -> Unit,
    private val onLongClick: (Student) -> Unit
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val studentNameTextView: TextView = itemView.findViewById(R.id.studentNameTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.student_item, parent, false)
        return StudentViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]
        holder.studentNameTextView.text = student.name
        holder.itemView.setOnClickListener { onClick(student) }
        holder.itemView.setOnLongClickListener {
            onLongClick(student)
            true
        }
    }

    override fun getItemCount() = students.size
}