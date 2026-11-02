package com.animex.app.core.comment

import kotlinx.serialization.Serializable

@Serializable
data class AnimeComment(
    val id: String,
    val animeId: Long,
    val username: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val likes: Int = 0,
    val isLikedByMe: Boolean = false,
    val avatarColor: Long = 0xFF00F0FF // Cyan or Magenta
) {
    val formattedTime: String
        get() {
            val diffMs = System.currentTimeMillis() - timestamp
            val mins = diffMs / (60 * 1000)
            val hours = mins / 60
            val days = hours / 24
            return when {
                mins < 1 -> "Just now"
                mins < 60 -> "${mins}m ago"
                hours < 24 -> "${hours}h ago"
                days < 7 -> "${days}d ago"
                else -> "${days / 7}w ago"
            }
        }
}
