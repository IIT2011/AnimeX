package com.animex.tv.core.network

import com.animex.tv.core.model.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AnimeXApi {

    @GET("api/v1/app/home")
    suspend fun getHome(): HomeFeedDto

    @GET("api/v1/app/anime/{id}")
    suspend fun getAnimeDetail(
        @Path("id") id: Long
    ): AnimeDto

    @GET("api/v1/app/anime/{id}/episodes")
    suspend fun getEpisodes(
        @Path("id") id: Long
    ): EpisodesResponse

    @POST("api/v1/app/episodes/{id}/play")
    suspend fun playEpisode(
        @Path("id") id: Long,
        @Body request: PlayRequest
    ): PlayResponseDto

    @GET("api/v1/app/search/suggest")
    suspend fun search(
        @Query("q") query: String
    ): List<AnimeDto>

    @GET("api/v1/app/browse")
    suspend fun browse(
        @Query("genre") genre: String? = null,
        @Query("type") type: String? = null,
        @Query("status") status: String? = null,
        @Query("season") season: String? = null,
        @Query("year") year: Int? = null,
        @Query("sort") sort: String? = null,
        @Query("page") page: Int = 1
    ): PagedAnimeResponse

    @GET("api/v1/app/genres")
    suspend fun getGenres(): List<GenreItemDto>

    @GET("api/v1/app/schedule")
    suspend fun getSchedule(
        @Query("date") date: String? = null
    ): ScheduleDto
}
