package com.l4kt.kofe.data.services


import android.location.Location
import com.google.firebase.firestore.GeoPoint
import com.l4kt.kofe.data.models.Cafe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

/**
 * Service for handling Google Maps API requests
 */
class GoogleMapsService(private val apiKey: String) {
    companion object {
        private const val PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place"
        private const val NEARBY_SEARCH = "/nearbysearch/json?"
        private const val PLACE_DETAILS = "/details/json?"
    }

    /**
     * Search for cafes near a location
     */
    suspend fun searchNearbyCafes(
        latitude: Double,
        longitude: Double,
        radius: Int = 1500 // Default 1.5km radius
    ): Result<List<Cafe>> = withContext(Dispatchers.IO) {
        try {
            val sb = StringBuilder(PLACES_API_BASE + NEARBY_SEARCH)
            sb.append("location=$latitude,$longitude")
            sb.append("&radius=$radius")
            sb.append("&types=cafe")   // Filters for places categorized as cafes
            sb.append("&keyword=coffee")  // Further refines results to places related to coffee
            sb.append("&key=$apiKey")

            val url = URL(sb.toString())
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val input = BufferedReader(InputStreamReader(connection.inputStream))
            val response = StringBuilder()
            var line: String?
            while (input.readLine().also { line = it } != null) {
                response.append(line)
            }
            input.close()

            val jsonObject = JSONObject(response.toString())
            val status = jsonObject.getString("status")

            if (status == "OK") {
                val results = jsonObject.getJSONArray("results")
                val cafes = mutableListOf<Cafe>()

                for (i in 0 until results.length()) {
                    val place = results.getJSONObject(i)

                    val id = UUID.randomUUID().toString() // Generate a unique ID for Firestore
                    val placeId = place.getString("place_id")
                    val name = place.getString("name")
                    val address = place.getString("vicinity")

                    // Get location
                    val locationObj = place.getJSONObject("geometry").getJSONObject("location")
                    val cafeLat = locationObj.getDouble("lat")
                    val cafeLng = locationObj.getDouble("lng")
                    val location = GeoPoint(cafeLat, cafeLng)

                    // Get rating if available
                    val rating = if (place.has("rating")) place.getDouble("rating") else 0.0

                    // Get price level if available (0 to 4)
                    val priceLevel = if (place.has("price_level")) place.getInt("price_level") else 0

                    // Get photo reference if available
                    var photoUrl = ""
                    if (place.has("photos")) {
                        val photos = place.getJSONArray("photos")
                        if (photos.length() > 0) {
                            val photoReference = photos.getJSONObject(0).getString("photo_reference")
                            photoUrl = "https://maps.googleapis.com/maps/api/place/photo" +
                                    "?maxwidth=400" +
                                    "&photo_reference=$photoReference" +
                                    "&key=$apiKey"
                        }
                    }

                    val cafe = Cafe(
                        id = id,
                        placeId = placeId,
                        name = name,
                        address = address,
                        location = location,
                        photoUrl = photoUrl,
                        rating = rating,
                        priceLevel = priceLevel
                    )

                    cafes.add(cafe)
                }

                Result.success(cafes)
            } else {
                Result.failure(Exception("API error: $status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calculate distance between two locations in meters
     */
    fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }
}