package com.fitmate.app

import com.fitmate.app.model.*
import java.util.prefs.Preferences

object ProfileStore {

    private val preferences =
        Preferences.userRoot().node("com/fitmate/desktop/profile")

    private fun defaultProfile() = UserProfile(
        name = "Alex Morgan",
        age = 22,
        heightCm = 175.0,
        weightKg = 72.0,
        gender = Gender.MALE,
        activityLevel = ActivityLevel.MODERATE,
        goal = FitnessGoal.STAY_FIT,
        experience = Difficulty.INTERMEDIATE
    )

    fun load(): UserProfile {
        return runCatching {
            UserProfile(
                name = preferences.get("name", "Alex Morgan"),
                age = preferences.getInt("age", 22),
                heightCm = preferences.getDouble("heightCm", 175.0),
                weightKg = preferences.getDouble("weightKg", 72.0),
                gender = Gender.valueOf(
                    preferences.get("gender", Gender.MALE.name)
                ),
                activityLevel = ActivityLevel.valueOf(
                    preferences.get("activityLevel", ActivityLevel.MODERATE.name)
                ),
                goal = FitnessGoal.valueOf(
                    preferences.get("goal", FitnessGoal.STAY_FIT.name)
                ),
                experience = Difficulty.valueOf(
                    preferences.get("experience", Difficulty.INTERMEDIATE.name)
                )
            )
        }.getOrElse {
            defaultProfile()
        }
    }

    fun save(profile: UserProfile) {
        preferences.put("name", profile.name)
        preferences.putInt("age", profile.age)
        preferences.putDouble("heightCm", profile.heightCm)
        preferences.putDouble("weightKg", profile.weightKg)
        preferences.put("gender", profile.gender.name)
        preferences.put("activityLevel", profile.activityLevel.name)
        preferences.put("goal", profile.goal.name)
        preferences.put("experience", profile.experience.name)
        preferences.flush()
    }
}
