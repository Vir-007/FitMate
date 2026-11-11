package com.fitmate.app.tutorial

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

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

    // Android emulator reaches the Mac host through 10.0.2.2.
    // For a physical phone, replace this with your Mac's Wi-Fi IP.
    private const val BASE_URL = "http://172.20.10.3:8000"
    fun searchTutorial(exerciseName: String): Tutorial {
        val encodedName = URLEncoder.encode(exerciseName, "UTF-8")
        val url = URL("$BASE_URL/tutorials/search?exercise=$encodedName&limit=5")

        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 30000

        try {
            val status = connection.responseCode

            if (status != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("Tutorial API returned HTTP $status")
            }

            val response = connection.inputStream.bufferedReader().use {
                it.readText()
            }

            val json = JSONObject(response)
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
                thumbnailUrl = selected.optString("thumbnail_url")
                    .takeIf { it.isNotBlank() },
                views = selected.optLong("view_count", 0),
                likes = if (selected.isNull("like_count")) null
                    else selected.optLong("like_count"),
                isEmbeddable = embeddable
            )
        } finally {
            connection.disconnect()
        }
    }
}
