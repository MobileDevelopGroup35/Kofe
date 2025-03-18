package com.l4kt.kofe.ui.screens.meetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.l4kt.kofe.data.models.Cafe
import com.l4kt.kofe.data.models.Match
import com.l4kt.kofe.data.models.User
import com.l4kt.kofe.data.repositories.CafeRepository
import com.l4kt.kofe.data.repositories.MatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

/**
 * ViewModel for meetup coordination
 */
class MeetupViewModel(
    private val matchRepository: MatchRepository,
    private val cafeRepository: CafeRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _match = MutableStateFlow<Match?>(null)
    val match: StateFlow<Match?> = _match.asStateFlow()

    private val _matchUsers = MutableStateFlow<List<User>>(emptyList())
    val matchUsers: StateFlow<List<User>> = _matchUsers.asStateFlow()

    private val _selectedCafe = MutableStateFlow<Cafe?>(null)
    val selectedCafe: StateFlow<Cafe?> = _selectedCafe.asStateFlow()

    private val _selectedDate = MutableStateFlow<Date?>(null)
    val selectedDate: StateFlow<Date?> = _selectedDate.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    val nearbyCafes = cafeRepository.nearbyCafes

    fun loadMatchDetails(matchId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            matchRepository.getMatchWithUserDetails(matchId)
                .onSuccess { (match, users) ->
                    _match.value = match
                    _matchUsers.value = users

                    // If match already has a cafe selected, load it
                    match.cafeId?.let { cafeId ->
                        loadCafeDetails(cafeId)
                    }

                    // If match already has a date selected, load it
                    _selectedDate.value = match.meetupDateTime

                    // If match has notes, load them
                    _notes.value = match.notes
                }
                .onFailure { exception ->
                    _error.value = "Failed to load match details: ${exception.message}"
                }

            _isLoading.value = false
        }
    }

    fun loadCafeDetails(cafeId: String) {
        viewModelScope.launch {
            cafeRepository.getCafeById(cafeId)
                .onSuccess { cafe ->
                    _selectedCafe.value = cafe
                }
                .onFailure { exception ->
                    _error.value = "Failed to load cafe details: ${exception.message}"
                }
        }
    }

    fun searchNearbyCafes(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _isLoading.value = true

            cafeRepository.searchNearbyCafes(latitude, longitude)
                .onFailure { exception ->
                    _error.value = "Failed to find cafes: ${exception.message}"
                }

            _isLoading.value = false
        }
    }

    fun selectCafe(cafe: Cafe) {
        _selectedCafe.value = cafe
    }

    fun selectDate(date: Date) {
        _selectedDate.value = date
    }

    fun updateNotes(notes: String) {
        _notes.value = notes
    }

    fun confirmMeetup() {
        val currentMatch = _match.value ?: return
        val selectedCafe = _selectedCafe.value ?: return
        val selectedDate = _selectedDate.value ?: return

        viewModelScope.launch {
            _isLoading.value = true

            matchRepository.completeMeetupPlan(
                currentMatch.id,
                selectedCafe.id,
                selectedDate,
                _notes.value
            ).onFailure { exception ->
                _error.value = "Failed to confirm meetup: ${exception.message}"
            }

            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}

