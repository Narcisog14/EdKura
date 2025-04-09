package com.example.edkura.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.R
import com.example.edkura.models.User

class UserSearchAdapter(
    private var users: List<User>,
    private val selectedUsers: MutableList<User>
) : RecyclerView.Adapter<UserSearchAdapter.UserViewHolder>() {

    // ViewHolder class
    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userNameTextView: TextView = itemView.findViewById(R.id.userNameTextView)
        val userCheckBox: CheckBox = itemView.findViewById(R.id.userCheckBox)
    }

    // Called when a new ViewHolder needs to be created
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(itemView)
    }

    // Called to bind data to a ViewHolder
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.userNameTextView.text = user.name
        holder.userCheckBox.isChecked = selectedUsers.contains(user)
        holder.userCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedUsers.add(user)
            } else {
                selectedUsers.remove(user)
            }
        }
    }

    // Returns the number of items in the list
    override fun getItemCount() = users.size

    // Method to update the list of users
    fun updateData(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }
}