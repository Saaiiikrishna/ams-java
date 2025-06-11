package com.example.entityadmin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.entityadmin.R
import com.example.entityadmin.model.Subscriber // Import the actual Subscriber model
import android.widget.ImageButton
import androidx.navigation.findNavController

interface OnSubscriberActionListener {
    fun onEdit(subscriber: Subscriber) // Keep edit for consistency if needed, or remove if only delete
    fun onDelete(subscriberId: String)
}

class SubscriberAdapter(
    private var subscribers: List<Subscriber>, // Use Subscriber model
    private val listener: OnSubscriberActionListener
) : RecyclerView.Adapter<SubscriberAdapter.SubscriberViewHolder>() {

    private var filteredSubscribers: List<Subscriber> = subscribers

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubscriberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subscriber, parent, false)
        return SubscriberViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubscriberViewHolder, position: Int) {
        try {
            if (position < filteredSubscribers.size) {
                val subscriber = filteredSubscribers[position]
                holder.bind(subscriber, listener)
            }
        } catch (e: Exception) {
            // Log error but don't crash the app
            e.printStackTrace()
        }
    }

    override fun getItemCount(): Int = filteredSubscribers.size

    fun updateData(newSubscribers: List<Subscriber>?) { // Use Subscriber model, allow null
        try {
            // Safely handle null input
            val safeSubscribers = newSubscribers ?: emptyList()
            this.subscribers = safeSubscribers
            this.filteredSubscribers = safeSubscribers
            notifyDataSetChanged() // Consider using DiffUtil for better performance
        } catch (e: Exception) {
            // Log error but don't crash
            e.printStackTrace()
            // Set empty lists as fallback
            this.subscribers = emptyList()
            this.filteredSubscribers = emptyList()
            notifyDataSetChanged()
        }
    }

    fun filter(query: String) {
        filteredSubscribers = if (query.isEmpty()) {
            subscribers
        } else {
            subscribers.filter {
                (it.name?.contains(query, ignoreCase = true) == true) ||
                (it.email?.contains(query, ignoreCase = true) == true)
            }
        }
        notifyDataSetChanged()
    }

    class SubscriberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.textViewSubscriberName)
        private val emailTextView: TextView = itemView.findViewById(R.id.textViewSubscriberEmail)
        private val initialsTextView: TextView = itemView.findViewById(R.id.tvSubscriberInitials)
        private val statusTextView: TextView = itemView.findViewById(R.id.tvSubscriberStatus)
        private val idTextView: TextView = itemView.findViewById(R.id.tvSubscriberId)
        private val editButton: ImageButton = itemView.findViewById(R.id.buttonEditSubscriber)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.buttonDeleteSubscriber)

        fun bind(subscriber: Subscriber, listener: OnSubscriberActionListener) { // Use Subscriber model
            // Handle null values safely
            nameTextView.text = subscriber.name ?: "Unknown Name"
            emailTextView.text = subscriber.email ?: "No Email"
            idTextView.text = "ID: ${subscriber.id}"
            statusTextView.text = "Active" // For now, assume all are active

            // Generate initials from name - handle null safely
            val initials = if (!subscriber.name.isNullOrBlank()) {
                subscriber.name.split(" ")
                    .take(2)
                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                    .joinToString("")
            } else {
                ""
            }
            initialsTextView.text = initials.ifEmpty { "?" }

            editButton.setOnClickListener {
                listener.onEdit(subscriber)
            }

            deleteButton.setOnClickListener {
                listener.onDelete(subscriber.id)
            }

            itemView.setOnClickListener {
                listener.onEdit(subscriber) // Using listener for edit as well now
            }
        }
    }
}
