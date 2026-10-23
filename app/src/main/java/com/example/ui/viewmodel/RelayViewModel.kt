package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.RelayRepository
import com.example.data.repository.SocialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RelayViewModel(application: Application) : AndroidViewModel(application) {
    private val socialRepository = SocialRepository(application)
    private val relayRepository = RelayRepository { socialRepository.currentUser.value }

    private val _isRelayLoading = MutableStateFlow(false)
    val isRelayLoading: StateFlow<Boolean> = _isRelayLoading.asStateFlow()

    fun toggleRelay(postId: String, quote: String?, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isRelayLoading.value = true
            relayRepository.toggleRelay(postId, quote)
                .onSuccess {
                    onDone(true)
                }
                .onFailure {
                    onDone(false)
                }
            _isRelayLoading.value = false
        }
    }
}
