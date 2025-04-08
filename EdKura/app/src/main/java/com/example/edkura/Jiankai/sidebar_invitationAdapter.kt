package com.example.edkura.Jiankai

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.R
import com.example.edkura.Rao.StudyPartnerRequest

class CustomRequestsAdapter(
    private var requests: List<StudyPartnerRequest>,
    private val listener: OnRequestActionListener
) : RecyclerView.Adapter<CustomRequestsAdapter.RequestViewHolder>() {

    // ViewHolder Class
    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val requestTextView: TextView = itemView.findViewById(R.id.requestTextView)
        val acceptButton: Button = itemView.findViewById(R.id.acceptButton)
        val declineButton: Button = itemView.findViewById(R.id.declineButton)

        init {
            acceptButton.setOnClickListener {
                listener.onAccept(requests[adapterPosition])  // Trigger onAccept when clicked
            }
            declineButton.setOnClickListener {
                listener.onDecline(requests[adapterPosition])  // Trigger onDecline when clicked
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
        val request = requests[position]
        // Modify the text to be in the desired format "BR (Accept) (Decline)"
        holder.requestTextView.text = "From: ${request.senderName}"
    }

    // Get the total number of items
    override fun getItemCount(): Int = requests.size

    // Update the list of requests and notify the adapter
    @SuppressLint("NotifyDataSetChanged")
    fun updateRequests(newRequests: List<StudyPartnerRequest>) {
        requests = newRequests
        notifyDataSetChanged()
    }

    // Interface for accepting and declining requests
    interface OnRequestActionListener {
        fun onAccept(request: StudyPartnerRequest)
        fun onDecline(request: StudyPartnerRequest)
    }
}