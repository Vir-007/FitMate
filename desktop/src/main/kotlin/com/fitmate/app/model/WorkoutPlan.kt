package com.fitmate.app.model

import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** COMPOSITION: a session *has* a list of exercises. */
class WorkoutSession(
    val day: DayOfWeek,
    val focus: String,
    val exercises: List<Exercise>,
    /**
     * Day 1–7 of the week, set by [com.fitmate.app.planner.BasePlanStrategy.buildPlan].
     * 0 means "not placed in a plan yet" (for example a session built by hand in a test).
     */
    val dayNumber: Int = 0
) : Describable {

    val isRestDay: Boolean get() = exercises.isEmpty()

    /** CultFit-style label: "Day 1 — Chest Day". Falls back to just the focus when unplaced. */
    val dayLabel: String get() = if (dayNumber in 1..7) "Day $dayNumber — $focus" else focus

    fun totalMinutes(): Int = exercises.sumOf { it.estimatedMinutes() }

    /** Polymorphism: each exercise is asked for calories using its *own* formula. */
    fun estimatedCalories(weightKg: Double): Double =
        exercises.sumOf { it.caloriesBurned(weightKg, it.estimatedMinutes()) }

    val dayName: String get() = day.getDisplayName(TextStyle.SHORT, Locale.getDefault())

    override val title: String get() = "$dayLabel · $dayName"
    override val subtitle: String
        get() = if (isRestDay) "Recovery day" else "${exercises.size} exercises · ~${totalMinutes()} min"
    override val emoji: String get() = if (isRestDay) "😴" else exercises.first().emoji
}

/** COMPOSITION again: a plan *has* seven sessions. */
class WorkoutPlan(
    val name: String,
    val goal: FitnessGoal,
    val level: Difficulty,
    val sessions: List<WorkoutSession>
) {
    val activeDays: Int get() = sessions.count { !it.isRestDay }

    val weeklyMinutes: Int get() = sessions.sumOf { it.totalMinutes() }

    fun weeklyCalories(weightKg: Double): Double = sessions.sumOf { it.estimatedCalories(weightKg) }

    fun sessionFor(day: DayOfWeek): WorkoutSession? = sessions.firstOrNull { it.day == day }

    /** Day 1 is Monday, Day 7 is Sunday. */
    fun sessionFor(dayNumber: Int): WorkoutSession? = sessions.firstOrNull { it.dayNumber == dayNumber }

    /** Today's session, by the calendar day. */
    fun sessionForToday(): WorkoutSession? = sessionFor(java.time.LocalDate.now().dayOfWeek)
}
