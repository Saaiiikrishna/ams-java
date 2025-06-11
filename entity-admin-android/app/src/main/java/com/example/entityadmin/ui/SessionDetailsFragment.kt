package com.example.entityadmin.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.entityadmin.R
import com.example.entityadmin.adapter.AttendeeAdapter
import com.example.entityadmin.model.Attendee
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SessionDetailsFragment : Fragment() {

    private val args: SessionDetailsFragmentArgs by navArgs()

    private lateinit var tvSessionName: TextView
    private lateinit var tvSessionId: TextView
    private lateinit var tvTotalAttendees: TextView
    private lateinit var tvSessionDate: TextView
    private lateinit var recyclerAttendees: RecyclerView
    private lateinit var tvNoAttendees: TextView

    private lateinit var attendeeAdapter: AttendeeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_session_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        loadSessionDetails()
    }

    private fun initViews(view: View) {
        tvSessionName = view.findViewById(R.id.tvSessionName)
        tvSessionId = view.findViewById(R.id.tvSessionId)
        tvTotalAttendees = view.findViewById(R.id.tvTotalAttendees)
        tvSessionDate = view.findViewById(R.id.tvSessionDate)
        recyclerAttendees = view.findViewById(R.id.recyclerAttendees)
        tvNoAttendees = view.findViewById(R.id.tvNoAttendees)
    }

    private fun setupRecyclerView() {
        recyclerAttendees.layoutManager = LinearLayoutManager(requireContext())
        attendeeAdapter = AttendeeAdapter { attendee ->
            Toast.makeText(requireContext(), "Attendee: ${attendee.name}", Toast.LENGTH_SHORT).show()
        }
        recyclerAttendees.adapter = attendeeAdapter
    }

    private fun loadSessionDetails() {
        // Set session info
        tvSessionName.text = "Session ${args.sessionId}"
        tvSessionId.text = "Session ID: ${args.sessionId}"
        tvSessionDate.text = getCurrentDate()

        // Load mock attendee data for now
        loadMockAttendees()
    }

    private fun loadMockAttendees() {
        val mockAttendees = listOf(
            Attendee("1", "John Doe", "john.doe@example.com", "09:15 AM"),
            Attendee("2", "Jane Smith", "jane.smith@example.com", "09:22 AM"),
            Attendee("3", "Mike Johnson", "mike.johnson@example.com", "09:30 AM"),
            Attendee("4", "Sarah Wilson", "sarah.wilson@example.com", "09:45 AM"),
            Attendee("5", "David Brown", "david.brown@example.com", "10:00 AM")
        )

        tvTotalAttendees.text = "${mockAttendees.size} Attendees"

        if (mockAttendees.isEmpty()) {
            recyclerAttendees.visibility = View.GONE
            tvNoAttendees.visibility = View.VISIBLE
        } else {
            recyclerAttendees.visibility = View.VISIBLE
            tvNoAttendees.visibility = View.GONE
            attendeeAdapter.updateAttendees(mockAttendees)
        }
    }

    private fun getCurrentDate(): String {
        val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        return dateFormat.format(java.util.Date())
    }
}
