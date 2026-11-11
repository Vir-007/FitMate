package com.fitmate.app

import com.fitmate.app.health.HealthMetrics
import com.fitmate.app.model.CardioExercise
import com.fitmate.app.model.Difficulty
import com.fitmate.app.model.Exercise
import com.fitmate.app.model.ExerciseLibrary
import com.fitmate.app.model.FitnessGoal
import com.fitmate.app.model.FlexibilityExercise
import com.fitmate.app.model.HiitExercise
import com.fitmate.app.model.BmiCategory
import com.fitmate.app.model.StrengthExercise
import com.fitmate.app.model.UserProfile
import com.fitmate.app.model.WorkoutLog
import com.fitmate.app.planner.FatLossStrategy
import com.fitmate.app.planner.MuscleGainStrategy
import com.fitmate.app.planner.WorkoutPlanFactory
import com.fitmate.app.suggestions.Priority
import com.fitmate.app.suggestions.SuggestionContext
import com.fitmate.app.suggestions.SuggestionEngine
import com.fitmate.app.tracking.WorkoutStats
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainTest {

    private fun profile(goal: FitnessGoal = FitnessGoal.STAY_FIT) =
        UserProfile("Asha", 21, 170.0, 65.0, goal = goal, experience = Difficulty.BEGINNER)

    private fun logOn(date: LocalDate, minutes: Int = 30) = WorkoutLog(
        exerciseName = "Running",
        category = ExerciseLibrary["running"].category,
        minutes = minutes,
        calories = 200.0,
        timestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 3_600_000
    )

    // --- Encapsulation ---

    @Test
    fun bmiIsComputedFromHeightAndWeight() {
        assertEquals(22.49, profile().bmi, 0.01)
        assertEquals(BmiCategory.NORMAL, profile().bmiCategory)
    }

    @Test(expected = IllegalArgumentException::class)
    fun impossibleWeightIsRejected() {
        UserProfile("Asha", 21, 170.0, 5.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun setterAlsoValidates() {
        profile().heightCm = 400.0
    }

    // --- Inheritance & polymorphism ---

    @Test
    fun hiitIsACardioExerciseAndBurnsMore() {
        val hiit: Exercise = ExerciseLibrary["mountain_climbers"]
        assertTrue(hiit is CardioExercise)
        val plainCardio = CardioExercise("x", "x", "", Difficulty.BEGINNER, hiit.met, 10)
        assertEquals(plainCardio.caloriesBurned(65.0, 10) * 1.15, hiit.caloriesBurned(65.0, 10), 0.001)
    }

    @Test
    fun scaledForKeepsTheSubclassAndAddsVolume() {
        val pushups = ExerciseLibrary["pushups"] as StrengthExercise
        val advanced = pushups.scaledFor(Difficulty.ADVANCED)
        assertEquals(pushups.sets + 2, advanced.sets)
        assertTrue(ExerciseLibrary["tabata"].scaledFor(Difficulty.ADVANCED) is HiitExercise)
    }

    @Test
    fun ofTypeUsesTheClassHierarchy() {
        val cardio = ExerciseLibrary.ofType<CardioExercise>()
        val hiit = ExerciseLibrary.ofType<HiitExercise>()
        assertTrue(cardio.containsAll(hiit))
        assertTrue(ExerciseLibrary.ofType<FlexibilityExercise>().isNotEmpty())
    }

    @Test
    fun everyHealthMetricProducesText() {
        HealthMetrics.all.forEach { metric ->
            assertTrue(metric.formatted(profile()).isNotBlank())
            assertTrue(metric.interpret(profile()).isNotBlank())
        }
    }

    // --- Strategy / Factory / Template method ---

    @Test
    fun factoryPicksTheStrategyForTheGoal() {
        assertTrue(WorkoutPlanFactory.strategyFor(FitnessGoal.LOSE_WEIGHT) is FatLossStrategy)
        assertTrue(WorkoutPlanFactory.strategyFor(FitnessGoal.BUILD_MUSCLE) is MuscleGainStrategy)
    }

    @Test
    fun everyGoalGetsASevenDayPlan() {
        FitnessGoal.entries.forEach { goal ->
            val plan = WorkoutPlanFactory.createPlan(profile(goal))
            assertEquals(7, plan.sessions.size)
            assertEquals(goal, plan.goal)
            assertTrue(plan.activeDays in 5..6)
        }
    }

    // --- Tracking & suggestions ---

    @Test
    fun streakCountsConsecutiveDays() {
        val today = LocalDate.of(2026, 10, 29)
        val logs = listOf(logOn(today), logOn(today.minusDays(1)), logOn(today.minusDays(2)), logOn(today.minusDays(5)))
        val stats = WorkoutStats(logs, today)
        assertEquals(3, stats.currentStreak)
        assertEquals(4, stats.workoutsThisWeek)
        assertEquals(800.0, stats.caloriesThisWeek, 0.001)
    }

    @Test
    fun newUserIsNudgedToLogFirstWorkoutAndSuggestionsAreSorted() {
        val suggestions = SuggestionEngine().suggestionsFor(SuggestionContext(profile(), WorkoutStats(emptyList())))
        assertEquals("Log your first workout", suggestions.first().title)
        assertEquals(suggestions.sortedBy { it.priority.ordinal }, suggestions)
        assertTrue(suggestions.any { it.priority == Priority.LOW })
    }

    // --- CultFit-style day plan + YouTube videos ---

    @Test
    fun everyExerciseHasAYouTubeLink() {
        ExerciseLibrary.all.forEach { exercise ->
            assertTrue(exercise.youtubeUrl.startsWith("https://www.youtube.com/"))
            assertTrue(exercise.youtubeUrl.contains("youtube.com"))
        }
        assertEquals(23, ExerciseLibrary.all.size)
    }

    @Test
    fun scaledExerciseKeepsItsVideoLink() {
        val pushups = ExerciseLibrary["pushups"]
        val advanced = pushups.scaledFor(Difficulty.ADVANCED)
        assertEquals(pushups.youtubeUrl, advanced.youtubeUrl)
    }

    @Test
    fun planIsNumberedDay1ToDay7WithClearDayNames() {
        FitnessGoal.entries.forEach { goal ->
            val plan = WorkoutPlanFactory.createPlan(profile(goal))
            assertEquals((1..7).toList(), plan.sessions.map { it.dayNumber })
            assertEquals("Day 1 — ${plan.sessions[0].focus}", plan.sessions[0].dayLabel)
            assertEquals(plan.sessions[0], plan.sessionFor(1))
            assertTrue(plan.sessions[0].title.startsWith("Day 1 — "))
        }
        // The muscle plan is the classic split the user asked for.
        val musclePlan = WorkoutPlanFactory.createPlan(profile(FitnessGoal.BUILD_MUSCLE))
        assertEquals("Chest Day", musclePlan.sessions[0].focus)
        assertEquals("Back Day", musclePlan.sessions[1].focus)
        assertEquals("Leg Day", musclePlan.sessions[2].focus)
        assertTrue(musclePlan.sessions[6].isRestDay)
    }
}
