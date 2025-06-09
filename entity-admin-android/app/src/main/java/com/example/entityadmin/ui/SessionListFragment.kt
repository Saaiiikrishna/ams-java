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
        recycler.layoutManager = LinearLayoutManager(requireContext())
        val adapter = SessionAdapter()
        recycler.adapter = adapter

        view.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddSession)
            .setOnClickListener {
                findNavController().navigate(R.id.action_sessionListFragment_to_createSessionFragment)
            }

        // Observe LiveData from SessionViewModel
        viewModel.sessionsLiveData.observe(viewLifecycleOwner) { sessionList ->
            adapter.sessions = sessionList
            adapter.notifyDataSetChanged()
        }
        viewModel.isLoadingLiveData.observe(viewLifecycleOwner) { isLoading ->
            // Assuming a progress bar with id progressBarSessions exists in fragment_session_list.xml
            view.findViewById<ProgressBar>(R.id.progressBarSessions)?.isVisible = isLoading
            recycler.isVisible = !isLoading
        }
        viewModel.errorLiveData.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
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
                findNavController().navigate(R.id.action_sessionListFragment_to_subscriberListFragment)
                true
            }
            R.id.action_logout -> { // Added
                tokenManager.clearToken()
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.loginFragment, true) // Pop up to loginFragment inclusive
                    .build()
                findNavController().navigate(R.id.loginFragment, null, navOptions)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
// Ensure imports for findNavController and FloatingActionButton if they are missing
import androidx.navigation.fragment.findNavController
// import com.google.android.material.floatingactionbutton.FloatingActionButton // Already imported via view.findViewById type
