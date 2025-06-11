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
import androidx.navigation.fragment.navArgs
import com.example.entityadmin.R
import com.example.entityadmin.model.Subscriber
import com.example.entityadmin.util.toUserFriendlyMessage // Added
import com.example.entityadmin.viewmodel.AddEditSubscriberViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddEditSubscriberFragment : Fragment() {

    private val viewModel: AddEditSubscriberViewModel by viewModels()
    private val args: AddEditSubscriberFragmentArgs by navArgs()

    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var nfcCardUidEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_edit_subscriber, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nameEditText = view.findViewById(R.id.editTextSubscriberName)
        emailEditText = view.findViewById(R.id.editTextSubscriberEmail)
        nfcCardUidEditText = view.findViewById(R.id.editTextNfcCardUid)
        saveButton = view.findViewById(R.id.buttonSaveSubscriber)
        progressBar = view.findViewById(R.id.progressBarAddEditSubscriber)

        // Set title based on passed argument (nav_graph label handles initial)
        // (activity as AppCompatActivity?)?.supportActionBar?.title = args.title

        args.subscriberId?.let {
            viewModel.loadSubscriberDetails(it)
            saveButton.text = "Update Subscriber"
            // Title could be set more dynamically if the label "{title}" isn't enough
        } ?: run {
            saveButton.text = "Create Subscriber"
        }


        saveButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val nfcCardUid = nfcCardUidEditText.text.toString().trim()

            viewModel.saveSubscriber(name, email, nfcCardUid) // Changed from createSubscriber
        }

        observeViewModel()
    }

    private fun populateFields(subscriber: Subscriber) {
        nameEditText.setText(subscriber.name ?: "")
        emailEditText.setText(subscriber.email ?: "")
        nfcCardUidEditText.setText(subscriber.nfcCardUid ?: "")
    }

    private fun observeViewModel() {
        viewModel.loadedSubscriber.observe(viewLifecycleOwner) { subscriber ->
            subscriber?.let { populateFields(it) }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.isVisible = isLoading
            saveButton.isEnabled = !isLoading
            nameEditText.isEnabled = !isLoading
            emailEditText.isEnabled = !isLoading
            nfcCardUidEditText.isEnabled = !isLoading
        }

        viewModel.saveResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = { subscriber ->
                    Toast.makeText(requireContext(), "Subscriber '${subscriber.name}' saved successfully!", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.action_addEditSubscriberFragment_to_subscriberListFragment)
                },
                onFailure = { error ->
                    val errorMessage = error.toUserFriendlyMessage() // Use utility
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}
