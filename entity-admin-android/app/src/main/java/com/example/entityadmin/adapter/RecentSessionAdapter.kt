package com.example.entityadmin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.entityadmin.R
import com.example.entityadmin.network.Session

class RecentSessionAdapter(
    private var sessions: List<Session> = emptyList(),
    private val onSessionClick: (Session) -> Unit
) : RecyclerView.Adapter<RecentSessionAdapter.RecentSessionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentSessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_session, parent, false)
        return RecentSessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecentSessionViewHolder, position: Int) {
        val session = sessions[position]
        holder.bind(session, onSessionClick)
    }

    override fun getItemCount(): Int = sessions.size

    fun updateSessions(newSessions: List<Session>) {
        this.sessions = newSessions
        notifyDataSetChanged()
    }

    class RecentSessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSessionName: TextView = itemView.findViewById(R.id.tvSessionName)
        private val tvSessionId: TextView = itemView.findViewById(R.id.tvSessionId)
        private val tvSessionStatus: TextView = itemView.findViewById(R.id.tvSessionStatus)

        fun bind(session: Session, onSessionClick: (Session) -> Unit) {
            tvSessionName.text = session.name
            tvSessionId.text = "ID: ${session.id}"
            tvSessionStatus.text = "Active" // For now, assume all are active
            
            itemView.setOnClickListener {
                onSessionClick(session)
            }
        }
    }
}
