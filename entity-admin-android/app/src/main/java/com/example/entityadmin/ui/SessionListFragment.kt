package com.example.entityadmin.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.recyclerview.widget.RecyclerView
import com.example.entityadmin.R
import com.example.entityadmin.adapter.SessionAdapter
import com.example.entityadmin.viewmodel.SessionViewModel
import com.example.entityadmin.network.TokenManager // Added
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject // Added
import androidx.navigation.NavOptions // Added
import androidx.navigation.fragment.findNavController
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.isVisible

@AndroidEntryPoint
class SessionListFragment : Fragment() {

    private val viewModel: SessionViewModel by viewModels()

    @Inject // Added
    lateinit var tokenManager: TokenManager // Added

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // Enable options menu
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_session_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerSessions)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBarSessions)
        val emptyState = view.findViewById<View>(R.id.layoutEmptyState)
        val tvActiveCount = view.findViewById<android.widget.TextView>(R.id.tvActiveSessionsCount)
        val tvTotalCount = view.findViewById<android.widget.TextView>(R.id.tvTotalSessionsCount)

        recycler.layoutManager = LinearLayoutManager(requireContext())
        val adapter = SessionAdapter { session ->
            // Navigate to session details
            try {
                val action = SessionListFragmentDirections.actionSessionsToSessionDetails(session.id.toInt())
                findNavController().navigate(action)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Session: ${session.name} (ID: ${session.id})", Toast.LENGTH_SHORT).show()
            }
        }
        recycler.adapter = adapter

        view.findViewById<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton>(R.id.fabAddSession)
            .setOnClickListener {
                try {
                    findNavController().navigate(R.id.action_sessions_to_createSession)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Create session feature coming soon!", Toast.LENGTH_SHORT).show()
                }
            }

        // Observe LiveData from SessionViewModel
        viewModel.sessionsLiveData.observe(viewLifecycleOwner) { sessionList ->
            adapter.sessions = sessionList
            adapter.notifyDataSetChanged()

            // Update stats
            tvTotalCount.text = sessionList.size.toString()
            tvActiveCount.text = sessionList.size.toString() // For now, assume all are active

            // Show/hide empty state
            if (sessionList.isEmpty()) {
                recycler.isVisible = false
                emptyState.isVisible = true
            } else {
                recycler.isVisible = true
                emptyState.isVisible = false
            }
        }

        viewModel.isLoadingLiveData.observe(viewLifecycleOwner) { isLoading ->
            progressBar.isVisible = isLoading
            if (isLoading) {
                recycler.isVisible = false
                emptyState.isVisible = false
            }
        }

        viewModel.errorLiveData.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                // Show empty state on error
                recycler.isVisible = false
                emptyState.isVisible = true
                progressBar.isVisible = false
            }
        }

        viewModel.loadSessions() // Initial call to load sessions
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_session_list, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_manage_subscribers -> {
                try {
                    // Navigate to subscribers tab in bottom navigation
                    requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
                        ?.selectedItemId = R.id.navigation_subscribers
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.action_logout -> { // Added
                try {
                    tokenManager.clearToken()
                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(R.id.loginFragment, true) // Pop up to loginFragment inclusive
                        .build()
                    findNavController().navigate(R.id.loginFragment, null, navOptions)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Logout error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
