package com.fitmate.app.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.roundToInt

/** A finished workout. `data class` generates equals, hashCode, toString and copy for free. */
data class WorkoutLog(
    override val id: String = UUID.randomUUID().toString(),
    val exerciseName: String,
    val category: ExerciseCategory,
    val minutes: Int,
    val calories: Double,
    val timestamp: Long = System.currentTimeMillis()
) : Identifiable, Describable {

    val date: LocalDate
        get() = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()

    override val title: String get() = exerciseName
    override val subtitle: String
        get() = "$minutes min · ${calories.roundToInt()} kcal · ${date.format(DATE_FORMAT)}"
    override val emoji: String get() = category.emoji

    companion object {
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM")

        /** FACTORY METHOD: builds a log from *any* Exercise subclass, using its own calorie formula. */
        fun from(exercise: Exercise, minutes: Int, weightKg: Double) = WorkoutLog(
            exerciseName = exercise.name,
            category = exercise.category,
            minutes = minutes,
            calories = exercise.caloriesBurned(weightKg, minutes)
        )
    }
}
