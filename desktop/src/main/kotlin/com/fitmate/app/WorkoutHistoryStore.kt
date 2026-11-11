package com.fitmate.app

import java.time.LocalDate
import java.util.prefs.Preferences

data class WorkoutHistoryEntry(
    val day: Int,
    val date: String,
    val exerciseCount: Int,
    val minutes: Int,
    val calories: Double
)

object WorkoutHistoryStore {
    private val preferences =
        Preferences.userRoot().node("com/fitmate/desktop/workout-history")

    fun load(): List<WorkoutHistoryEntry> {
        val count = preferences.getInt("count", 0)

        return (0 until count).mapNotNull { index ->
            runCatching {
                val values = preferences.get("entry_$index", "").split("|")
                WorkoutHistoryEntry(
                    day = values[0].toInt(),
                    date = values[1],
                    exerciseCount = values[2].toInt(),
                    minutes = values[3].toInt(),
                    calories = values[4].toDouble()
                )
            }.getOrNull()
        }.sortedByDescending { it.date }
    }

    fun record(
        day: Int,
        exerciseCount: Int,
        minutes: Int,
        calories: Double
    ) {
        val date = LocalDate.now().toString()

        if (load().any { it.day == day && it.date == date }) return

        val entry = WorkoutHistoryEntry(
            day, date, exerciseCount, minutes, calories
        )

        val count = preferences.getInt("count", 0)

        preferences.put(
            "entry_$count",
            "${entry.day}|${entry.date}|${entry.exerciseCount}|${entry.minutes}|${entry.calories}"
        )
        preferences.putInt("count", count + 1)
        preferences.flush()
    }
}
