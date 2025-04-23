package com.example.edkura.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.GroupProject.GroupProjectDashboardActivity
import com.example.edkura.R

class GroupInviteAdapter(private val invites: List<GroupProjectDashboardActivity.GroupInvite>, private val listener: OnInviteActionListener) :
    RecyclerView.Adapter<GroupInviteAdapter.InviteViewHolder>() {

    interface OnInviteActionListener {
        fun onAcceptClicked(invite: GroupProjectDashboardActivity.GroupInvite)
        fun onDeclineClicked(invite: GroupProjectDashboardActivity.GroupInvite)
    }

    class InviteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val groupNameTextView: TextView = view.findViewById(R.id.groupNameTextView)
        val invitedByTextView: TextView = view.findViewById(R.id.invitedByTextView)
        val acceptButton: Button = view.findViewById(R.id.acceptButton)
        val declineButton: Button = view.findViewById(R.id.declineButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InviteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.dialog_invite_group, parent, false)
        return InviteViewHolder(view)
    }

    override fun onBindViewHolder(holder: InviteViewHolder, position: Int) {
        val invite = invites[position]
        holder.groupNameTextView.text = invite.groupName
        holder.invitedByTextView.text = "Invited by: ${invite.invitedByUserName}"
        holder.acceptButton.setOnClickListener {
            listener.onAcceptClicked(invite)
        }
        holder.declineButton.setOnClickListener {
            listener.onDeclineClicked(invite)
        }
    }

    override fun getItemCount(): Int = invites.size
}