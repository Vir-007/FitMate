package com.fitmate.app.model

/*
 * Kotlin enums are full classes: every constant can carry its own data,
 * and the enum can have methods and a companion object.
 */

enum class Gender(val label: String, val bmrOffset: Int) {
    MALE("Male", 5),
    FEMALE("Female", -161)
}

enum class ActivityLevel(val label: String, val description: String, val multiplier: Double) {
    SEDENTARY("Sedentary", "Little or no exercise", 1.2),
    LIGHT("Light", "Exercise 1–3 days a week", 1.375),
    MODERATE("Moderate", "Exercise 3–5 days a week", 1.55),
    ACTIVE("Active", "Exercise 6–7 days a week", 1.725),
    ATHLETE("Athlete", "Hard training twice a day", 1.9)
}

enum class FitnessGoal(val label: String, val emoji: String, val calorieAdjustment: Int) {
    LOSE_WEIGHT("Lose weight", "🔥", -500),
    BUILD_MUSCLE("Build muscle", "💪", 300),
    STAY_FIT("Stay fit", "⚡", 0),
    IMPROVE_FLEXIBILITY("Flexibility", "🧘", 0)
}

enum class Difficulty(val label: String, val extraSets: Int, val durationFactor: Double) {
    BEGINNER("Beginner", 0, 0.8),
    INTERMEDIATE("Intermediate", 1, 1.0),
    ADVANCED("Advanced", 2, 1.3)
}

enum class MuscleGroup(val label: String) {
    CHEST("Chest"),
    BACK("Back"),
    LEGS("Legs"),
    ARMS("Arms"),
    SHOULDERS("Shoulders"),
    CORE("Core"),
    FULL_BODY("Full body")
}

enum class ExerciseCategory(val label: String, val emoji: String) {
    STRENGTH("Strength", "🏋️"),
    CARDIO("Cardio", "🏃"),
    HIIT("HIIT", "🔥"),
    FLEXIBILITY("Flexibility", "🧘")
}

enum class BmiCategory(val label: String, val advice: String) {
    UNDERWEIGHT("Underweight", "Focus on nutrient-dense meals and strength training."),
    NORMAL("Healthy", "Great work — keep up a balanced routine."),
    OVERWEIGHT("Overweight", "Regular cardio and a small calorie deficit will help."),
    OBESE("Obese", "Start with low-impact cardio and consider a doctor's advice.");

    companion object {
        /** Static-style factory method that picks the right constant for a BMI value. */
        fun from(bmi: Double): BmiCategory = when {
            bmi < 18.5 -> UNDERWEIGHT
            bmi < 25.0 -> NORMAL
            bmi < 30.0 -> OVERWEIGHT
            else -> OBESE
        }
    }
}
