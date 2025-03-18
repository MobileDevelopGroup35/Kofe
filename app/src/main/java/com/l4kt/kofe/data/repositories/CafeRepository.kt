package com.l4kt.kofe.data.repositories

import com.l4kt.kofe.data.models.Cafe
import com.l4kt.kofe.data.services.FirebaseService
import com.l4kt.kofe.data.services.GoogleMapsService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository responsible for cafe data
 */
class CafeRepository(
    private val firebaseService: FirebaseService,
    private val googleMapsService: GoogleMapsService
) {
    private val _nearbyCafes = MutableStateFlow<List<Cafe>>(emptyList())
    val nearbyCafes: StateFlow<List<Cafe>> = _nearbyCafes.asStateFlow()

    suspend fun searchNearbyCafes(latitude: Double, longitude: Double, radius: Int = 1500): Result<List<Cafe>> {
        val result = googleMapsService.searchNearbyCafes(latitude, longitude, radius)

        return if (result.isSuccess) {
            val cafes = result.getOrThrow()
            _nearbyCafes.value = cafes

            // Cache cafes in Firestore for future reference
            cafes.forEach { cafe ->
                firebaseService.saveCafe(cafe)
            }

            Result.success(cafes)
        } else {
            result
        }
    }

    suspend fun getCafeById(cafeId: String): Result<Cafe> {
        // First check in-memory cache
        val cachedCafe = _nearbyCafes.value.find { it.id == cafeId }
        if (cachedCafe != null) {
            return Result.success(cachedCafe)
        }

        // Otherwise, fetch from Firestore
        return firebaseService.getCafeById(cafeId)
    }
}