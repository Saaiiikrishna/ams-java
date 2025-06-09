package com.example.entityadmin.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.entityadmin.R
import com.example.entityadmin.util.toUserFriendlyMessage // Added
import com.example.entityadmin.viewmodel.CreateSessionViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateSessionFragment : Fragment() {

    private val viewModel: CreateSessionViewModel by viewModels()

    // Assume a ProgressBar with id 'progressBar' is added to fragment_create_session.xml
    private var progressBar: ProgressBar? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_create_session, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionNameEditText = view.findViewById<EditText>(R.id.editTextSessionName)
        val createSessionButton = view.findViewById<Button>(R.id.buttonCreateSession)
        // Initialize progressBar - ensure this ID exists in your layout or add it
        // progressBar = view.findViewById(R.id.progressBar)


        createSessionButton.setOnClickListener {
            val sessionName = sessionNameEditText.text.toString().trim()
            // ViewModel will handle empty check, but can also be done here for immediate feedback
            if (sessionName.isNotEmpty()) {
                viewModel.createSession(sessionName)
            } else {
                Toast.makeText(requireContext(), "Session name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Show/hide progress bar (ensure progressBar is not null and ID is correct)
            // progressBar?.isVisible = isLoading
            createSessionButton.isEnabled = !isLoading // Optionally disable button while loading
        }

        viewModel.sessionCreationResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = { session ->
                    Toast.makeText(requireContext(), "Session '${session.name}' created successfully!", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.action_createSessionFragment_to_sessionListFragment)
                },
                onFailure = { error ->
                    val errorMessage = error.toUserFriendlyMessage() // Use utility
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}
