package com.l4kt.kofe.data.repositories

import com.google.firebase.firestore.GeoPoint
import com.l4kt.kofe.data.models.User
import com.l4kt.kofe.data.services.FirebaseService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

/**
 * Repository responsible for handling user data
 */
class UserRepository(private val firebaseService: FirebaseService) {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    suspend fun signInWithGoogle(idToken: String): Result<String> {
        val result = firebaseService.signInWithGoogle(idToken)

        if (result.isSuccess) {
            // Update current user
            getCurrentUser()
        }

        return result
    }

    fun signOut() {
        firebaseService.signOut()
        _currentUser.value = null
    }

    suspend fun getCurrentUser() {
        firebaseService.getCurrentUser().onSuccess { user ->
            _currentUser.value = user
        }
    }

    suspend fun getUserById(userId: String): Result<User> {
        return firebaseService.getUserById(userId)
    }

    suspend fun updateUserProfile(
        name: String,
        bio: String,
        coffeePreferences: String,
        photoUrl: String? = null
    ): Result<Unit> {
        val currentUser = _currentUser.value ?: return Result.failure(Exception("User not logged in"))

        val updatedUser = currentUser.copy(
            name = name,
            bio = bio,
            coffeePreferences = coffeePreferences,
            photoUrl = photoUrl ?: currentUser.photoUrl,
            lastActive = Date()
        )

        val result = firebaseService.updateUserProfile(updatedUser)

        if (result.isSuccess) {
            _currentUser.value = updatedUser
        }

        return result
    }

    suspend fun updateUserLocation(latitude: Double, longitude: Double): Result<Unit> {
        val currentUserId = _currentUser.value?.uid ?: return Result.failure(Exception("User not logged in"))

        val result = firebaseService.updateUserLocation(currentUserId, latitude, longitude)

        if (result.isSuccess) {
            // Update local user data with new location
            _currentUser.value = _currentUser.value?.copy(
                location = GeoPoint(latitude, longitude),
                lastActive = Date()
            )
        }

        return result
    }

    suspend fun findNearbyUsers(maxResults: Int = 20): Result<List<User>> {
        return firebaseService.findNearbyUsers(maxResults)
    }
}


