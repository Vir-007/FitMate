import Foundation

/// STRATEGY pattern: each fitness goal has its own algorithm for building a week.
protocol PlanStrategy {
    var goal: FitnessGoal { get }
    func buildPlan(level: Difficulty) -> WorkoutPlan
}

/// TEMPLATE METHOD pattern: `buildPlan` fixes the steps (get schedule → scale to level → wrap in a plan)
/// and is `final`, so subclasses can only fill in `goal`, `planName` and `weeklySchedule()`.
/// Swift has no `protected`, so the helpers are internal and documented as "for subclasses".
class BasePlanStrategy: PlanStrategy {

    var goal: FitnessGoal { fatalError("Subclasses must override goal") }

    var planName: String { fatalError("Subclasses must override planName") }

    func weeklySchedule() -> [WorkoutSession] { fatalError("Subclasses must override weeklySchedule()") }

    final func buildPlan(level: Difficulty) -> WorkoutPlan {
        // Day 1 is Monday, Day 7 is Sunday: the list order is the week order.
        let sessions = weeklySchedule().enumerated().map { index, session in
            WorkoutSession(day: session.day, focus: session.focus,
                           exercises: session.exercises.map { $0.scaled(for: level) },
                           dayNumber: index + 1)
        }
        return WorkoutPlan(name: planName, goal: goal, level: level, sessions: sessions)
    }

    // Helpers for subclasses.
    func day(_ day: Weekday, _ focus: String, _ exerciseIds: String...) -> WorkoutSession {
        WorkoutSession(day: day, focus: focus, exercises: exerciseIds.map { ExerciseLibrary.shared[$0] })
    }

    func rest(_ day: Weekday) -> WorkoutSession {
        WorkoutSession(day: day, focus: "Rest & recover", exercises: [])
    }
}

final class FatLossStrategy: BasePlanStrategy {
    override var goal: FitnessGoal { .loseWeight }
    override var planName: String { "Fat Burn Program" }

    override func weeklySchedule() -> [WorkoutSession] {
        // CultFit-style week: every day has a clear name — Day 1 Chest Day, Day 2 Leg Day, …
        [
            day(.monday, "Full-Body HIIT Day", "burpees", "squats", "pushups", "crunches"),
            day(.tuesday, "Cardio Day", "running", "crunches", "hamstring_stretch"),
            day(.wednesday, "Leg Day", "mountain_climbers", "lunges", "glute_bridge"),
            day(.thursday, "Upper-Body Day", "cycling", "rows", "shoulder_press", "dips"),
            day(.friday, "Tabata Day", "tabata", "jump_rope", "childs_pose"),
            day(.saturday, "Recovery Yoga Day", "brisk_walk", "sun_salutation", "cat_cow"),
            rest(.sunday)
        ]
    }
}

final class MuscleGainStrategy: BasePlanStrategy {
    override var goal: FitnessGoal { .buildMuscle }
    override var planName: String { "Push / Pull / Legs" }

    override func weeklySchedule() -> [WorkoutSession] {
        [
            day(.monday, "Chest Day", "pushups", "dips", "crunches"),
            day(.tuesday, "Back Day", "rows", "curls", "deadlifts"),
            day(.wednesday, "Leg Day", "squats", "lunges", "glute_bridge", "crunches"),
            day(.thursday, "Shoulder Day", "shoulder_press", "pushups", "dips"),
            day(.friday, "Full-Body Day", "squats", "rows", "pushups", "deadlifts"),
            day(.saturday, "Mobility Day", "cat_cow", "hip_opener", "hamstring_stretch", "sun_salutation"),
            rest(.sunday)
        ]
    }
}

final class GeneralFitnessStrategy: BasePlanStrategy {
    override var goal: FitnessGoal { .stayFit }
    override var planName: String { "Balanced Fitness" }

    override func weeklySchedule() -> [WorkoutSession] {
        [
            day(.monday, "Full-Body Day", "squats", "pushups", "rows", "crunches"),
            day(.tuesday, "Cardio Day", "running", "hamstring_stretch"),
            day(.wednesday, "Yoga Day", "sun_salutation", "cobra", "childs_pose"),
            day(.thursday, "HIIT Day", "mountain_climbers", "lunges", "glute_bridge"),
            day(.friday, "Core & Cardio Day", "cycling", "crunches"),
            day(.saturday, "Legs & Walk Day", "brisk_walk", "hip_opener", "squats"),
            rest(.sunday)
        ]
    }
}

final class FlexibilityStrategy: BasePlanStrategy {
    override var goal: FitnessGoal { .improveFlexibility }
    override var planName: String { "Mobility & Yoga" }

    override func weeklySchedule() -> [WorkoutSession] {
        // Generics in action: grab every stretch in the library without listing ids.
        let allStretches: [Exercise] = ExerciseLibrary.shared.ofType(FlexibilityExercise.self)
        return [
            day(.monday, "Yoga Day", "sun_salutation", "cat_cow", "cobra"),
            day(.tuesday, "Hips Day", "brisk_walk", "hip_opener", "hamstring_stretch"),
            WorkoutSession(day: .wednesday, focus: "Full Stretch Day", exercises: allStretches),
            day(.thursday, "Core Day", "glute_bridge", "crunches", "cobra"),
            day(.friday, "Yoga Flow Day", "sun_salutation", "childs_pose", "hip_opener"),
            day(.saturday, "Stretch & Cycle Day", "cycling", "hamstring_stretch"),
            rest(.sunday)
        ]
    }
}

/// FACTORY pattern: callers ask for a plan and never need to know which strategy class built it.
enum WorkoutPlanFactory {
    static func strategy(for goal: FitnessGoal) -> any PlanStrategy {
        switch goal {
        case .loseWeight: return FatLossStrategy()
        case .buildMuscle: return MuscleGainStrategy()
        case .stayFit: return GeneralFitnessStrategy()
        case .improveFlexibility: return FlexibilityStrategy()
        }
    }

    static func createPlan(for profile: UserProfile) -> WorkoutPlan {
        strategy(for: profile.goal).buildPlan(level: profile.experience)
    }
}
