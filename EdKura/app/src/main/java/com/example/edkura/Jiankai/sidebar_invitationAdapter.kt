package com.example.edkura.Jiankai

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.GroupProject.GroupInvite
import com.example.edkura.R
import com.example.edkura.Rao.StudyPartnerRequest

class CustomRequestsAdapter(
    private val listener: OnRequestActionListener
) : RecyclerView.Adapter<CustomRequestsAdapter.RequestViewHolder>() {

    // Define a sealed class to represent different types of items
    sealed class RequestItem {
        data class StudyPartnerItem(val request: StudyPartnerRequest) : RequestItem()
        data class GroupInviteItem(val invite: GroupInvite) : RequestItem()
    }

    // Combined list of all request items
    private var items: List<RequestItem> = emptyList()

    // ViewHolder Class
    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val requestTextView: TextView = itemView.findViewById(R.id.requestTextView)
        val acceptButton: Button = itemView.findViewById(R.id.acceptButton)
        val declineButton: Button = itemView.findViewById(R.id.declineButton)

        init {
            acceptButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    when (val item = items[position]) {
                        is RequestItem.StudyPartnerItem -> listener.onAccept(item.request)
                        is RequestItem.GroupInviteItem -> listener.onGroupAccept(item.invite)
                    }
                }
            }

            declineButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    when (val item = items[position]) {
                        is RequestItem.StudyPartnerItem -> listener.onDecline(item.request)
                        is RequestItem.GroupInviteItem -> listener.onGroupDecline(item.invite)
                    }
                }
            }
        }
    }

    // Create ViewHolder and bind the custom layout
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.sidebar_request_adapter, parent, false)
        return RequestViewHolder(view)
    }

    // Bind data to the ViewHolder
    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        when (val item = items[position]) {
            is RequestItem.StudyPartnerItem -> {
                val request = item.request
                holder.requestTextView.text = "From: ${request.senderName} ( ${request.course})"
            }
            is RequestItem.GroupInviteItem -> {
                val GroupInvite = item.invite
                holder.requestTextView.text = "Group: ${GroupInvite.GroupName}"
                Log.d("groupName2", "Group Name: ${GroupInvite.GroupName}")
            }
        }
    }

    // Get the total number of items
    override fun getItemCount(): Int = items.size

    // Update both lists and combine them into items list
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(studyRequests: List<StudyPartnerRequest>, groupInvites: List<GroupInvite>) {
        // Create a new combined list
        val newItems = mutableListOf<RequestItem>()

        // Add all study partner requests
        studyRequests.forEach { request ->
            newItems.add(RequestItem.StudyPartnerItem(request))
        }

        // Add all group invites
        groupInvites.forEach { invite ->
            newItems.add(RequestItem.GroupInviteItem(invite))
        }

        // Update the items list
        items = newItems
        notifyDataSetChanged()
    }

    // Interface for handling actions
    interface OnRequestActionListener {
        fun onAccept(request: StudyPartnerRequest)
        fun onGroupAccept(invite: GroupInvite)
        fun onDecline(request: StudyPartnerRequest)
        fun onGroupDecline(invite: GroupInvite)
    }
}