package com.l4kt.kofe.data.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint


data class Cafe(
    @DocumentId
    val id: String = "",  // Generated ID for Firestore
    val placeId: String = "",  // Google Maps Place ID
    val name: String = "",
    val address: String = "",
    val location: GeoPoint? = null,
    val photoUrl: String = "",
    val rating: Double = 0.0,
    val priceLevel: Int = 0  // 0-4, where 0 = Free, 4 = Very Expensive
)