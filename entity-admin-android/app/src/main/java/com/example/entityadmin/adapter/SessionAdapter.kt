package com.example.entityadmin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.entityadmin.R
import com.example.entityadmin.network.Session

class SessionAdapter(
    private val onSessionClick: (Session) -> Unit = {}
) : RecyclerView.Adapter<SessionAdapter.SessionViewHolder>() {

    var sessions: List<Session> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(sessions[position])
    }

    override fun getItemCount(): Int = sessions.size

    inner class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.textSessionName)
        private val idText: TextView = itemView.findViewById(R.id.tvSessionId)
        private val timeText: TextView = itemView.findViewById(R.id.tvSessionTime)
        private val statusText: TextView = itemView.findViewById(R.id.tvSessionStatus)
        private val actionIcon: ImageView = itemView.findViewById(R.id.ivSessionAction)

        fun bind(session: Session) {
            nameText.text = session.name
            idText.text = "ID: ${session.id}"
            timeText.text = "Created recently" // Since we don't have timestamp
            statusText.text = "Active" // Assume all sessions are active for now

            // Set click listeners
            itemView.setOnClickListener {
                onSessionClick(session)
            }

            actionIcon.setOnClickListener {
                onSessionClick(session)
            }
        }
    }
}
