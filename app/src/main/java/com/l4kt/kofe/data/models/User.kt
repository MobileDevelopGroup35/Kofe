package com.l4kt.kofe.data.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint
import java.util.*

data class User(
    @DocumentId
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val bio: String = "",
    val coffeePreferences: String = "",
    val photoUrl: String = "",
    val location: GeoPoint? = null,
    val lastActive: Date = Date(),
    val fcmToken: String = ""
)