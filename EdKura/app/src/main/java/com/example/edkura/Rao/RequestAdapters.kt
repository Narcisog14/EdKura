package com.example.edkura.Rao

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.R
import com.example.edkura.Rao.StudyPartnerRequest


class RequestsAdapter(
    private var requests: List<StudyPartnerRequest>,
    private val listener: OnRequestActionListener
) : RecyclerView.Adapter<RequestsAdapter.RequestViewHolder>() {

    interface OnRequestActionListener {
        fun onAccept(request: StudyPartnerRequest)
        fun onDecline(request: StudyPartnerRequest)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.br_item_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val request = requests[position]

        holder.senderTextView.text = "From: ${request.senderName}"

        holder.acceptButton.setOnClickListener {
            listener.onAccept(request)
        }

        holder.declineButton.setOnClickListener {
            listener.onDecline(request)
        }

    }

    override fun getItemCount(): Int = requests.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateRequests(newRequests: List<StudyPartnerRequest>) {
        requests = newRequests
        notifyDataSetChanged()
    }

    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderTextView: TextView = itemView.findViewById(R.id.senderTextView)
        val acceptButton: Button = itemView.findViewById(R.id.acceptButton)
        val declineButton: Button = itemView.findViewById(R.id.declineButton)


        @SuppressLint("SetTextI18n")
        fun bind(request: StudyPartnerRequest) {
            senderTextView.text = "From: ${request.senderId}"
            acceptButton.setOnClickListener {
                listener.onAccept(request)
            }
            declineButton.setOnClickListener {
                listener.onDecline(request)
            }
        }
    }
}