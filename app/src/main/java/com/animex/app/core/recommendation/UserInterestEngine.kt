package com.animex.app.core.recommendation

import android.content.Context
import com.animex.app.core.network.model.AnimeDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

data class ScoredAnime(
    val anime: AnimeDto,
    val matchPercentage: Int,
    val matchReason: String
)

object UserInterestEngine {

    private const val USER_INTEL_FILE_NAME = ".animex_user_intel.txt"
    private val scope = CoroutineScope(Dispatchers.IO)

    // Genre Affinity Vector: genre name -> weight (Float)
    private val genreWeights = ConcurrentHashMap<String, Float>()

    // Tag / Keyword affinities from searches & watched synopses
    private val keywordAffinities = ConcurrentHashMap<String, Float>()

    // Cache of anime genres discovered across endpoints
    private val genreCache = ConcurrentHashMap<Long, List<String>>()

    // History of interacted anime IDs
    private val interactedAnimeIds = ConcurrentHashMap.newKeySet<Long>()

    // Count of learning iterations
    private var interactionCounter = 0

    // Reactive notification trigger for UI listeners
    private val _intelVersion = MutableStateFlow(0)
    val intelVersion: StateFlow<Int> = _intelVersion.asStateFlow()

    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        isInitialized = true

        scope.launch {
            loadIntelHiddenFile(context)
        }
    }

    /**
     * Resolves the genres for an anime (from explicit DTO, memory cache, or title inference).
     */
    fun resolveAnimeGenres(anime: AnimeDto): List<String> {
        val explicit = anime.genres?.map { it.name }?.filter { it.isNotBlank() }
        if (!explicit.isNullOrEmpty()) {
            genreCache[anime.id] = explicit
            return explicit
        }
        genreCache[anime.id]?.let { return it }

        // Fallback inference from title when list API returns null genres
        val titleLower = anime.title.lowercase(Locale.getDefault())
        val inferred = mutableListOf<String>()
        for ((genreName, keywords) in GENRE_KEYWORD_RULES) {
            if (keywords.any { titleLower.contains(it) }) {
                inferred.add(genreName)
            }
        }
        val result = if (inferred.isNotEmpty()) inferred else listOf(anime.type ?: "Anime")
        genreCache[anime.id] = result
        return result
    }

    /**
     * Records when a user browses or inspects an anime's details.
     */
    fun recordAnimeView(context: Context, anime: AnimeDto) {
        scope.launch {
            interactedAnimeIds.add(anime.id)
            interactionCounter++

            val genres = resolveAnimeGenres(anime)
            genres.forEach { genreName ->
                val current = genreWeights.getOrDefault(genreName, 1.0f)
                genreWeights[genreName] = (current + 1.8f).coerceAtMost(50.0f)
            }

            extractAndBoostKeywords(anime.title, anime.synopsis, boost = 1.0f)

            saveIntelHiddenFile(context)
            _intelVersion.value = _intelVersion.value + 1
        }
    }

    /**
     * Records streaming playback progress.
     */
    fun recordPlayback(
        context: Context,
        animeId: Long,
        title: String,
        genres: List<String>?,
        progressPercent: Float
    ) {
        scope.launch {
            interactedAnimeIds.add(animeId)
            interactionCounter++

            val completionBonus = if (progressPercent >= 0.7f) 3.5f else 1.8f
            val targetGenres = if (!genres.isNullOrEmpty()) {
                genres
            } else {
                genreCache[animeId] ?: emptyList()
            }

            targetGenres.forEach { genreName ->
                val current = genreWeights.getOrDefault(genreName, 1.0f)
                genreWeights[genreName] = (current + completionBonus).coerceAtMost(50.0f)
            }

            extractAndBoostKeywords(title, null, boost = 1.2f)

            saveIntelHiddenFile(context)
            _intelVersion.value = _intelVersion.value + 1
        }
    }

    /**
     * Records addition to watchlist (strong intent signal).
     */
    fun recordWatchlistAdd(context: Context, anime: AnimeDto) {
        scope.launch {
            interactedAnimeIds.add(anime.id)
            interactionCounter++

            val genres = resolveAnimeGenres(anime)
            genres.forEach { genreName ->
                val current = genreWeights.getOrDefault(genreName, 1.0f)
                genreWeights[genreName] = (current + 4.0f).coerceAtMost(50.0f)
            }

            extractAndBoostKeywords(anime.title, anime.synopsis, boost = 2.0f)

            saveIntelHiddenFile(context)
            _intelVersion.value = _intelVersion.value + 1
        }
    }

    /**
     * Records a search query to adapt affinities towards searched terms and genres.
     */
    fun recordSearch(context: Context, query: String) {
        if (query.isBlank()) return
        scope.launch {
            interactionCounter++
            extractAndBoostKeywords(query, null, boost = 2.5f)

            val qLower = query.lowercase(Locale.getDefault())
            genreWeights.keys.forEach { genreKey ->
                if (genreKey.lowercase(Locale.getDefault()).contains(qLower)) {
                    val current = genreWeights.getOrDefault(genreKey, 1.0f)
                    genreWeights[genreKey] = (current + 3.5f).coerceAtMost(50.0f)
                }
            }

            saveIntelHiddenFile(context)
            _intelVersion.value = _intelVersion.value + 1
        }
    }

    /**
     * Calculates the match score (72% - 98%) and reasoning for an anime.
     */
    fun calculateMatch(anime: AnimeDto): ScoredAnime {
        val genres = resolveAnimeGenres(anime)
        val genreAffinities = genres.map { genreWeights.getOrDefault(it, 1.0f) }
        val avgAffinity = genreAffinities.sum() / genreAffinities.size.coerceAtLeast(1)
        val genreBonus = (avgAffinity * 1.5f).coerceIn(0f, 20.0f)

        // Quality rating score boost
        val scoreVal = (anime.score ?: 7.5).toFloat()
        val scoreBonus = ((scoreVal - 6.0f) * 1.4f).coerceIn(0f, 5.0f)

        // Airing status boost
        val statusBonus = if (anime.status?.contains("air", ignoreCase = true) == true) 1.5f else 0.5f

        // Keyword overlap boost
        var keywordBonus = 0f
        val titleLower = anime.title.lowercase(Locale.getDefault())
        keywordAffinities.forEach { (keyword, weight) ->
            if (titleLower.contains(keyword)) {
                keywordBonus += (weight * 0.5f).coerceAtMost(4.0f)
            }
        }

        val rawMatch = 70.0f + genreBonus + scoreBonus + statusBonus + keywordBonus
        val finalPercentage = rawMatch.roundToInt().coerceIn(72, 98)

        val reason = if (genres.isNotEmpty() && genres.first() != "Anime") {
            genres.take(2).joinToString(" • ")
        } else {
            anime.type ?: "Series"
        }

        return ScoredAnime(
            anime = anime,
            matchPercentage = finalPercentage,
            matchReason = reason
        )
    }

    /**
     * Scans a catalog of candidate anime and ranks them using user interest vectors.
     */
    fun rankAndRecommend(candidates: List<AnimeDto>, limit: Int = 14): List<ScoredAnime> {
        val scoredList = candidates
            .distinctBy { it.id }
            .map { anime -> calculateMatch(anime) }
            .sortedByDescending { it.matchPercentage }

        return scoredList.take(limit)
    }

    /**
     * Returns the user's top favorite genres based on interest weights.
     */
    fun getTopFavoriteGenres(limit: Int = 3): List<String> {
        return genreWeights.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }
    }

    /**
     * Reads the raw hidden .txt intelligence file.
     */
    suspend fun readHiddenIntelFile(context: Context): String = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, USER_INTEL_FILE_NAME)
        if (file.exists()) file.readText() else "User profile not yet generated."
    }

    private fun extractAndBoostKeywords(title: String, synopsis: String?, boost: Float) {
        val words = (title + " " + (synopsis ?: "")).lowercase(Locale.getDefault())
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split("\\s+".toRegex())
            .filter { it.length >= 4 && !STOP_WORDS.contains(it) }

        words.take(15).forEach { word ->
            val curr = keywordAffinities.getOrDefault(word, 0.5f)
            keywordAffinities[word] = (curr + boost).coerceAtMost(25.0f)
        }
    }

    /**
     * Saves user interest matrix to a hidden .txt file in context.filesDir.
     */
    private suspend fun saveIntelHiddenFile(context: Context) = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, USER_INTEL_FILE_NAME)
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            val topGenresFormatted = genreWeights.entries
                .sortedByDescending { it.value }
                .take(8)
                .joinToString(", ") { "${it.key}: ${String.format(Locale.US, "%.1f", it.value)}" }

            val topKeywordsFormatted = keywordAffinities.entries
                .sortedByDescending { it.value }
                .take(10)
                .joinToString(", ") { "${it.key}: ${String.format(Locale.US, "%.1f", it.value)}" }

            val content = buildString {
                appendLine("==================================================")
                appendLine("     ANIMEX USER PROFILE & RECOMMENDATION MATRIX  ")
                appendLine("==================================================")
                appendLine("TIMESTAMP_UTC       : $dateStr")
                appendLine("ENGINE_VERSION      : 2.4.0-RECOMMENDER")
                appendLine("TOTAL_INTERACTIONS  : $interactionCounter")
                appendLine("TRACKED_ANIME_IDS   : ${interactedAnimeIds.joinToString(",")}")
                appendLine("--------------------------------------------------")
                appendLine("TOP_GENRE_AFFINITIES:")
                appendLine(if (topGenresFormatted.isNotBlank()) "  $topGenresFormatted" else "  [Action: 5.0, Sci-Fi: 4.5, Shounen: 4.0 (Default Seed)]")
                appendLine("--------------------------------------------------")
                appendLine("TOP_KEYWORD_VECTORS :")
                appendLine(if (topKeywordsFormatted.isNotBlank()) "  $topKeywordsFormatted" else "  [cyber, hero, magic, ninja (Default Seed)]")
                appendLine("--------------------------------------------------")
                appendLine("CLOUD_SYNC_STATUS   : READY_FOR_SYNC")
                appendLine("STORAGE_MODE        : SANDBOX_HIDDEN_TXT")
                appendLine("==================================================")
            }

            file.writeText(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Loads existing user interest profile from the hidden file on device startup.
     */
    private suspend fun loadIntelHiddenFile(context: Context) = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, USER_INTEL_FILE_NAME)
            if (!file.exists()) {
                seedDefaultAffinities()
                saveIntelHiddenFile(context)
                return@withContext
            }

            val lines = file.readLines()
            for (line in lines) {
                if (line.contains(":") && !line.startsWith("===") && !line.startsWith("---")) {
                    val parts = line.split(":", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim()
                        if (key == "TOTAL_INTERACTIONS") {
                            interactionCounter = value.toIntOrNull() ?: 0
                        }
                    }
                }
            }

            if (genreWeights.isEmpty()) {
                seedDefaultAffinities()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            seedDefaultAffinities()
        }
    }

    private fun seedDefaultAffinities() {
        genreWeights["Action"] = 6.0f
        genreWeights["Sci-Fi"] = 5.5f
        genreWeights["Shounen"] = 5.0f
        genreWeights["Fantasy"] = 4.8f
        genreWeights["Supernatural"] = 4.0f
        genreWeights["Adventure"] = 3.5f
    }

    private val GENRE_KEYWORD_RULES = mapOf(
        "Action" to listOf("fight", "battle", "war", "sword", "blade", "titan", "hunter", "slayer", "punch", "ninja", "kaisen", "gear", "blood", "kill"),
        "Sci-Fi" to listOf("cyber", "edgerunner", "robot", "space", "mecha", "tech", "future", "gundam"),
        "Fantasy" to listOf("magic", "mage", "dungeon", "isekai", "tensei", "reincarnat", "demon", "guild", "level", "sorcerer", "witch"),
        "Romance" to listOf("love", "bride", "wedding", "heart", "girlfriend", "boyfriend", "kiss", "kanojo"),
        "Comedy" to listOf("chiikawa", "comedy", "funny", "gag", "asobi"),
        "Adventure" to listOf("journey", "quest", "beyond", "odyssey", "expedition", "travel", "clover", "piece"),
        "Mystery" to listOf("apothecary", "detective", "mystery", "diaries", "case", "investigat")
    )

    private val STOP_WORDS = setOf(
        "this", "that", "with", "from", "have", "were", "they", "their", "when",
        "about", "which", "will", "would", "there", "anime", "series", "season",
        "episode", "story", "world", "after", "before", "while", "into", "some"
    )
}
