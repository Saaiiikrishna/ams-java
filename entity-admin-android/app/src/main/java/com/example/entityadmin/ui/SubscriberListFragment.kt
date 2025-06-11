package com.example.entityadmin.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AlertDialog
import com.example.entityadmin.R
import com.example.entityadmin.adapter.SubscriberAdapter
import com.example.entityadmin.adapter.OnSubscriberActionListener
// import com.example.entityadmin.adapter.SubscriberPlaceholder // No longer needed
import com.example.entityadmin.model.Subscriber // Import actual model
import com.example.entityadmin.util.toUserFriendlyMessage // Added
import com.example.entityadmin.viewmodel.SubscriberListViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.navigation.fragment.findNavController

@AndroidEntryPoint
class SubscriberListFragment : Fragment(), OnSubscriberActionListener {

    private lateinit var subscriberAdapter: SubscriberAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private val viewModel: SubscriberListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_subscriber_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            recyclerView = view.findViewById(R.id.recyclerViewSubscribers)
            progressBar = view.findViewById(R.id.progressBarSubscribers)

            // Setup RecyclerView first
            setupRecyclerView()

            val fabAddSubscriber: com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton? = view.findViewById(R.id.fabAddSubscriber)
            fabAddSubscriber?.setOnClickListener {
                // Navigate for adding (no subscriberId, default title "Add Subscriber" from nav_graph)
                try {
                    findNavController().navigate(R.id.action_subscribers_to_addEditSubscriber)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            // Setup search functionality - only if adapter is initialized
            val searchEditText = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSearchSubscribers)
            searchEditText?.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    if (::subscriberAdapter.isInitialized) {
                        subscriberAdapter.filter(s.toString())
                    }
                }
            })

            observeViewModel()

            // Add a delay and retry mechanism for permission issues
            try {
                viewModel.fetchSubscribers()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading subscribers. Retrying...", Toast.LENGTH_SHORT).show()
                // Retry after a short delay
                view.postDelayed({
                    try {
                        viewModel.fetchSubscribers()
                    } catch (retryException: Exception) {
                        Toast.makeText(requireContext(), "Unable to load subscribers: ${retryException.message}", Toast.LENGTH_LONG).show()
                    }
                }, 2000)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error initializing subscriber screen: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun setupRecyclerView() {
        subscriberAdapter = SubscriberAdapter(emptyList(), this) // Pass 'this' as listener
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = subscriberAdapter
        }
    }

    override fun onEdit(subscriber: Subscriber) {
        try {
            // For now, just show a toast since the edit functionality needs to be implemented
            Toast.makeText(requireContext(), "Edit subscriber: ${subscriber.name}", Toast.LENGTH_SHORT).show()
            // TODO: Implement proper navigation to edit screen
            // findNavController().navigate(R.id.action_subscribers_to_addEditSubscriber)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Edit error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDelete(subscriberId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Subscriber")
            .setMessage("Are you sure you want to delete this subscriber?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteSubscriber(subscriberId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModel() {
        val emptyState = view?.findViewById<View>(R.id.layoutEmptyState)
        val tvTotalCount = view?.findViewById<android.widget.TextView>(R.id.tvTotalSubscribersCount)
        val tvActiveCount = view?.findViewById<android.widget.TextView>(R.id.tvActiveSubscribersCount)

        viewModel.subscribers.observe(viewLifecycleOwner) { subscribers ->
            try {
                // Safely handle null or empty subscribers list
                val safeSubscribers = subscribers ?: emptyList()
                subscriberAdapter.updateData(safeSubscribers)

                // Update stats safely
                tvTotalCount?.text = safeSubscribers.size.toString()
                tvActiveCount?.text = safeSubscribers.size.toString() // For now, assume all are active

                // Show/hide empty state
                if (safeSubscribers.isEmpty()) {
                    recyclerView.isVisible = false
                    emptyState?.isVisible = true
                } else {
                    recyclerView.isVisible = true
                    emptyState?.isVisible = false
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error updating subscriber list: ${e.message}", Toast.LENGTH_SHORT).show()
                recyclerView.isVisible = false
                emptyState?.isVisible = true
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            try {
                progressBar.isVisible = isLoading
                if (isLoading) {
                    recyclerView.isVisible = false
                    emptyState?.isVisible = false
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error updating loading state: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            try {
                errorMsg?.let {
                    // Handle specific error messages for better user experience
                    val userFriendlyMessage = when {
                        it.contains("403") || it.contains("Access denied") ->
                            "You don't have permission to view subscribers. Please contact your administrator."
                        it.contains("401") || it.contains("Authentication failed") ->
                            "Your session has expired. Please login again."
                        it.contains("404") || it.contains("not found") ->
                            "Subscribers service is not available. Please try again later."
                        it.contains("500") || it.contains("Server error") ->
                            "Server is experiencing issues. Please try again later."
                        else -> "Error loading subscribers: $it"
                    }

                    Toast.makeText(requireContext(), userFriendlyMessage, Toast.LENGTH_LONG).show()

                    // Show empty state on error
                    recyclerView.isVisible = false
                    emptyState?.isVisible = true
                    progressBar.isVisible = false
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error handling error message: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.deleteResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = {
                    Toast.makeText(requireContext(), "Subscriber deleted successfully", Toast.LENGTH_SHORT).show()
                    // List is refreshed by ViewModel after successful delete
                },
                onFailure = { error ->
                    val errorMessage = error.toUserFriendlyMessage() // Use utility
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}
