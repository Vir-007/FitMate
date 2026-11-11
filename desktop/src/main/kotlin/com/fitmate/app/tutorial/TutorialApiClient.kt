package com.fitmate.app.tutorial

import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

data class Tutorial(
    val provider: String,
    val videoId: String,
    val title: String,
    val embedUrl: String,
    val watchUrl: String,
    val thumbnailUrl: String?,
    val views: Long,
    val likes: Long?,
    val isEmbeddable: Boolean
)

object TutorialApiClient {

    private const val BASE_URL = "http://127.0.0.1:8000"

    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    fun searchTutorial(exerciseName: String): Tutorial {
        val encodedName = URLEncoder.encode(exerciseName, Charsets.UTF_8)

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$BASE_URL/tutorials/search?exercise=$encodedName&limit=5"))
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build()

        val response = client.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        )

        if (response.statusCode() != 200) {
            throw IllegalStateException(
                "Tutorial API returned HTTP ${response.statusCode()}"
            )
        }

        val json = JSONObject(response.body())
        val selected = json.optJSONObject("selected_tutorial")
            ?: throw IllegalStateException("No tutorial found for $exerciseName")

        val embeddable = selected.optBoolean("is_embeddable", false)
        val embedUrl = selected.optString("embed_url", "")

        if (!embeddable || embedUrl.isBlank()) {
            throw IllegalStateException("Selected tutorial cannot be embedded")
        }

        return Tutorial(
            provider = selected.optString("provider"),
            videoId = selected.optString("video_id"),
            title = selected.optString("title"),
            embedUrl = embedUrl,
            watchUrl = selected.optString("watch_url"),
            thumbnailUrl = selected.optString("thumbnail_url").takeIf { it.isNotBlank() },
            views = selected.optLong("view_count", 0),
            likes = if (selected.isNull("like_count")) null
                    else selected.optLong("like_count"),
            isEmbeddable = embeddable
        )
    }
}
