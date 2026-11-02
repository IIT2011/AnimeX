package com.animex.app.core.download

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.util.Log
import com.animex.app.core.network.NetworkClient
import com.animex.app.core.network.model.PlayRequest
import com.animex.app.core.network.model.PlaybackSourceDto
import com.animex.app.core.network.model.SubtitleDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.util.UUID

object DownloadRepository {
    private const val TAG = "DownloadRepository"
    private const val DOWNLOADS_FILE = ".animex_downloads.json"

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = mutableMapOf<String, Job>()

    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        loadTasks(context)
    }

    private fun loadTasks(context: Context) {
        try {
            val file = File(context.filesDir, DOWNLOADS_FILE)
            if (file.exists()) {
                val content = file.readText()
                val list = json.decodeFromString<List<DownloadTask>>(content)
                // Any task that was DOWNLOADING before app kill is reset to QUEUED or FAILED
                val cleaned = list.map {
                    if (it.status == DownloadStatus.DOWNLOADING) it.copy(status = DownloadStatus.QUEUED)
                    else it
                }
                _tasks.value = cleaned
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading downloads", e)
        }
    }

    private fun saveTasks(context: Context) {
        try {
            val file = File(context.filesDir, DOWNLOADS_FILE)
            file.writeText(json.encodeToString(_tasks.value))
        } catch (e: Exception) {
            Log.e(TAG, "Error saving downloads", e)
        }
    }

    fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
    }

    fun enqueueBatch(
        context: Context,
        animeId: Long,
        animeTitle: String,
        animePoster: String?,
        episodes: List<Pair<Long, Int>>, // List of (episodeId, episodeNumber)
        destination: DownloadDestination,
        quality: String = "720p",
        audioTrack: String = "sub",
        subtitle: String? = "English"
    ) {
        val sanitizedTitle = sanitizeFileName(animeTitle)
        val baseDir = when (destination) {
            DownloadDestination.EXTERNAL -> {
                val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(publicDownloads, "AnimeX/$sanitizedTitle")
                if (!targetDir.exists()) {
                    try { targetDir.mkdirs() } catch (_: Exception) {}
                }
                if (targetDir.exists() && targetDir.canWrite()) {
                    targetDir
                } else {
                    val fallback = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, "AnimeX/$sanitizedTitle")
                    fallback.apply { mkdirs() }
                    fallback
                }
            }
            DownloadDestination.IN_APP -> {
                val internalMovies = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) 
                    ?: File(context.filesDir, "downloads")
                File(internalMovies, "AnimeX/$sanitizedTitle").apply { mkdirs() }
            }
        }

        val newTasks = mutableListOf<DownloadTask>()
        for ((epId, epNum) in episodes) {
            // Naming format: "<Anime Name + S1 + EP1>" -> e.g. "Solo Leveling S1 EP1.mp4"
            val fileName = "$sanitizedTitle S1 EP$epNum.mp4"
            val targetFile = File(baseDir, fileName)

            val task = DownloadTask(
                id = UUID.randomUUID().toString(),
                animeId = animeId,
                animeTitle = animeTitle,
                animePoster = animePoster,
                episodeId = epId,
                episodeNumber = epNum,
                destination = destination,
                quality = quality,
                audioTrack = audioTrack,
                subtitle = subtitle,
                fileName = fileName,
                targetPath = targetFile.absolutePath,
                status = DownloadStatus.QUEUED
            )
            newTasks.add(task)
        }

        val updated = _tasks.value.toMutableList().apply { addAll(newTasks) }
        _tasks.value = updated
        saveTasks(context)

        // Launch worker for queued tasks
        processNextDownload(context)
    }

    private fun processNextDownload(context: Context) {
        val currentList = _tasks.value
        val next = currentList.firstOrNull { it.status == DownloadStatus.QUEUED } ?: return

        updateTask(context, next.id) { it.copy(status = DownloadStatus.DOWNLOADING, progress = 0.05f) }

        val job = scope.launch {
            try {
                executeDownload(context, next)
            } catch (e: Exception) {
                Log.e(TAG, "Download execution failed for ${next.fileName}", e)
                updateTask(context, next.id) {
                    it.copy(
                        status = DownloadStatus.FAILED,
                        error = e.localizedMessage ?: "Network or playback error"
                    )
                }
            } finally {
                activeJobs.remove(next.id)
                processNextDownload(context)
            }
        }
        activeJobs[next.id] = job
    }

    private suspend fun executeDownload(context: Context, task: DownloadTask) {
        val api = NetworkClient.api
        val playData = api.playEpisode(task.episodeId, PlayRequest(track = task.audioTrack))
        val source = playData.sources.firstOrNull { it.kind == "hls" } ?: playData.sources.firstOrNull()
            ?: throw IllegalStateException("No valid video stream available for episode")

        val masterUrl = source.url ?: throw IllegalStateException("Empty stream URL")
        val headers = source.mediaHeaders.toMutableMap().apply {
            if (!containsKey("Referer")) put("Referer", "https://zokoanime.video/")
            if (!containsKey("User-Agent")) put("User-Agent", "AnimeX/1.1 (Android; Native)")
        }

        val client = NetworkClient.createMedia3OkHttpClient(headers)
        val targetFile = File(task.targetPath)
        targetFile.parentFile?.mkdirs()

        if (masterUrl.endsWith(".m3u8") || masterUrl.contains(".m3u8")) {
            // Resolve HLS playlist
            val masterRequest = Request.Builder().url(masterUrl).apply {
                headers.forEach { (k, v) -> addHeader(k, v) }
            }.build()

            val masterBody = client.newCall(masterRequest).execute().body?.string()
                ?: throw IllegalStateException("Failed to read master playlist")

            // Download companion subtitles early (WebVTT & SubRip .srt for VLC / offline playback)
            downloadSubtitles(
                context = context,
                client = client,
                headers = headers,
                playSources = playData.sources,
                masterUrl = masterUrl,
                masterBody = masterBody,
                targetVideoFile = targetFile,
                preferredLanguage = task.subtitle
            )

            // Pick quality sub-playlist
            val playlistUrl = resolvePlaylistUrl(masterUrl, masterBody, task.quality)

            // Read child playlist for segment list
            val subRequest = Request.Builder().url(playlistUrl).apply {
                headers.forEach { (k, v) -> addHeader(k, v) }
            }.build()
            val subBody = client.newCall(subRequest).execute().body?.string()
                ?: throw IllegalStateException("Failed to read sub playlist")

            val segments = parseSegments(playlistUrl, subBody)
            if (segments.isEmpty()) {
                throw IllegalStateException("Playlist contained zero video segments")
            }

            var totalBytesWritten = 0L
            val totalSegs = segments.size

            FileOutputStream(targetFile, false).use { outStream ->
                for ((index, segUrl) in segments.withIndex()) {
                    val segRequest = Request.Builder().url(segUrl).apply {
                        headers.forEach { (k, v) -> addHeader(k, v) }
                    }.build()

                    var success = false
                    var retries = 3
                    while (!success && retries > 0) {
                        try {
                            client.newCall(segRequest).execute().use { resp ->
                                if (resp.isSuccessful) {
                                    val bytes = resp.body?.bytes()
                                    if (bytes != null) {
                                        outStream.write(bytes)
                                        totalBytesWritten += bytes.size
                                        success = true
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            retries--
                            if (retries <= 0) throw e
                        }
                    }

                    val progress = ((index + 1).toFloat() / totalSegs.toFloat()).coerceIn(0.05f, 0.99f)
                    updateTask(context, task.id, persist = (index % 10 == 0)) {
                        it.copy(
                            progress = progress,
                            downloadedBytes = totalBytesWritten,
                            totalBytes = (totalBytesWritten / (index + 1)) * totalSegs
                        )
                    }
                }
            }

            // Completed!
            updateTask(context, task.id, persist = true) {
                it.copy(
                    status = DownloadStatus.COMPLETED,
                    progress = 1.0f,
                    downloadedBytes = targetFile.length(),
                    totalBytes = targetFile.length()
                )
            }
            if (task.destination == DownloadDestination.EXTERNAL) {
                scanCompanionFiles(context, targetFile)
            }
        } else {
            // Direct file download
            downloadSubtitles(
                context = context,
                client = client,
                headers = headers,
                playSources = playData.sources,
                masterUrl = masterUrl,
                masterBody = null,
                targetVideoFile = targetFile,
                preferredLanguage = task.subtitle
            )

            val directRequest = Request.Builder().url(masterUrl).apply {
                headers.forEach { (k, v) -> addHeader(k, v) }
            }.build()

            client.newCall(directRequest).execute().use { resp ->
                val body = resp.body ?: throw IllegalStateException("Empty direct body")
                val totalLength = body.contentLength().coerceAtLeast(1L)
                var bytesCopied = 0L

                FileOutputStream(targetFile).use { out ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(32 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            out.write(buffer, 0, read)
                            bytesCopied += read
                            val progress = (bytesCopied.toFloat() / totalLength.toFloat()).coerceIn(0.05f, 0.99f)
                            updateTask(context, task.id) {
                                it.copy(progress = progress, downloadedBytes = bytesCopied, totalBytes = totalLength)
                            }
                        }
                    }
                }
            }

            updateTask(context, task.id, persist = true) {
                it.copy(
                    status = DownloadStatus.COMPLETED,
                    progress = 1.0f,
                    downloadedBytes = targetFile.length(),
                    totalBytes = targetFile.length()
                )
            }
            if (task.destination == DownloadDestination.EXTERNAL) {
                scanCompanionFiles(context, targetFile)
            }
        }
    }

    private fun resolvePlaylistUrl(masterUrl: String, masterBody: String, quality: String): String {
        val lines = masterBody.lines().map { it.trim() }
        var targetLine: String? = null
        val targetHeight = quality.replace("p", "").toIntOrNull() ?: 720

        var bestMatchLine: String? = null
        var bestDiff = Int.MAX_VALUE

        for (i in lines.indices) {
            val line = lines[i]
            if (line.startsWith("#EXT-X-STREAM-INF")) {
                val heightMatch = Regex("RESOLUTION=\\d+x(\\d+)").find(line)
                val h = heightMatch?.groupValues?.get(1)?.toIntOrNull() ?: 720
                val nextUrlLine = lines.getOrNull(i + 1)?.takeIf { !it.startsWith("#") } ?: continue

                val diff = Math.abs(h - targetHeight)
                if (diff < bestDiff) {
                    bestDiff = diff
                    bestMatchLine = nextUrlLine
                }
            }
        }

        val relativeUrl = bestMatchLine ?: lines.firstOrNull { !it.startsWith("#") && it.isNotBlank() } ?: ""
        return resolveRelative(masterUrl, relativeUrl)
    }

    private fun parseSegments(playlistUrl: String, playlistBody: String): List<String> {
        val list = mutableListOf<String>()
        val lines = playlistBody.lines().map { it.trim() }
        for (line in lines) {
            if (line.isNotBlank() && !line.startsWith("#")) {
                list.add(resolveRelative(playlistUrl, line))
            }
        }
        return list
    }

    private fun resolveRelative(baseUrl: String, relOrAbsUrl: String): String {
        if (relOrAbsUrl.startsWith("http://") || relOrAbsUrl.startsWith("https://")) {
            return relOrAbsUrl
        }
        return try {
            val baseUri = URI(baseUrl)
            baseUri.resolve(relOrAbsUrl).toString()
        } catch (e: Exception) {
            val lastSlash = baseUrl.lastIndexOf('/')
            if (lastSlash >= 0) baseUrl.substring(0, lastSlash + 1) + relOrAbsUrl else relOrAbsUrl
        }
    }

    private fun updateTask(context: Context, taskId: String, persist: Boolean = true, transform: (DownloadTask) -> DownloadTask) {
        val list = _tasks.value.toMutableList()
        val index = list.indexOfFirst { it.id == taskId }
        if (index >= 0) {
            list[index] = transform(list[index])
            _tasks.value = list
            if (persist) {
                saveTasks(context)
            }
        }
    }

    fun retryDownload(context: Context, taskId: String) {
        updateTask(context, taskId, persist = true) {
            it.copy(status = DownloadStatus.QUEUED, error = null, progress = 0.05f)
        }
        processNextDownload(context)
    }

    fun cancelOrDelete(context: Context, taskId: String) {
        activeJobs[taskId]?.cancel()
        activeJobs.remove(taskId)

        val task = _tasks.value.firstOrNull { it.id == taskId }
        if (task != null) {
            try {
                val file = File(task.targetPath)
                if (file.exists()) file.delete()
                val parent = file.parentFile
                val base = file.nameWithoutExtension
                if (parent != null && parent.exists()) {
                    listOf(".srt", ".en.srt", ".English.srt", ".vtt", ".en.vtt").forEach { ext ->
                        val subFile = File(parent, "$base$ext")
                        if (subFile.exists()) subFile.delete()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting file for $taskId", e)
            }
        }

        _tasks.value = _tasks.value.filter { it.id != taskId }
        saveTasks(context)
    }

    private fun downloadSubtitles(
        context: Context,
        client: OkHttpClient,
        headers: Map<String, String>,
        playSources: List<PlaybackSourceDto>,
        masterUrl: String,
        masterBody: String?,
        targetVideoFile: File,
        preferredLanguage: String?
    ) {
        if (preferredLanguage.isNullOrBlank() || preferredLanguage.equals("None", ignoreCase = true)) {
            return
        }

        try {
            data class SubCandidate(val lang: String, val label: String, val url: String, val default: Boolean)
            val candidates = mutableListOf<SubCandidate>()

            // 1. Gather subtitles from all PlaybackSourceDto
            for (src in playSources) {
                for (sub in src.subtitles) {
                    if (sub.url.isNotBlank()) {
                        val fullUrl = resolveRelative(src.url ?: masterUrl, sub.url)
                        candidates.add(SubCandidate(sub.lang, sub.label, fullUrl, sub.default))
                    }
                }
            }

            // 2. Gather subtitles from HLS master playlist if present
            if (!masterBody.isNullOrBlank()) {
                val subMediaRegex = Regex("""#EXT-X-MEDIA:[^\r\n]*TYPE=SUBTITLES[^\r\n]*""")
                val uriRegex = Regex("""URI="([^"]+)"""")
                val nameRegex = Regex("""NAME="([^"]+)"""")
                val langRegex = Regex("""LANGUAGE="([^"]+)"""")
                val defaultRegex = Regex("""DEFAULT=(YES|NO)""")

                for (match in subMediaRegex.findAll(masterBody)) {
                    val line = match.value
                    val uriMatch = uriRegex.find(line)?.groupValues?.get(1) ?: continue
                    val name = nameRegex.find(line)?.groupValues?.get(1) ?: "Subtitles"
                    val lang = langRegex.find(line)?.groupValues?.get(1) ?: "en"
                    val isDefault = defaultRegex.find(line)?.groupValues?.get(1).equals("YES", ignoreCase = true)
                    val fullUrl = resolveRelative(masterUrl, uriMatch)
                    candidates.add(SubCandidate(lang, name, fullUrl, isDefault))
                }
            }

            if (candidates.isEmpty()) {
                Log.d(TAG, "No subtitle streams found for ${targetVideoFile.name}")
                return
            }

            // 3. Find the best candidate matching preferredLanguage (e.g., "English")
            val target = preferredLanguage.trim()
            val chosen = candidates.firstOrNull {
                it.label.contains(target, ignoreCase = true) ||
                (target.contains("Eng", ignoreCase = true) && (it.lang.equals("en", ignoreCase = true) || it.label.contains("English", ignoreCase = true)))
            } ?: candidates.firstOrNull { it.default } ?: candidates.first()

            Log.d(TAG, "Selected subtitle: ${chosen.label} (${chosen.url})")

            val subReq = Request.Builder().url(chosen.url).apply {
                headers.forEach { (k, v) -> addHeader(k, v) }
            }.build()

            val subResp = client.newCall(subReq).execute()
            if (!subResp.isSuccessful) {
                Log.w(TAG, "Failed to download subtitle from ${chosen.url}: code ${subResp.code}")
                return
            }

            val body = subResp.body?.string() ?: return
            val rawVtt = if (body.contains("#EXTM3U")) {
                // HLS playlist of subtitles
                val subLines = body.lines().map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("#") }
                val sb = StringBuilder("WEBVTT\n\n")
                for (seg in subLines) {
                    val segUrl = resolveRelative(chosen.url, seg)
                    try {
                        val req = Request.Builder().url(segUrl).apply {
                            headers.forEach { (k, v) -> addHeader(k, v) }
                        }.build()
                        client.newCall(req).execute().use { r ->
                            if (r.isSuccessful) {
                                val s = r.body?.string() ?: ""
                                val content = if (s.contains("WEBVTT")) {
                                    s.substringAfter("WEBVTT").trimStart()
                                } else {
                                    s
                                }
                                if (content.isNotBlank()) {
                                    sb.append(content).append("\n\n")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error downloading sub segment: $segUrl", e)
                    }
                }
                sb.toString()
            } else {
                body
            }

            if (rawVtt.isBlank()) {
                Log.w(TAG, "Empty subtitle content downloaded")
                return
            }

            val parentDir = targetVideoFile.parentFile ?: return
            val baseName = targetVideoFile.nameWithoutExtension

            // Write raw VTT format files
            File(parentDir, "$baseName.vtt").writeText(rawVtt, Charsets.UTF_8)
            File(parentDir, "$baseName.en.vtt").writeText(rawVtt, Charsets.UTF_8)

            // Convert and write SubRip (.srt) format files for VLC & external players
            val srt = convertVttToSrt(rawVtt)
            if (srt.isNotBlank()) {
                File(parentDir, "$baseName.srt").writeText(srt, Charsets.UTF_8)
                File(parentDir, "$baseName.en.srt").writeText(srt, Charsets.UTF_8)
                File(parentDir, "$baseName.English.srt").writeText(srt, Charsets.UTF_8)
            }

            Log.i(TAG, "Downloaded and saved companion subtitles for $baseName successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download subtitles for ${targetVideoFile.name}", e)
        }
    }

    fun convertVttToSrt(vttContent: String): String {
        val srtBuilder = StringBuilder()
        val lines = vttContent.lines()
        var cueIndex = 1
        var i = 0

        val timeRegex = Regex("""(?:(\d{1,2}):)?(\d{2}):(\d{2})[.,](\d{1,4})\s*-->\s*(?:(\d{1,2}):)?(\d{2}):(\d{2})[.,](\d{1,4})""")

        while (i < lines.size) {
            val line = lines[i].trim()
            val match = timeRegex.find(line)
            if (match != null) {
                val groups = match.groupValues
                val startH = groups[1].ifEmpty { "00" }.padStart(2, '0')
                val startM = groups[2].padStart(2, '0')
                val startS = groups[3].padStart(2, '0')
                val startMs = groups[4].padEnd(3, '0').take(3)

                val endH = groups[5].ifEmpty { "00" }.padStart(2, '0')
                val endM = groups[6].padStart(2, '0')
                val endS = groups[7].padStart(2, '0')
                val endMs = groups[8].padEnd(3, '0').take(3)

                val startTime = "$startH:$startM:$startS,$startMs"
                val endTime = "$endH:$endM:$endS,$endMs"

                i++
                val cueLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().isNotEmpty() && !timeRegex.containsMatchIn(lines[i])) {
                    val rawText = lines[i]
                    val cleaned = rawText
                        .replace(Regex("<c\\.[^>]+>"), "")
                        .replace("</c>", "")
                        .replace(Regex("<v[^>]*>"), "")
                        .replace("</v>", "")
                        .replace(Regex("<lang[^>]*>"), "")
                        .replace("</lang>", "")
                        .replace("&amp;", "&")
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .replace("&nbsp;", " ")
                        .trim()
                    if (cleaned.isNotEmpty()) {
                        cueLines.add(cleaned)
                    }
                    i++
                }

                if (cueLines.isNotEmpty()) {
                    srtBuilder.append(cueIndex).append("\r\n")
                    srtBuilder.append("$startTime --> $endTime").append("\r\n")
                    cueLines.forEach { srtBuilder.append(it).append("\r\n") }
                    srtBuilder.append("\r\n")
                    cueIndex++
                }
            } else {
                i++
            }
        }

        return srtBuilder.toString()
    }

    private fun scanCompanionFiles(context: Context, videoFile: File) {
        try {
            val filesToScan = mutableListOf(videoFile.absolutePath)
            val mimes = mutableListOf("video/mp4")
            val parent = videoFile.parentFile
            val base = videoFile.nameWithoutExtension
            if (parent != null && parent.exists()) {
                listOf(
                    "$base.srt" to "application/x-subrip",
                    "$base.en.srt" to "application/x-subrip",
                    "$base.English.srt" to "application/x-subrip",
                    "$base.vtt" to "text/vtt",
                    "$base.en.vtt" to "text/vtt"
                ).forEach { (fName, mime) ->
                    val f = File(parent, fName)
                    if (f.exists()) {
                        filesToScan.add(f.absolutePath)
                        mimes.add(mime)
                    }
                }
            }
            MediaScannerConnection.scanFile(
                context,
                filesToScan.toTypedArray(),
                mimes.toTypedArray(),
                null
            )
        } catch (e: Exception) {
            Log.w(TAG, "MediaScanner scan failed", e)
        }
    }

    fun getInAppGroups(): List<AnimeDownloadGroup> {
        val completedInApp = _tasks.value.filter { 
            it.destination == DownloadDestination.IN_APP && it.status == DownloadStatus.COMPLETED 
        }
        return completedInApp.groupBy { it.animeId }.map { (animeId, tasks) ->
            val first = tasks.first()
            AnimeDownloadGroup(
                animeId = animeId,
                animeTitle = first.animeTitle,
                animePoster = first.animePoster,
                tasks = tasks.sortedBy { it.episodeNumber }
            )
        }
    }
}
