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

@AndroidEntryPoint
class LoginFragment : Fragment() {

    @Inject lateinit var authRepository: AuthRepository
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
                        Toast.makeText(requireContext(), R.string.login_success, Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_loginFragment_to_sessionListFragment)
                    },
                    onFailure = { exception ->
                        val errorMessage = exception.toUserFriendlyMessage()
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }
}
