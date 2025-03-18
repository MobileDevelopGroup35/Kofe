package com.l4kt.kofe.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.l4kt.kofe.data.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for handling login and authentication
 */
class LoginViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        // Check if user is already logged in
        viewModelScope.launch {
            userRepository.getCurrentUser()
            userRepository.currentUser.collect { user ->
                _isLoggedIn.value = user != null
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _loginError.value = null

            userRepository.signInWithGoogle(idToken)
                .onSuccess {
                    _isLoggedIn.value = true
                }
                .onFailure { exception ->
                    _loginError.value = exception.message ?: "Authentication failed"
                }

            _isLoading.value = false
        }
    }

    fun resetError() {
        _loginError.value = null
    }
}
