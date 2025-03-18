package com.l4kt.kofe.data.services

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.l4kt.kofe.data.models.Cafe
import com.l4kt.kofe.data.models.Match
import com.l4kt.kofe.data.models.User
import kotlinx.coroutines.tasks.await
import java.util.*

/**
 * Service class handling all Firebase operations for the Kofe app
 */
class FirebaseService {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val usersCollection = firestore.collection("users")
    private val matchesCollection = firestore.collection("matches")
    private val cafesCollection = firestore.collection("cafes")

    // Authentication functions
    suspend fun signInWithGoogle(idToken: String): Result<String> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user
                ?: return Result.failure(Exception("Authentication failed"))

            // Check if user profile exists, create if not
            val userDoc = usersCollection.document(user.uid).get().await()
            if (!userDoc.exists()) {
                val newUser = User(
                    uid = user.uid,
                    name = user.displayName ?: "",
                    email = user.email ?: "",
                    bio = "",
                    coffeePreferences = "",
                    photoUrl = user.photoUrl?.toString() ?: "",
                    location = null,
                    lastActive = Date()
                )
                usersCollection.document(user.uid).set(newUser).await()
            } else {
                // Update lastActive
                usersCollection.document(user.uid).update("lastActive", Date()).await()
            }

            Result.success(user.uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    // User functions
    suspend fun getCurrentUser(): Result<User> {
        val uid = getCurrentUserId() ?: return Result.failure(Exception("Not authenticated"))
        return getUserById(uid)
    }

    suspend fun getUserById(userId: String): Result<User> {
        return try {
            val document = usersCollection.document(userId).get().await()
            if (document.exists()) {
                val user = document.toObject(User::class.java)
                    ?: return Result.failure(Exception("Failed to parse user data"))
                Result.success(user)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.uid).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserLocation(userId: String, latitude: Double, longitude: Double): Result<Unit> {
        return try {
            val location = GeoPoint(latitude, longitude)
            usersCollection.document(userId).update(
                mapOf(
                    "location" to location,
                    "lastActive" to Date()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun findNearbyUsers(maxResults: Int = 20): Result<List<User>> {
        val currentUser = getCurrentUser().getOrNull() ?: return Result.failure(Exception("Not authenticated"))

        return try {
            // In MVP we'll just get the most recently active users
            // Future version would use GeoFirestore for actual proximity queries
            val snapshot = usersCollection
                .whereNotEqualTo("uid", currentUser.uid)
                .orderBy("uid")  // Required for inequality filter
                .orderBy("lastActive", Query.Direction.DESCENDING)
                .limit(maxResults.toLong())
                .get()
                .await()

            val users = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Match functions
    suspend fun createMatch(otherUserId: String): Result<String> {
        val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("Not authenticated"))

        return try {
            // Check if match already exists
            val existingMatch = matchesCollection
                .whereArrayContains("users", currentUserId)
                .get()
                .await()
                .documents
                .find { doc ->
                    val match = doc.toObject(Match::class.java)
                    match?.users?.contains(otherUserId) == true
                }

            if (existingMatch != null) {
                return Result.failure(Exception("Match already exists"))
            }

            // Create new match
            val match = Match(
                id = "",  // Will be set to document ID after creation
                users = listOf(currentUserId, otherUserId),
                status = "pending",
                initiatedBy = currentUserId,
                createdAt = Date(),
                cafeId = null,
                meetupDateTime = null,
                notes = ""
            )

            val docRef = matchesCollection.add(match).await()
            matchesCollection.document(docRef.id).update("id", docRef.id).await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserMatches(): Result<List<Match>> {
        val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("Not authenticated"))

        return try {
            val snapshot = matchesCollection
                .whereArrayContains("users", currentUserId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val matches = snapshot.documents.mapNotNull { it.toObject(Match::class.java) }
            Result.success(matches)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMatchById(matchId: String): Result<Match> {
        return try {
            val document = matchesCollection.document(matchId).get().await()
            if (document.exists()) {
                val match = document.toObject(Match::class.java)
                    ?: return Result.failure(Exception("Failed to parse match data"))
                Result.success(match)
            } else {
                Result.failure(Exception("Match not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMatchStatus(matchId: String, status: String): Result<Unit> {
        return try {
            matchesCollection.document(matchId)
                .update("status", status)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMatchDetails(
        matchId: String,
        cafeId: String,
        meetupDateTime: Date,
        notes: String
    ): Result<Unit> {
        return try {
            matchesCollection.document(matchId)
                .update(
                    mapOf(
                        "cafeId" to cafeId,
                        "meetupDateTime" to meetupDateTime,
                        "notes" to notes
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Cafe functions (for caching Google Maps results)
    suspend fun saveCafe(cafe: Cafe): Result<Unit> {
        return try {
            cafesCollection.document(cafe.id).set(cafe).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCafeById(cafeId: String): Result<Cafe> {
        return try {
            val document = cafesCollection.document(cafeId).get().await()
            if (document.exists()) {
                val cafe = document.toObject(Cafe::class.java)
                    ?: return Result.failure(Exception("Failed to parse cafe data"))
                Result.success(cafe)
            } else {
                Result.failure(Exception("Cafe not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}