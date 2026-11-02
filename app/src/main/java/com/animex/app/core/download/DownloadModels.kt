package com.animex.app.core.download

import kotlinx.serialization.Serializable

enum class DownloadDestination {
    EXTERNAL, // Saved to Android Downloads/AnimeX/<Anime Name>/<Anime Name S1 EP1>.mp4
    IN_APP    // Saved to app private storage for offline viewing in Profile
}

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}

@Serializable
data class DownloadTask(
    val id: String,
    val animeId: Long,
    val animeTitle: String,
    val animePoster: String? = null,
    val episodeId: Long,
    val episodeNumber: Int,
    val destination: DownloadDestination,
    val quality: String = "720p",
    val audioTrack: String = "sub",
    val subtitle: String? = "English",
    val fileName: String,
    val targetPath: String,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Float = 0f,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedSize: String
        get() {
            if (totalBytes <= 0 && downloadedBytes <= 0) return "Estimating..."
            val bytes = if (totalBytes > 0) totalBytes else downloadedBytes
            val mb = bytes / (1024.0 * 1024.0)
            return if (mb >= 1000) {
                String.format("%.2f GB", mb / 1024.0)
            } else {
                String.format("%.1f MB", mb)
            }
        }
}

@Serializable
data class AnimeDownloadGroup(
    val animeId: Long,
    val animeTitle: String,
    val animePoster: String?,
    val tasks: List<DownloadTask>
) {
    val completedCount: Int get() = tasks.count { it.status == DownloadStatus.COMPLETED }
    val totalSizeMb: Double get() = tasks.filter { it.status == DownloadStatus.COMPLETED }.sumOf { 
        (if (it.totalBytes > 0) it.totalBytes else it.downloadedBytes).toDouble() / (1024.0 * 1024.0) 
    }
    val formattedTotalSize: String
        get() = if (totalSizeMb >= 1000) {
            String.format("%.2f GB", totalSizeMb / 1024.0)
        } else {
            String.format("%.1f MB", totalSizeMb)
        }
}
