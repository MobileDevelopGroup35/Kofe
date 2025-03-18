package com.l4kt.kofe.data.repositories

import com.l4kt.kofe.data.models.Match
import com.l4kt.kofe.data.models.User
import com.l4kt.kofe.data.services.FirebaseService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

/**
 * Repository responsible for handling match data
 */
class MatchRepository(
    private val firebaseService: FirebaseService,
    private val userRepository: UserRepository
) {
    private val _matches = MutableStateFlow<List<Match>>(emptyList())
    val matches: StateFlow<List<Match>> = _matches.asStateFlow()

    suspend fun loadUserMatches() {
        firebaseService.getUserMatches().onSuccess { matchList ->
            _matches.value = matchList
        }
    }

    suspend fun createMatch(otherUserId: String): Result<String> {
        val result = firebaseService.createMatch(otherUserId)

        if (result.isSuccess) {
            // Refresh matches list
            loadUserMatches()
        }

        return result
    }

    suspend fun getMatchWithUserDetails(matchId: String): Result<Pair<Match, List<User>>> {
        val matchResult = firebaseService.getMatchById(matchId)

        return if (matchResult.isSuccess) {
            val match = matchResult.getOrThrow()
            val userResults = match.users.map { userId ->
                userRepository.getUserById(userId)
            }

            // Check if all user results were successful
            val allSuccessful = userResults.all { it.isSuccess }

            if (allSuccessful) {
                val users = userResults.map { it.getOrThrow() }
                Result.success(Pair(match, users))
            } else {
                Result.failure(Exception("Failed to fetch user details"))
            }
        } else {
            // Get the original error and propagate it with the correct type
            Result.failure(matchResult.exceptionOrNull() ?: Exception("Unknown error fetching match"))
        }
    }

    suspend fun acceptMatch(matchId: String): Result<Unit> {
        val result = firebaseService.updateMatchStatus(matchId, "accepted")

        if (result.isSuccess) {
            // Update local state
            val updatedMatches = _matches.value.map { match ->
                if (match.id == matchId) match.copy(status = "accepted") else match
            }
            _matches.value = updatedMatches
        }

        return result
    }

    suspend fun declineMatch(matchId: String): Result<Unit> {
        val result = firebaseService.updateMatchStatus(matchId, "declined")

        if (result.isSuccess) {
            // Update local state
            val updatedMatches = _matches.value.map { match ->
                if (match.id == matchId) match.copy(status = "declined") else match
            }
            _matches.value = updatedMatches
        }

        return result
    }

    suspend fun completeMeetupPlan(
        matchId: String,
        cafeId: String,
        meetupDateTime: Date,
        notes: String
    ): Result<Unit> {
        val result = firebaseService.updateMatchDetails(
            matchId,
            cafeId,
            meetupDateTime,
            notes
        )

        if (result.isSuccess) {
            // Update local state
            val updatedMatches = _matches.value.map { match ->
                if (match.id == matchId) {
                    match.copy(
                        cafeId = cafeId,
                        meetupDateTime = meetupDateTime,
                        notes = notes
                    )
                } else {
                    match
                }
            }
            _matches.value = updatedMatches
        }

        return result
    }
}