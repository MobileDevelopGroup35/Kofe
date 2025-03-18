package com.l4kt.kofe.utils

import android.content.Context
import android.location.Location
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.l4kt.kofe.data.models.Cafe
import com.l4kt.kofe.data.models.Match
import com.l4kt.kofe.data.models.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Extension functions for the Kofe app
 */

// Context Extensions
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

// String Extensions
fun String.capitalizeWords(): String {
    return this.split(" ").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }
}

fun String.isValidEmail(): Boolean {
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    return this.matches(emailRegex.toRegex())
}

// Date Extensions
fun Date.formatToString(pattern: String = "MMM d, yyyy, h:mm a"): String {
    val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())
    return dateFormat.format(this)
}

fun Date.timeAgo(): String {
    val now = Date()
    val seconds = TimeUnit.MILLISECONDS.toSeconds(now.time - this.time)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(now.time - this.time)
    val hours = TimeUnit.MILLISECONDS.toHours(now.time - this.time)
    val days = TimeUnit.MILLISECONDS.toDays(now.time - this.time)

    return when {
        seconds < 60 -> "just now"
        minutes < 60 -> "$minutes minutes ago"
        hours < 24 -> "$hours hours ago"
        days < 30 -> "$days days ago"
        else -> formatToString("MMM d, yyyy")
    }
}

// Firebase Extensions
fun Timestamp.toDate(): Date = this.toDate()

// GeoPoint Extensions
fun GeoPoint.toLatLng(): LatLng = LatLng(this.latitude, this.longitude)

// Location Extensions
fun Location.toLatLng(): LatLng = LatLng(this.latitude, this.longitude)

fun LatLng.distanceTo(other: LatLng): Float {
    val results = FloatArray(1)
    Location.distanceBetween(
        this.latitude, this.longitude,
        other.latitude, other.longitude,
        results
    )
    return results[0]
}

// Compose Modifier Extensions
fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
    clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() }
    ) {
        onClick()
    }
}

// Model Extensions
fun Cafe.formattedDistance(userLocation: LatLng?): String {
    if (userLocation == null || this.location == null) return "Distance unknown"
    val cafeLatLng = this.location.toLatLng()
    val distanceInMeters = cafeLatLng.distanceTo(userLocation)

    return when {
        distanceInMeters < 1000 -> "${distanceInMeters.toInt()} m"
        else -> "${(distanceInMeters / 1000).toInt()} km"
    }
}

fun Match.isPending(): Boolean = this.status == "pending"
fun Match.isAccepted(): Boolean = this.status == "accepted"
fun Match.isDeclined(): Boolean = this.status == "declined"
fun Match.isCompleted(): Boolean = this.status == "completed"
fun Match.hasCompletedPlan(): Boolean = this.cafeId?.isNotEmpty() == true && this.meetupDateTime != null

fun User.displayName(): String = if (this.name.isNotEmpty()) {
    this.name
} else {
    this.email.substringBefore("@")
}

fun User.hasCompletedProfile(): Boolean =
    this.name.isNotEmpty() &&
            this.bio.isNotEmpty() &&
            this.photoUrl.isNotEmpty() &&
            this.coffeePreferences.isNotEmpty() &&
            this.location != null

fun User.formattedLocation(userLocation: LatLng?): String {
    if (userLocation == null || this.location == null) return "Location unknown"
    val userLatLng = this.location.toLatLng()
    val distanceInMeters = userLatLng.distanceTo(userLocation)

    return when {
        distanceInMeters < 1000 -> "${distanceInMeters.toInt()} m away"
        else -> "${(distanceInMeters / 1000).toInt()} km away"
    }
}

fun User.isActive(): Boolean {
    val now = Date()
    val hoursSinceLastActive = TimeUnit.MILLISECONDS.toHours(now.time - this.lastActive.time)
    return hoursSinceLastActive < 24
}