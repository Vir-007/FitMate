package com.fitmate.app.model

/**
 * ENCAPSULATION: the numeric fields can only be set through validating setters,
 * so a UserProfile can never hold an impossible height, weight or age.
 * Invalid input throws IllegalArgumentException, which the UI shows as an error.
 */
class UserProfile(
    name: String,
    age: Int,
    heightCm: Double,
    weightKg: Double,
    var gender: Gender = Gender.MALE,
    var activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    var goal: FitnessGoal = FitnessGoal.STAY_FIT,
    var experience: Difficulty = Difficulty.BEGINNER
) {
    var name: String = ""
        set(value) {
            require(value.isNotBlank()) { "Name can't be empty" }
            field = value.trim()
        }

    var age: Int = 0
        set(value) {
            require(value in MIN_AGE..MAX_AGE) { "Age must be between $MIN_AGE and $MAX_AGE" }
            field = value
        }

    var heightCm: Double = 0.0
        set(value) {
            require(value in MIN_HEIGHT..MAX_HEIGHT) {
                "Height must be between ${MIN_HEIGHT.toInt()} and ${MAX_HEIGHT.toInt()} cm"
            }
            field = value
        }

    var weightKg: Double = 0.0
        set(value) {
            require(value in MIN_WEIGHT..MAX_WEIGHT) {
                "Weight must be between ${MIN_WEIGHT.toInt()} and ${MAX_WEIGHT.toInt()} kg"
            }
            field = value
        }

    init {
        // Route constructor values through the setters so they get validated too.
        this.name = name
        this.age = age
        this.heightCm = heightCm
        this.weightKg = weightKg
    }

    /** Body-mass index = weight (kg) / height (m)². A read-only computed property. */
    val bmi: Double
        get() {
            val heightM = heightCm / 100.0
            return weightKg / (heightM * heightM)
        }

    val bmiCategory: BmiCategory
        get() = BmiCategory.from(bmi)

    /** Returns a new, validated profile with a different height/weight (used by the BMI calculator). */
    fun withBody(heightCm: Double, weightKg: Double): UserProfile =
        UserProfile(name, age, heightCm, weightKg, gender, activityLevel, goal, experience)

    companion object {
        const val MIN_AGE = 10
        const val MAX_AGE = 100
        const val MIN_HEIGHT = 80.0
        const val MAX_HEIGHT = 250.0
        const val MIN_WEIGHT = 20.0
        const val MAX_WEIGHT = 300.0
    }
}
