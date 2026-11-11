package com.fitmate.app

import java.util.Base64
import java.util.prefs.Preferences

object WorkoutProgressStore {
    private val preferences =
        Preferences.userRoot().node("com/fitmate/desktop/workout-progress")

    fun load(): Map<Int, Set<String>> {
        return (1..7).associateWith { day ->
            preferences.get("day_$day", "")
                .split(",")
                .filter { it.isNotBlank() }
                .mapNotNull { encoded ->
                    runCatching {
                        String(
                            Base64.getUrlDecoder().decode(encoded),
                            Charsets.UTF_8
                        )
                    }.getOrNull()
                }
                .toSet()
        }
    }

    fun save(day: Int, exerciseIds: Set<String>) {
        val encoded = exerciseIds.joinToString(",") {
            Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(it.toByteArray(Charsets.UTF_8))
        }

        preferences.put("day_$day", encoded)
        runCatching { preferences.flush() }
    }
}
