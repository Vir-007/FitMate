package com.fitmate.app.planner

import com.fitmate.app.model.Difficulty
import com.fitmate.app.model.ExerciseLibrary
import com.fitmate.app.model.FitnessGoal
import com.fitmate.app.model.FlexibilityExercise
import com.fitmate.app.model.UserProfile
import com.fitmate.app.model.WorkoutPlan
import com.fitmate.app.model.WorkoutSession
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY

/** STRATEGY pattern: each fitness goal has its own algorithm for building a week. */
interface PlanStrategy {
    val goal: FitnessGoal
    fun buildPlan(level: Difficulty): WorkoutPlan
}

/**
 * TEMPLATE METHOD pattern: [buildPlan] fixes the steps (get schedule → scale to level → wrap in a plan)
 * and is `final`, so subclasses can only fill in [weeklySchedule] and [planName].
 */
abstract class BasePlanStrategy : PlanStrategy {

    protected abstract val planName: String

    protected abstract fun weeklySchedule(): List<WorkoutSession>

    final override fun buildPlan(level: Difficulty): WorkoutPlan {
        // Day 1 is Monday, Day 7 is Sunday: the list order is the week order.
        val sessions = weeklySchedule().mapIndexed { index, session ->
            WorkoutSession(
                session.day, session.focus,
                session.exercises.map { it.scaledFor(level) },
                dayNumber = index + 1
            )
        }
        return WorkoutPlan(planName, goal, level, sessions)
    }

    // `protected` helpers: visible to subclasses, hidden from everyone else.
    protected fun day(day: DayOfWeek, focus: String, vararg exerciseIds: String) =
        WorkoutSession(day, focus, exerciseIds.map { ExerciseLibrary[it] })

    protected fun rest(day: DayOfWeek) = WorkoutSession(day, "Rest & recover", emptyList())
}

class FatLossStrategy : BasePlanStrategy() {
    override val goal = FitnessGoal.LOSE_WEIGHT
    override val planName = "Fat Burn Program"

    // CultFit-style week: every day has a clear name — Day 1 Chest Day, Day 2 Leg Day, …
    override fun weeklySchedule() = listOf(
        day(MONDAY, "Full-Body HIIT Day", "burpees", "squats", "pushups", "crunches"),
        day(TUESDAY, "Cardio Day", "running", "crunches", "hamstring_stretch"),
        day(WEDNESDAY, "Leg Day", "mountain_climbers", "lunges", "glute_bridge"),
        day(THURSDAY, "Upper-Body Day", "cycling", "rows", "shoulder_press", "dips"),
        day(FRIDAY, "Tabata Day", "tabata", "jump_rope", "childs_pose"),
        day(SATURDAY, "Recovery Yoga Day", "brisk_walk", "sun_salutation", "cat_cow"),
        rest(SUNDAY)
    )
}

class MuscleGainStrategy : BasePlanStrategy() {
    override val goal = FitnessGoal.BUILD_MUSCLE
    override val planName = "Push / Pull / Legs"

    override fun weeklySchedule() = listOf(
        day(MONDAY, "Chest Day", "pushups", "dips", "crunches"),
        day(TUESDAY, "Back Day", "rows", "curls", "deadlifts"),
        day(WEDNESDAY, "Leg Day", "squats", "lunges", "glute_bridge", "crunches"),
        day(THURSDAY, "Shoulder Day", "shoulder_press", "pushups", "dips"),
        day(FRIDAY, "Full-Body Day", "squats", "rows", "pushups", "deadlifts"),
        day(SATURDAY, "Mobility Day", "cat_cow", "hip_opener", "hamstring_stretch", "sun_salutation"),
        rest(SUNDAY)
    )
}

class GeneralFitnessStrategy : BasePlanStrategy() {
    override val goal = FitnessGoal.STAY_FIT
    override val planName = "Balanced Fitness"

    override fun weeklySchedule() = listOf(
        day(MONDAY, "Full-Body Day", "squats", "pushups", "rows", "crunches"),
        day(TUESDAY, "Cardio Day", "running", "hamstring_stretch"),
        day(WEDNESDAY, "Yoga Day", "sun_salutation", "cobra", "childs_pose"),
        day(THURSDAY, "HIIT Day", "mountain_climbers", "lunges", "glute_bridge"),
        day(FRIDAY, "Core & Cardio Day", "cycling", "crunches"),
        day(SATURDAY, "Legs & Walk Day", "brisk_walk", "hip_opener", "squats"),
        rest(SUNDAY)
    )
}

class FlexibilityStrategy : BasePlanStrategy() {
    override val goal = FitnessGoal.IMPROVE_FLEXIBILITY
    override val planName = "Mobility & Yoga"

    override fun weeklySchedule(): List<WorkoutSession> {
        // Generics in action: grab every stretch in the library without listing ids.
        val allStretches = ExerciseLibrary.ofType<FlexibilityExercise>()
        return listOf(
            day(MONDAY, "Yoga Day", "sun_salutation", "cat_cow", "cobra"),
            day(TUESDAY, "Hips Day", "brisk_walk", "hip_opener", "hamstring_stretch"),
            WorkoutSession(WEDNESDAY, "Full Stretch Day", allStretches),
            day(THURSDAY, "Core Day", "glute_bridge", "crunches", "cobra"),
            day(FRIDAY, "Yoga Flow Day", "sun_salutation", "childs_pose", "hip_opener"),
            day(SATURDAY, "Stretch & Cycle Day", "cycling", "hamstring_stretch"),
            rest(SUNDAY)
        )
    }
}

/** FACTORY pattern: callers ask for a plan and never need to know which strategy class built it. */
object WorkoutPlanFactory {

    fun strategyFor(goal: FitnessGoal): PlanStrategy = when (goal) {
        FitnessGoal.LOSE_WEIGHT -> FatLossStrategy()
        FitnessGoal.BUILD_MUSCLE -> MuscleGainStrategy()
        FitnessGoal.STAY_FIT -> GeneralFitnessStrategy()
        FitnessGoal.IMPROVE_FLEXIBILITY -> FlexibilityStrategy()
    }

    fun createPlan(profile: UserProfile): WorkoutPlan =
        strategyFor(profile.goal).buildPlan(profile.experience)
}
