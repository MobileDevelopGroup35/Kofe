package com.l4kt.kofe.data.models

import com.google.firebase.firestore.DocumentId
import java.util.*

data class Match(
    @DocumentId
    val id: String = "",
    val users: List<String> = emptyList(),  // Always contains exactly 2 user IDs
    val status: String = "",  // "pending", "accepted", "declined", "completed"
    val initiatedBy: String = "",  // User ID who initiated the match
    val createdAt: Date = Date(),
    val cafeId: String? = null,  // Selected cafe ID (null until confirmed)
    val meetupDateTime: Date? = null,  // Planned meetup time (null until confirmed)
    val notes: String = ""
)