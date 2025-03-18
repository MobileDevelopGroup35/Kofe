package com.l4kt.kofe.ui.screens.cafes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.l4kt.kofe.data.models.Cafe
import com.l4kt.kofe.data.repositories.CafeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for handling cafe data and selection
 */
class CafeViewModel(private val cafeRepository: CafeRepository) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedCafe = MutableStateFlow<Cafe?>(null)
    val selectedCafe: StateFlow<Cafe?> = _selectedCafe.asStateFlow()

    val nearbyCafes = cafeRepository.nearbyCafes

    fun searchNearbyCafes(latitude: Double, longitude: Double, radius: Int = 1500) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            cafeRepository.searchNearbyCafes(latitude, longitude, radius)
                .onFailure { exception ->
                    _error.value = "Failed to find cafes: ${exception.message}"
                }

            _isLoading.value = false
        }
    }

    fun selectCafe(cafe: Cafe) {
        _selectedCafe.value = cafe
    }

    fun getCafeById(cafeId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            cafeRepository.getCafeById(cafeId)
                .onSuccess { cafe ->
                    _selectedCafe.value = cafe
                }
                .onFailure { exception ->
                    _error.value = "Failed to get cafe details: ${exception.message}"
                }

            _isLoading.value = false
        }
    }

    fun clearSelectedCafe() {
        _selectedCafe.value = null
    }

    fun clearError() {
        _error.value = null
    }
}