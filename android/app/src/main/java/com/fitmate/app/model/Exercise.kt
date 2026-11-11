package com.fitmate.app.model

import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * ABSTRACTION + INHERITANCE root of the exercise hierarchy:
 *
 *   Exercise (abstract)
 *    ├── StrengthExercise
 *    ├── CardioExercise (open)
 *    │    └── HiitExercise
 *    └── FlexibilityExercise
 *
 * You can't create a plain Exercise; only its concrete subclasses.
 */
abstract class Exercise(
    override val id: String,
    val name: String,
    val description: String,
    val level: Difficulty,
    /** Metabolic Equivalent of Task: how many times resting energy this activity burns. */
    val met: Double,
    /**
     * Optional link to one specific "how to do it" video on YouTube.
     * Leave it blank and [youtubeUrl] falls back to a YouTube search for
     * this exercise, so the link can never go dead. To pin a single video,
     * set this to its full https://www.youtube.com/watch?v=… URL in
     * [ExerciseLibrary] — that is the one place to change it.
     */
    val videoUrl: String = ""
) : Identifiable, Describable {

    abstract val category: ExerciseCategory

    /** YouTube link for this exercise: the pinned video, or a YouTube search for it. */
    val youtubeUrl: String
        get() = videoUrl.ifBlank {
            "https://www.youtube.com/results?search_query=" +
                ("$name correct form how to").replace(" ", "+")
        }

    abstract val category: ExerciseCategory

    /** How long this exercise usually takes, in minutes. Each subclass works it out differently. */
    abstract fun estimatedMinutes(): Int

    /** Returns a copy tuned to an experience level. Overrides return their own subclass type. */
    abstract fun scaledFor(level: Difficulty): Exercise

    /**
     * Standard MET formula: kcal per minute = MET × 3.5 × weight(kg) / 200.
     * `open` so subclasses can refine it (see [HiitExercise]).
     */
    open fun caloriesBurned(weightKg: Double, minutes: Int): Double =
        met * 3.5 * weightKg / 200.0 * minutes

    override val title: String get() = name
    override val emoji: String get() = category.emoji
}

class StrengthExercise(
    id: String,
    name: String,
    description: String,
    level: Difficulty,
    met: Double,
    val muscleGroup: MuscleGroup,
    val sets: Int,
    val reps: Int,
    videoUrl: String = ""
) : Exercise(id, name, description, level, met, videoUrl) {

    override val category: ExerciseCategory get() = ExerciseCategory.STRENGTH

    override val subtitle: String get() = "$sets sets × $reps reps · ${muscleGroup.label}"

    // About 4 seconds per rep plus 90 seconds of rest between sets.
    override fun estimatedMinutes(): Int =
        ceil((sets * reps * 4 + (sets - 1) * 90) / 60.0).toInt()

    override fun scaledFor(level: Difficulty): StrengthExercise =
        StrengthExercise(id, name, description, level, met, muscleGroup, sets + level.extraSets, reps, videoUrl)
}

/** `open` so that [HiitExercise] can extend it. */
open class CardioExercise(
    id: String,
    name: String,
    description: String,
    level: Difficulty,
    met: Double,
    val durationMinutes: Int,
    videoUrl: String = ""
) : Exercise(id, name, description, level, met, videoUrl) {

    override val category: ExerciseCategory get() = ExerciseCategory.CARDIO

    override val subtitle: String get() = "$durationMinutes min"

    override fun estimatedMinutes(): Int = durationMinutes

    override fun scaledFor(level: Difficulty): CardioExercise =
        CardioExercise(id, name, description, level, met, (durationMinutes * level.durationFactor).roundToInt(), videoUrl)
}

/** Multi-level inheritance: Exercise → CardioExercise → HiitExercise. */
class HiitExercise(
    id: String,
    name: String,
    description: String,
    level: Difficulty,
    met: Double,
    val rounds: Int,
    val workSeconds: Int,
    val restSeconds: Int,
    videoUrl: String = ""
) : CardioExercise(
    id, name, description, level, met,
    durationMinutes = ceil(rounds * (workSeconds + restSeconds) / 60.0).toInt(),
    videoUrl = videoUrl
) {

    override val category: ExerciseCategory get() = ExerciseCategory.HIIT

    override val subtitle: String get() = "$rounds rounds · ${workSeconds}s on / ${restSeconds}s off"

    /** HIIT keeps burning calories after you stop (the "afterburn"), so add a bonus to the parent's result. */
    override fun caloriesBurned(weightKg: Double, minutes: Int): Double =
        super.caloriesBurned(weightKg, minutes) * AFTERBURN_BONUS

    override fun scaledFor(level: Difficulty): HiitExercise =
        HiitExercise(id, name, description, level, met, rounds + level.extraSets * 2, workSeconds, restSeconds, videoUrl)

    companion object {
        private const val AFTERBURN_BONUS = 1.15
    }
}

class FlexibilityExercise(
    id: String,
    name: String,
    description: String,
    level: Difficulty,
    met: Double,
    val holdSeconds: Int,
    val rounds: Int,
    videoUrl: String = ""
) : Exercise(id, name, description, level, met, videoUrl) {

    override val category: ExerciseCategory get() = ExerciseCategory.FLEXIBILITY

    override val subtitle: String get() = "Hold ${holdSeconds}s × $rounds"

    // Both sides of the body, so each round is done twice.
    override fun estimatedMinutes(): Int =
        maxOf(1, ceil(holdSeconds * rounds * 2 / 60.0).toInt())

    override fun scaledFor(level: Difficulty): FlexibilityExercise =
        FlexibilityExercise(id, name, description, level, met, holdSeconds, rounds + level.extraSets, videoUrl)
}
