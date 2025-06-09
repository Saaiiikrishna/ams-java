package com.example.entityadmin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.entityadmin.R
import com.example.entityadmin.model.Subscriber // Import the actual Subscriber model
import android.widget.ImageButton

interface OnSubscriberActionListener {
    fun onEdit(subscriber: Subscriber) // Keep edit for consistency if needed, or remove if only delete
    fun onDelete(subscriberId: String)
}

class SubscriberAdapter(
    private var subscribers: List<Subscriber>, // Use Subscriber model
    private val listener: OnSubscriberActionListener
) : RecyclerView.Adapter<SubscriberAdapter.SubscriberViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubscriberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subscriber, parent, false)
        return SubscriberViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubscriberViewHolder, position: Int) {
        val subscriber = subscribers[position]
        holder.bind(subscriber)
    }

    override fun getItemCount(): Int = subscribers.size

    fun updateData(newSubscribers: List<Subscriber>) { // Use Subscriber model
        this.subscribers = newSubscribers
        notifyDataSetChanged() // Consider using DiffUtil for better performance
    }

    class SubscriberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.textViewSubscriberName)
        private val emailTextView: TextView = itemView.findViewById(R.id.textViewSubscriberEmail)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.buttonDeleteSubscriber)

        fun bind(subscriber: Subscriber, listener: OnSubscriberActionListener) { // Use Subscriber model
            nameTextView.text = subscriber.name
            emailTextView.text = subscriber.email
            // Potentially display subscriber.nfcCardUid if a TextView for it exists in item_subscriber.xml

            deleteButton.setOnClickListener {
                listener.onDelete(subscriber.id)
            }
        }

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val subscriber = subscribers[position]
                    listener.onEdit(subscriber) // Using listener for edit as well now
                }
            }
        }
    }
}
// Required import for findNavController
import androidx.navigation.findNavController
// import android.widget.ImageButton // Already added
