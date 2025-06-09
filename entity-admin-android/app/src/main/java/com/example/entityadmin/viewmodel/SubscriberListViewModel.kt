package com.example.entityadmin.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.entityadmin.model.Subscriber
import com.example.entityadmin.network.SubscriberRepository
import com.example.entityadmin.util.toUserFriendlyMessage // Added
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriberListViewModel @Inject constructor(
    private val subscriberRepository: SubscriberRepository
) : ViewModel() {

    private val _subscribers = MutableLiveData<List<Subscriber>>()
    val subscribers: LiveData<List<Subscriber>> = _subscribers

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _deleteResult = MutableLiveData<Result<Unit>>()
    val deleteResult: LiveData<Result<Unit>> = _deleteResult

    fun fetchSubscribers() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = subscriberRepository.getSubscribers()
            result.onSuccess {
                _subscribers.value = it
            }.onFailure {
                _error.value = it.toUserFriendlyMessage() // Use utility
            }
            _isLoading.value = false
        }
    }

    fun deleteSubscriber(subscriberId: String) {
        viewModelScope.launch {
            // Optionally set another LiveData for delete-specific loading/progress
            // _isLoading.value = true // Or a specific delete loading flag
            val result = subscriberRepository.deleteSubscriber(subscriberId)
            _deleteResult.value = result // Post result to new LiveData
            // _isLoading.value = false

            // If successful, refresh the list
            if (result.isSuccess) {
                fetchSubscribers() // Or manually remove from _subscribers list
            }
            // Failure case can be observed via deleteResult by the fragment for specific error messages
        }
    }
}
