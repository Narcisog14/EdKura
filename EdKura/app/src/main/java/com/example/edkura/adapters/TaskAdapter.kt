package com.example.edkura.Deadlines

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.R
import com.example.edkura.models.Task
import java.text.SimpleDateFormat
import java.util.Locale

class TaskAdapter(private val tasks: MutableList<Task>) :
    RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {
    var onLongClick: ((Task) -> Unit)? = null


    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val taskNameTextView: TextView = itemView.findViewById(R.id.taskNameTextView)
        val deadlineTextView: TextView = itemView.findViewById(R.id.deadlineTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val currentTask = tasks[position]
        holder.taskNameTextView.text = currentTask.taskName

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val formattedDeadline = currentTask.deadline?.let { dateFormat.format(it) } ?: "N/A"
        holder.deadlineTextView.text = "Deadline: $formattedDeadline"

        // 🔴 Add long-click listener here
        holder.itemView.setOnLongClickListener {
            onLongClick?.invoke(currentTask)
            true
        }
    }


    override fun getItemCount(): Int {
        return tasks.size
    }

    fun updateTasks(newTasks: List<Task>) {
        tasks.clear()
        tasks.addAll(newTasks)
        notifyDataSetChanged()
    }
}

