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
import android.app.AlertDialog
import com.example.entityadmin.R
import com.example.entityadmin.adapter.SubscriberAdapter
import com.example.entityadmin.adapter.OnSubscriberActionListener
// import com.example.entityadmin.adapter.SubscriberPlaceholder // No longer needed
import com.example.entityadmin.model.Subscriber // Import actual model
import com.example.entityadmin.util.toUserFriendlyMessage // Added
import com.example.entityadmin.viewmodel.SubscriberListViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
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

        recyclerView = view.findViewById(R.id.recyclerViewSubscribers)
        progressBar = view.findViewById(R.id.progressBarSubscribers)
        setupRecyclerView()

        val fabAddSubscriber: FloatingActionButton = view.findViewById(R.id.fabAddSubscriber)
        fabAddSubscriber.setOnClickListener {
            // Navigate for adding (no subscriberId, default title "Add Subscriber" from nav_graph)
            val action = SubscriberListFragmentDirections.actionSubscriberListFragmentToAddEditSubscriberFragment()
            findNavController().navigate(action)
        }

        observeViewModel()
        viewModel.fetchSubscribers()
    }

    private fun setupRecyclerView() {
        subscriberAdapter = SubscriberAdapter(emptyList(), this) // Pass 'this' as listener
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = subscriberAdapter
        }
    }

    override fun onEdit(subscriber: Subscriber) {
        val action = SubscriberListFragmentDirections.actionSubscriberListFragmentToAddEditSubscriberFragment(
            subscriberId = subscriber.id,
            title = "Edit Subscriber"
        )
        findNavController().navigate(action)
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
        viewModel.subscribers.observe(viewLifecycleOwner) { subscribers ->
            subscriberAdapter.updateData(subscribers)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.isVisible = isLoading
            // Only hide recyclerView if loading AND there's no error shown
            // Or if it's initial load. Subsequent loads (like after delete) might want to keep showing stale data.
            if (isLoading && (viewModel.subscribers.value == null || viewModel.subscribers.value!!.isEmpty())) {
                 recyclerView.isVisible = false
            } else if (!isLoading) {
                 recyclerView.isVisible = true
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(requireContext(), "Error loading subscribers: $it", Toast.LENGTH_LONG).show()
                // Consider showing error view or allowing retry
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
