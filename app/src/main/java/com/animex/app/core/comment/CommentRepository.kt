package com.animex.app.core.comment

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

object CommentRepository {
    private const val TAG = "CommentRepository"
    private const val COMMENTS_FILE = ".animex_comments.json"
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    // Map: animeId -> List of AnimeComment
    private val _comments = MutableStateFlow<Map<Long, List<AnimeComment>>>(emptyMap())
    val comments: StateFlow<Map<Long, List<AnimeComment>>> = _comments.asStateFlow()

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        loadComments(context)
    }

    private fun loadComments(context: Context) {
        try {
            val file = File(context.filesDir, COMMENTS_FILE)
            if (file.exists()) {
                val content = file.readText()
                val map = json.decodeFromString<Map<Long, List<AnimeComment>>>(content)
                _comments.value = map
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading comments", e)
        }
    }

    private fun saveComments(context: Context) {
        try {
            val file = File(context.filesDir, COMMENTS_FILE)
            file.writeText(json.encodeToString(_comments.value))
        } catch (e: Exception) {
            Log.e(TAG, "Error saving comments", e)
        }
    }

    fun getCommentsForAnime(animeId: Long): List<AnimeComment> {
        val list = _comments.value[animeId]
        if (!list.isNullOrEmpty()) return list

        // Generate seeded initial community comments if none exist
        return listOf(
            AnimeComment(
                id = "seed_1_$animeId",
                animeId = animeId,
                username = "NeoSamurai",
                text = "The fight choreography and sound design in this season are completely unreal! 🔥",
                timestamp = System.currentTimeMillis() - (1000 * 60 * 45), // 45m ago
                likes = 14,
                avatarColor = 0xFF00F0FF
            ),
            AnimeComment(
                id = "seed_2_$animeId",
                animeId = animeId,
                username = "CyberRider_99",
                text = "Episode pacing has been spot on. Highly recommend watching with subtitles in 1080p.",
                timestamp = System.currentTimeMillis() - (1000 * 60 * 180), // 3h ago
                likes = 8,
                avatarColor = 0xFFFF007F
            )
        )
    }

    fun postComment(context: Context, animeId: Long, username: String, text: String) {
        if (text.isBlank()) return
        val currentMap = _comments.value.toMutableMap()
        val currentList = (currentMap[animeId] ?: getCommentsForAnime(animeId)).toMutableList()

        val newComment = AnimeComment(
            id = UUID.randomUUID().toString(),
            animeId = animeId,
            username = username.ifBlank { "CyberUser_X" },
            text = text.trim(),
            timestamp = System.currentTimeMillis(),
            likes = 0,
            isLikedByMe = false,
            avatarColor = if (username.hashCode() % 2 == 0) 0xFF00F0FF else 0xFFFF007F
        )

        currentList.add(0, newComment)
        currentMap[animeId] = currentList
        _comments.value = currentMap
        saveComments(context)
    }

    fun toggleLike(context: Context, animeId: Long, commentId: String) {
        val currentMap = _comments.value.toMutableMap()
        val currentList = (currentMap[animeId] ?: getCommentsForAnime(animeId)).toMutableList()

        val index = currentList.indexOfFirst { it.id == commentId }
        if (index >= 0) {
            val c = currentList[index]
            val newLiked = !c.isLikedByMe
            val newLikes = if (newLiked) c.likes + 1 else (c.likes - 1).coerceAtLeast(0)
            currentList[index] = c.copy(isLikedByMe = newLiked, likes = newLikes)
            currentMap[animeId] = currentList
            _comments.value = currentMap
            saveComments(context)
        }
    }
}
