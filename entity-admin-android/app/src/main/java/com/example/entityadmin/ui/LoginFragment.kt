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
import com.example.entityadmin.viewmodel.SessionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.entityadmin.network.AuthRepository

@AndroidEntryPoint
class LoginFragment : Fragment() {

    @Inject lateinit var authRepository: AuthRepository
    private val viewModel: SessionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val emailInput = view.findViewById<EditText>(R.id.editEmail)
        val passwordInput = view.findViewById<EditText>(R.id.editPassword)
        val loginButton = view.findViewById<Button>(R.id.buttonLogin)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (email.isEmpty()) {
                emailInput.error = getString(R.string.error_email_required)
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                passwordInput.error = getString(R.string.error_password_required)
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    authRepository.login(email, password)
                    Toast.makeText(requireContext(), R.string.login_success, Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_loginFragment_to_sessionListFragment)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), R.string.login_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
