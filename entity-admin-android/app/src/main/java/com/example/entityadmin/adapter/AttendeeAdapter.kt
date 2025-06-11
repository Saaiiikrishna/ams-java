package com.example.entityadmin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.entityadmin.R
import com.example.entityadmin.model.Attendee

class AttendeeAdapter(
    private var attendees: List<Attendee> = emptyList(),
    private val onAttendeeClick: (Attendee) -> Unit
) : RecyclerView.Adapter<AttendeeAdapter.AttendeeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendeeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendee, parent, false)
        return AttendeeViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttendeeViewHolder, position: Int) {
        val attendee = attendees[position]
        holder.bind(attendee, onAttendeeClick)
    }

    override fun getItemCount(): Int = attendees.size

    fun updateAttendees(newAttendees: List<Attendee>) {
        this.attendees = newAttendees
        notifyDataSetChanged()
    }

    class AttendeeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAttendeeName: TextView = itemView.findViewById(R.id.tvAttendeeName)
        private val tvAttendeeEmail: TextView = itemView.findViewById(R.id.tvAttendeeEmail)
        private val tvCheckInTime: TextView = itemView.findViewById(R.id.tvCheckInTime)
        private val tvAttendeeStatus: TextView = itemView.findViewById(R.id.tvAttendeeStatus)
        private val tvAttendeeInitials: TextView = itemView.findViewById(R.id.tvAttendeeInitials)

        fun bind(attendee: Attendee, onAttendeeClick: (Attendee) -> Unit) {
            tvAttendeeName.text = attendee.name
            tvAttendeeEmail.text = attendee.email
            tvCheckInTime.text = "Check-in: ${attendee.checkInTime}"
            tvAttendeeStatus.text = attendee.status
            
            // Generate initials
            val initials = attendee.name.split(" ")
                .take(2)
                .map { it.firstOrNull()?.uppercaseChar() ?: "" }
                .joinToString("")
            tvAttendeeInitials.text = initials.ifEmpty { "?" }
            
            itemView.setOnClickListener {
                onAttendeeClick(attendee)
            }
        }
    }
}
