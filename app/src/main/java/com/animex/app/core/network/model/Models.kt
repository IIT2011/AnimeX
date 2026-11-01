package com.animex.app.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnimeDto(
    val id: Long,
    val slug: String? = null,
    val title: String,
    @SerialName("title_jp") val titleJp: String? = null,
    @SerialName("title_romaji") val titleRomaji: String? = null,
    val poster: String? = null,
    val banner: String? = null,
    val synopsis: String? = null,
    val type: String? = null,
    val sub: Int? = 0,
    val dub: Int? = 0,
    val eps: Int? = 0,
    val rating: String? = null,
    val duration: String? = null,
    val status: String? = null,
    val score: Double? = null,
    val genres: List<GenreDto>? = emptyList(),
    @SerialName("episode_id") val episodeId: Long? = null,
    @SerialName("episode_number") val episodeNumber: Int? = null
)

@Serializable
data class GenreDto(
    val id: Long? = null,
    val name: String
)

@Serializable
data class GenreItemDto(
    val id: Long,
    val slug: String,
    val name: String,
    val image: String? = null
)

@Serializable
data class HomeFeedDto(
    val spotlight: List<AnimeDto> = emptyList(),
    val trending: List<AnimeDto> = emptyList(),
    val latest: List<AnimeDto> = emptyList(),
    @SerialName("top_airing") val topAiring: List<AnimeDto> = emptyList(),
    @SerialName("most_popular") val mostPopular: List<AnimeDto> = emptyList()
)

@Serializable
data class ScheduleDto(
    val date: String? = null,
    val days: List<ScheduleDayDto> = emptyList(),
    val items: List<ScheduleItemDto> = emptyList()
)

@Serializable
data class ScheduleDayDto(
    val date: String,
    val label: String
)

@Serializable
data class ScheduleItemDto(
    val time: String? = null,
    @SerialName("airing_at") val airingAt: String? = null,
    @SerialName("episode_number") val episodeNumber: Int? = null,
    val id: Long,
    val slug: String? = null,
    val title: String,
    @SerialName("title_jp") val titleJp: String? = null,
    val poster: String? = null,
    val type: String? = null,
    val sub: Int? = 0,
    val dub: Int? = 0,
    val eps: Int? = null,
    val status: String? = null,
    val score: Double? = null
)

@Serializable
data class PagedAnimeResponse(
    val data: List<AnimeDto> = emptyList()
)

@Serializable
data class EpisodesResponse(
    val data: List<EpisodeItemDto> = emptyList()
)

@Serializable
data class EpisodeItemDto(
    val id: Long,
    val number: Int,
    val title: String? = null,
    val thumbnail: String? = null,
    val duration: Int? = null
)

@Serializable
data class PlayRequest(
    val track: String = "sub"
)

@Serializable
data class PlayResponseDto(
    @SerialName("episode_id") val episodeId: Long,
    val track: String? = "sub",
    val tracks: List<String> = listOf("sub"),
    val sources: List<PlaybackSourceDto> = emptyList()
)

@Serializable
data class PlaybackSourceDto(
    val kind: String? = null,
    val server: String? = null,
    @SerialName("server_id") val serverId: Int? = null,
    val url: String? = null,
    val mirrors: List<String> = emptyList(),
    @SerialName("media_headers") val mediaHeaders: Map<String, String> = emptyMap(),
    val qualities: List<QualityDto> = emptyList(),
    val subtitles: List<SubtitleDto> = emptyList()
)

@Serializable
data class QualityDto(
    val height: Int? = null,
    val bandwidth: Long? = null
)

@Serializable
data class SubtitleDto(
    val lang: String,
    val label: String,
    val url: String,
    val default: Boolean = false
)

// User Google Cloud Sync State Models
data class UserGoogleAccount(
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val isCloudSyncEnabled: Boolean = true,
    val lastSyncedTime: String = "Just now"
)

@Serializable
data class WatchHistoryEntry(
    val animeId: Long,
    val title: String,
    val poster: String?,
    val episodeId: Long,
    val episodeNumber: Int,
    val progressPercent: Float
)
