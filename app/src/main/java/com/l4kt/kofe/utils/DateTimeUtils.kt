package com.l4kt.kofe.utils

import java.text.SimpleDateFormat
import java.util.*

object DateTimeUtils {
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        return "${formatDate(timestamp)} at ${formatTime(timestamp)}"
    }
}