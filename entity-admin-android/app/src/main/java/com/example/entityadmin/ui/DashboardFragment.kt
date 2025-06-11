package com.example.entityadmin.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.entityadmin.R
import com.example.entityadmin.adapter.RecentSessionAdapter
import com.example.entityadmin.network.Session
import com.example.entityadmin.network.SessionRepository
import com.example.entityadmin.network.SubscriberRepository
import com.example.entityadmin.network.TokenManager
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var subscriberRepository: SubscriberRepository

    @Inject
    lateinit var tokenManager: TokenManager

    private lateinit var tvWelcomeMessage: TextView
    private lateinit var tvTotalSubscribers: TextView
    private lateinit var tvTotalSessions: TextView
    private lateinit var tvTodaySessions: TextView
    private lateinit var tvActiveSessions: TextView
    private lateinit var recyclerRecentSessions: RecyclerView
    private lateinit var tvNoRecentSessions: TextView
    private lateinit var btnManageSubscribers: MaterialButton
    private lateinit var btnViewSessions: MaterialButton
    private lateinit var btnGenerateReports: MaterialButton

    private lateinit var recentSessionAdapter: RecentSessionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupClickListeners()
        loadDashboardData()
    }

    private fun initViews(view: View) {
        tvWelcomeMessage = view.findViewById(R.id.tvWelcomeMessage)
        tvTotalSubscribers = view.findViewById(R.id.tvTotalSubscribers)
        tvTotalSessions = view.findViewById(R.id.tvTotalSessions)
        tvTodaySessions = view.findViewById(R.id.tvTodaySessions)
        tvActiveSessions = view.findViewById(R.id.tvActiveSessions)
        recyclerRecentSessions = view.findViewById(R.id.recyclerRecentSessions)
        tvNoRecentSessions = view.findViewById(R.id.tvNoRecentSessions)
        btnManageSubscribers = view.findViewById(R.id.btnManageSubscribers)
        btnViewSessions = view.findViewById(R.id.btnViewSessions)
        btnGenerateReports = view.findViewById(R.id.btnGenerateReports)

        // Setup RecyclerView
        recyclerRecentSessions.layoutManager = LinearLayoutManager(requireContext())
        recentSessionAdapter = RecentSessionAdapter { session ->
            // Navigate to session details
            navigateToSessionDetails(session)
        }
        recyclerRecentSessions.adapter = recentSessionAdapter

        // Set personalized welcome message
        setupWelcomeMessage()

        // Add some nice animations
        setupAnimations()
    }

    private fun setupWelcomeMessage() {
        val entityName = tokenManager.getEntityName() ?: "Entity Admin"
        val userName = tokenManager.getUserName()

        val welcomeMessage = if (userName != null) {
            "Welcome to $entityName!"
        } else {
            "Welcome to $entityName!"
        }

        tvWelcomeMessage.text = welcomeMessage
    }

    private fun setupAnimations() {
        // Add subtle fade-in animation for stats cards
        val fadeIn = android.view.animation.AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
        tvWelcomeMessage.startAnimation(fadeIn)
        tvTotalSubscribers.startAnimation(fadeIn)
        tvTotalSessions.startAnimation(fadeIn)
        tvTodaySessions.startAnimation(fadeIn)
        tvActiveSessions.startAnimation(fadeIn)
    }

    private fun setupClickListeners() {
        btnManageSubscribers.setOnClickListener {
            // Navigate to subscribers tab in bottom navigation
            requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
                ?.selectedItemId = R.id.navigation_subscribers
        }

        btnViewSessions.setOnClickListener {
            // Navigate to sessions tab in bottom navigation
            requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
                ?.selectedItemId = R.id.navigation_sessions
        }

        btnGenerateReports.setOnClickListener {
            // Navigate to reports tab in bottom navigation
            requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
                ?.selectedItemId = R.id.navigation_reports
        }

        // Add click listener for "View All" sessions
        view?.findViewById<TextView>(R.id.tvViewAllSessions)?.setOnClickListener {
            requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
                ?.selectedItemId = R.id.navigation_sessions
        }
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            try {
                // Show loading state with animated counting
                animateCountUp(tvTotalSubscribers, 0)
                animateCountUp(tvTotalSessions, 0)
                animateCountUp(tvTodaySessions, 0)
                animateCountUp(tvActiveSessions, 0)

                // Load subscribers
                val subscribersResult = subscriberRepository.getSubscribers()
                subscribersResult.fold(
                    onSuccess = { subscribers ->
                        animateCountUp(tvTotalSubscribers, subscribers.size)
                    },
                    onFailure = {
                        animateCountUp(tvTotalSubscribers, 0)
                    }
                )

                // Load sessions
                val sessionsResult = sessionRepository.getSessions()
                sessionsResult.fold(
                    onSuccess = { sessions ->
                        animateCountUp(tvTotalSessions, sessions.size)

                        // Calculate today's sessions (sessions created today)
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        val todaySessions = sessions.filter { session ->
                            // For now, assume all sessions are from today since we don't have timestamp
                            // In real implementation, you'd check session creation date
                            true
                        }
                        animateCountUp(tvTodaySessions, todaySessions.size)

                        // For now, assume all sessions are active since we don't have end time
                        animateCountUp(tvActiveSessions, sessions.size)

                        // Show recent sessions (limit to 3 for dashboard)
                        val recentSessions = sessions.take(3)
                        if (recentSessions.isEmpty()) {
                            recyclerRecentSessions.visibility = View.GONE
                            tvNoRecentSessions.visibility = View.VISIBLE
                        } else {
                            recyclerRecentSessions.visibility = View.VISIBLE
                            tvNoRecentSessions.visibility = View.GONE
                            recentSessionAdapter.updateSessions(recentSessions)
                        }
                    },
                    onFailure = {
                        animateCountUp(tvTotalSessions, 0)
                        animateCountUp(tvTodaySessions, 0)
                        animateCountUp(tvActiveSessions, 0)
                        recyclerRecentSessions.visibility = View.GONE
                        tvNoRecentSessions.visibility = View.VISIBLE
                    }
                )

            } catch (e: Exception) {
                // Handle error - set default values with animation
                animateCountUp(tvTotalSubscribers, 0)
                animateCountUp(tvTotalSessions, 0)
                animateCountUp(tvTodaySessions, 0)
                animateCountUp(tvActiveSessions, 0)
                recyclerRecentSessions.visibility = View.GONE
                tvNoRecentSessions.visibility = View.VISIBLE
            }
        }
    }

    private fun animateCountUp(textView: TextView, targetValue: Int) {
        val animator = android.animation.ValueAnimator.ofInt(0, targetValue)
        animator.duration = 1000 // 1 second animation
        animator.addUpdateListener { animation ->
            textView.text = animation.animatedValue.toString()
        }
        animator.start()
    }

    private fun navigateToSessionDetails(session: Session) {
        // Navigate to session details screen
        try {
            requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
                ?.selectedItemId = R.id.navigation_sessions
            // TODO: Pass session ID to sessions screen to show details
        } catch (e: Exception) {
            android.widget.Toast.makeText(requireContext(), "Session: ${session.name}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to dashboard
        loadDashboardData()
    }
}
