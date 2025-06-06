package com.example.entityadmin

import android.os.Bundle
import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.entityadmin.network.AuthRepository
import com.example.entityadmin.SessionListActivity

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    @Inject lateinit var authRepository: AuthRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailInput = findViewById<EditText>(R.id.editEmail)
        val passwordInput = findViewById<EditText>(R.id.editPassword)
        val loginButton = findViewById<Button>(R.id.buttonLogin)

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
                    Toast.makeText(this@LoginActivity, R.string.login_success, Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, SessionListActivity::class.java))
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, R.string.login_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
