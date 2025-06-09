package com.example.entityadmin.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.entityadmin.model.Subscriber
import com.example.entityadmin.model.SubscriberRequest
import com.example.entityadmin.network.SubscriberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditSubscriberViewModel @Inject constructor(
    private val subscriberRepository: SubscriberRepository
) : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _saveResult = MutableLiveData<Result<Subscriber>>()
    val saveResult: LiveData<Result<Subscriber>> = _saveResult

    private val _loadedSubscriber = MutableLiveData<Subscriber?>()
    val loadedSubscriber: LiveData<Subscriber?> = _loadedSubscriber

    private var currentSubscriberId: String? = null

    fun loadSubscriberDetails(subscriberId: String) {
        currentSubscriberId = subscriberId
        viewModelScope.launch {
            _isLoading.value = true
            val result = subscriberRepository.getSubscriberById(subscriberId)
            result.onSuccess {
                _loadedSubscriber.value = it
            }.onFailure {
                // Post error to a new LiveData or reuse _saveResult for simplicity if appropriate
                _saveResult.value = Result.failure(it) // Or a dedicated error LiveData
            }
            _isLoading.value = false
        }
    }

    fun saveSubscriber(name: String, email: String, nfcCardUidInput: String?) {
        val id = currentSubscriberId
        if (id != null) {
            updateSubscriber(id, name, email, nfcCardUidInput)
        } else {
            createSubscriber(name, email, nfcCardUidInput)
        }
    }

    private fun createSubscriber(name: String, email: String, nfcCardUidInput: String?) {
        if (name.isBlank() || email.isBlank()) {
            _saveResult.value = Result.failure(IllegalArgumentException("Name and Email cannot be empty."))
            return
        }
        // Basic email validation
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _saveResult.value = Result.failure(IllegalArgumentException("Invalid email format."))
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            // Ensure nfcCardUid is null if blank, otherwise pass the value
            val nfcCardUid = nfcCardUidInput?.takeIf { it.isNotBlank() }
            val request = SubscriberRequest(name, email, nfcCardUid)
            val result = subscriberRepository.createSubscriber(request)
            _saveResult.value = result
            _isLoading.value = false
        }
    }

    private fun updateSubscriber(subscriberId: String, name: String, email: String, nfcCardUidInput: String?) {
        if (name.isBlank() || email.isBlank()) {
            _saveResult.value = Result.failure(IllegalArgumentException("Name and Email cannot be empty."))
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _saveResult.value = Result.failure(IllegalArgumentException("Invalid email format."))
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val nfcCardUid = nfcCardUidInput?.takeIf { it.isNotBlank() }
            val request = SubscriberRequest(name, email, nfcCardUid)
            val result = subscriberRepository.updateSubscriber(subscriberId, request)
            _saveResult.value = result
            _isLoading.value = false
        }
    }
}
