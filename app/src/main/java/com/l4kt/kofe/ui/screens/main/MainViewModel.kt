package com.l4kt.kofe.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.l4kt.kofe.data.models.Match
import com.l4kt.kofe.data.models.User
import com.l4kt.kofe.data.repositories.MatchRepository
import com.l4kt.kofe.data.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel for the main screen functionality
 */
class MainViewModel(
    private val userRepository: UserRepository,
    private val matchRepository: MatchRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _nearbyUsers = MutableStateFlow<List<User>>(emptyList())
    val nearbyUsers: StateFlow<List<User>> = _nearbyUsers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _userMatches = MutableStateFlow<List<Match>>(emptyList())
    val userMatches: StateFlow<List<Match>> = _userMatches.asStateFlow()

    // Filtered matches
    val pendingMatches: StateFlow<List<Match>> = MutableStateFlow(emptyList())
    val confirmedMatches: StateFlow<List<Match>> = MutableStateFlow(emptyList())

    init {
        viewModelScope.launch {
            // Collect current user
            userRepository.currentUser.collect { user ->
                _currentUser.value = user
            }
        }

        viewModelScope.launch {
            // Collect matches
            matchRepository.matches.collectLatest { matches ->
                _userMatches.value = matches

                // Filter matches by status
                (pendingMatches as MutableStateFlow).value = matches.filter {
                    it.status == "pending"
                }
                (confirmedMatches as MutableStateFlow).value = matches.filter {
                    it.status == "accepted"
                }
            }
        }

        // Load initial data
        loadUserData()
    }

    fun loadUserData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                userRepository.getCurrentUser()
                matchRepository.loadUserMatches()
                findNearbyUsers()
            } catch (e: Exception) {
                _error.value = "Failed to load data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun findNearbyUsers() {
        viewModelScope.launch {
            _isLoading.value = true

            userRepository.findNearbyUsers()
                .onSuccess { users ->
                    _nearbyUsers.value = users
                }
                .onFailure { exception ->
                    _error.value = "Failed to find nearby users: ${exception.message}"
                }

            _isLoading.value = false
        }
    }

    fun updateUserLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            userRepository.updateUserLocation(latitude, longitude)
                .onFailure { exception ->
                    _error.value = "Failed to update location: ${exception.message}"
                }
        }
    }

    fun updateUserProfile(name: String, bio: String, coffeePreferences: String) {
        viewModelScope.launch {
            _isLoading.value = true

            userRepository.updateUserProfile(name, bio, coffeePreferences)
                .onFailure { exception ->
                    _error.value = "Failed to update profile: ${exception.message}"
                }

            _isLoading.value = false
        }
    }

    fun sendCoffeeInvite(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            matchRepository.createMatch(userId)
                .onFailure { exception ->
                    _error.value = "Failed to send invite: ${exception.message}"
                }

            _isLoading.value = false
        }
    }

    fun acceptMatch(matchId: String) {
        viewModelScope.launch {
            matchRepository.acceptMatch(matchId)
                .onFailure { exception ->
                    _error.value = "Failed to accept invite: ${exception.message}"
                }
        }
    }

    fun declineMatch(matchId: String) {
        viewModelScope.launch {
            matchRepository.declineMatch(matchId)
                .onFailure { exception ->
                    _error.value = "Failed to decline invite: ${exception.message}"
                }
        }
    }

    fun signOut() {
        userRepository.signOut()
    }

    fun clearError() {
        _error.value = null
    }
}