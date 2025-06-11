package com.example.entityadmin.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.entityadmin.R
import com.example.entityadmin.util.toUserFriendlyMessage // Added
import com.example.entityadmin.viewmodel.SessionViewModel // This ViewModel is not used for login logic here
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.entityadmin.network.AuthRepository
import com.example.entityadmin.network.TokenManager

@AndroidEntryPoint
class LoginFragment : Fragment() {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var tokenManager: TokenManager
    // private val viewModel: SessionViewModel by viewModels() // Not used for login action

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val usernameInput = view.findViewById<EditText>(R.id.editUsername)
        val passwordInput = view.findViewById<EditText>(R.id.editPassword)
        val loginButton = view.findViewById<Button>(R.id.buttonLogin)

        loginButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (username.isEmpty()) {
                usernameInput.error = getString(R.string.error_username_required)
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                passwordInput.error = getString(R.string.error_password_required)
                return@setOnClickListener
            }

            lifecycleScope.launch {
                // Disable button, show progress bar here if they were added to layout
                val loginResult = authRepository.login(username, password)
                // Re-enable button, hide progress bar here

                loginResult.fold(
                    onSuccess = {
                        // Store user information for personalized messages
                        tokenManager.saveUserName(username)

                        // For now, we'll extract entity name from username or use a default
                        // In a real app, this would come from the API response
                        val entityName = extractEntityName(username)
                        tokenManager.saveEntityName(entityName)

                        // Show personalized success message
                        val welcomeMessage = "Welcome to $entityName! Login successful."
                        Toast.makeText(requireContext(), welcomeMessage, Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_loginFragment_to_mainContainer)
                    },
                    onFailure = { exception ->
                        val errorMessage = exception.toUserFriendlyMessage()
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    private fun extractEntityName(username: String): String {
        // Extract entity name from username
        // Common patterns: admin@company.com, company_admin, admin_company
        return when {
            username.contains("@") -> {
                val domain = username.substringAfter("@").substringBefore(".")
                when (domain.lowercase()) {
                    "gmail", "yahoo", "hotmail", "outlook" -> "Entity Admin"
                    else -> domain.replaceFirstChar { it.uppercase() } + " Organization"
                }
            }
            username.contains("_") -> {
                val parts = username.split("_")
                if (parts.size > 1 && parts[0] == "admin") {
                    parts[1].replaceFirstChar { it.uppercase() } + " Corp"
                } else {
                    parts[0].replaceFirstChar { it.uppercase() } + " Company"
                }
            }
            username.startsWith("admin") -> {
                val suffix = username.removePrefix("admin")
                if (suffix.isNotEmpty()) {
                    suffix.replaceFirstChar { it.uppercase() } + " Enterprise"
                } else {
                    "Entity Admin"
                }
            }
            else -> {
                // Use the username as company name
                username.replaceFirstChar { it.uppercase() } + " Organization"
            }
        }
    }
}
