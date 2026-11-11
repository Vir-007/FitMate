package com.fitmate.app.model

import com.fitmate.app.model.Difficulty.ADVANCED
import com.fitmate.app.model.Difficulty.BEGINNER
import com.fitmate.app.model.Difficulty.INTERMEDIATE

/**
 * SINGLETON: Kotlin's `object` keyword creates exactly one shared instance,
 * so the whole app reads from the same exercise catalogue.
 *
 * VIDEOS: every exercise gets a YouTube link from [Exercise.youtubeUrl].
 * By default that is a YouTube search for "how to do <exercise> with correct
 * form", so the link never goes dead. To pin one specific video for an
 * exercise, pass its full watch URL as `videoUrl = "https://www.youtube.com/watch?v=…"`
 * on that exercise below — that is the one place to change it.
 */
object ExerciseLibrary {

    val all: List<Exercise> = listOf(
        // Strength
        StrengthExercise("pushups", "Push-ups", "Keep a straight line from head to heels and lower your chest to the floor.", BEGINNER, 3.8, MuscleGroup.CHEST, sets = 3, reps = 12),
        StrengthExercise("squats", "Bodyweight Squats", "Feet shoulder-width apart, sit back as if into a chair, keep your chest up.", BEGINNER, 5.0, MuscleGroup.LEGS, sets = 3, reps = 15),
        StrengthExercise("lunges", "Walking Lunges", "Step forward and lower until both knees bend to 90°, then step through.", BEGINNER, 4.0, MuscleGroup.LEGS, sets = 3, reps = 12),
        StrengthExercise("glute_bridge", "Glute Bridges", "Lie on your back, feet flat, and drive your hips up by squeezing your glutes.", BEGINNER, 3.5, MuscleGroup.LEGS, sets = 3, reps = 15),
        StrengthExercise("crunches", "Crunches", "Curl your shoulders off the floor using your abs, not your neck.", BEGINNER, 3.8, MuscleGroup.CORE, sets = 3, reps = 20),
        StrengthExercise("curls", "Bicep Curls", "Keep elbows pinned to your sides and curl the weights up slowly.", BEGINNER, 3.5, MuscleGroup.ARMS, sets = 3, reps = 12),
        StrengthExercise("rows", "Dumbbell Rows", "Hinge forward with a flat back and pull the weight to your hip.", INTERMEDIATE, 5.0, MuscleGroup.BACK, sets = 3, reps = 10),
        StrengthExercise("shoulder_press", "Shoulder Press", "Press the weights overhead without arching your lower back.", INTERMEDIATE, 5.0, MuscleGroup.SHOULDERS, sets = 3, reps = 10),
        StrengthExercise("dips", "Tricep Dips", "Use a sturdy chair; lower until elbows reach 90°, then push up.", INTERMEDIATE, 3.8, MuscleGroup.ARMS, sets = 3, reps = 10),
        StrengthExercise("deadlifts", "Romanian Deadlifts", "Hinge at the hips with soft knees and a neutral spine.", ADVANCED, 6.0, MuscleGroup.FULL_BODY, sets = 3, reps = 8),

        // Cardio
        CardioExercise("brisk_walk", "Brisk Walk", "Walk fast enough that talking takes a little effort.", BEGINNER, 4.3, durationMinutes = 30),
        CardioExercise("cycling", "Cycling", "Steady pace outdoors or on a stationary bike.", BEGINNER, 7.5, durationMinutes = 30),
        CardioExercise("running", "Running", "Comfortable pace; land softly under your hips.", INTERMEDIATE, 9.8, durationMinutes = 20),
        CardioExercise("jump_rope", "Jump Rope", "Small, quick hops on the balls of your feet.", INTERMEDIATE, 11.8, durationMinutes = 10),

        // HIIT
        HiitExercise("mountain_climbers", "Mountain Climbers", "From a plank, drive your knees to your chest as fast as you can.", INTERMEDIATE, 8.0, rounds = 8, workSeconds = 40, restSeconds = 20),
        HiitExercise("tabata", "Tabata Circuit", "Squat jumps, high knees and push-ups — 20 s all-out, 10 s rest.", INTERMEDIATE, 9.0, rounds = 16, workSeconds = 20, restSeconds = 10),
        HiitExercise("burpees", "Burpee Blast", "Squat, kick back to plank, push-up, jump up. Repeat.", ADVANCED, 8.0, rounds = 10, workSeconds = 30, restSeconds = 30),

        // Flexibility
        FlexibilityExercise("sun_salutation", "Sun Salutation", "Flow through the yoga sequence, moving with your breath.", BEGINNER, 3.3, holdSeconds = 30, rounds = 5),
        FlexibilityExercise("cat_cow", "Cat-Cow Flow", "On all fours, alternate arching and rounding your spine.", BEGINNER, 2.5, holdSeconds = 30, rounds = 4),
        FlexibilityExercise("cobra", "Cobra Stretch", "Lie face down and gently press your chest up, hips on the floor.", BEGINNER, 2.3, holdSeconds = 20, rounds = 4),
        FlexibilityExercise("hamstring_stretch", "Hamstring Stretch", "Hinge forward over a straight leg until you feel a gentle pull.", BEGINNER, 2.3, holdSeconds = 30, rounds = 3),
        FlexibilityExercise("hip_opener", "Hip Flexor Stretch", "Kneel in a lunge and push your hips forward gently.", BEGINNER, 2.3, holdSeconds = 30, rounds = 3),
        FlexibilityExercise("childs_pose", "Child's Pose", "Sit back on your heels and reach your arms forward on the floor.", BEGINNER, 2.0, holdSeconds = 45, rounds = 3)
    )

    private val byId: Map<String, Exercise> = all.associateBy { it.id }

    /** Operator overloading: lets callers write `ExerciseLibrary["squats"]`. */
    operator fun get(id: String): Exercise = byId[id] ?: error("Unknown exercise id: $id")

    /**
     * GENERICS: `ofType<CardioExercise>()` returns every cardio exercise —
     * including HIIT ones, because a HiitExercise *is a* CardioExercise.
     */
    inline fun <reified T : Exercise> ofType(): List<T> = all.filterIsInstance<T>()

    fun byCategory(category: ExerciseCategory?): List<Exercise> =
        if (category == null) all else all.filter { it.category == category }
}
