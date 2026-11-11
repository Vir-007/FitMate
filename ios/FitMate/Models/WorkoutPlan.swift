import Foundation

/// Monday-first days of the week (Calendar uses 1 = Sunday, so we convert).
enum Weekday: Int, CaseIterable, Identifiable {
    case monday = 1, tuesday, wednesday, thursday, friday, saturday, sunday

    var id: Int { rawValue }

    var shortName: String {
        ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"][rawValue - 1]
    }

    static var today: Weekday {
        let calendarWeekday = Calendar.current.component(.weekday, from: Date()) // 1 = Sunday
        return Weekday(rawValue: (calendarWeekday + 5) % 7 + 1) ?? .monday
    }
}

/// COMPOSITION: a session *has* a list of exercises.
final class WorkoutSession: Identifiable, Describable {
    let day: Weekday
    let focus: String
    let exercises: [Exercise]
    /// Day 1–7 of the week, set by `BasePlanStrategy.buildPlan`. 0 means "not placed in a plan yet".
    let dayNumber: Int

    init(day: Weekday, focus: String, exercises: [Exercise], dayNumber: Int = 0) {
        self.day = day
        self.focus = focus
        self.exercises = exercises
        self.dayNumber = dayNumber
    }

    var id: Int { day.rawValue }

    /// CultFit-style label: "Day 1 — Chest Day". Falls back to just the focus when unplaced.
    var dayLabel: String {
        (1...7).contains(dayNumber) ? "Day \(dayNumber) — \(focus)" : focus
    }

    var isRestDay: Bool { exercises.isEmpty }

    func totalMinutes() -> Int {
        exercises.reduce(0) { $0 + $1.estimatedMinutes() }
    }

    /// Polymorphism: each exercise is asked for calories using its *own* formula.
    func estimatedCalories(weightKg: Double) -> Double {
        exercises.reduce(0.0) { total, exercise in
            total + exercise.caloriesBurned(weightKg: weightKg, minutes: exercise.estimatedMinutes())
        }
    }

    var title: String { "\(dayLabel) · \(day.shortName)" }

    var subtitle: String {
        isRestDay ? "Recovery day" : "\(exercises.count) exercises · ~\(totalMinutes()) min"
    }

    var emoji: String { exercises.first?.emoji ?? "😴" }
}

/// COMPOSITION again: a plan *has* seven sessions.
final class WorkoutPlan {
    let name: String
    let goal: FitnessGoal
    let level: Difficulty
    let sessions: [WorkoutSession]

    init(name: String, goal: FitnessGoal, level: Difficulty, sessions: [WorkoutSession]) {
        self.name = name
        self.goal = goal
        self.level = level
        self.sessions = sessions
    }

    var activeDays: Int { sessions.filter { !$0.isRestDay }.count }

    var weeklyMinutes: Int { sessions.reduce(0) { $0 + $1.totalMinutes() } }

    func weeklyCalories(weightKg: Double) -> Double {
        sessions.reduce(0.0) { $0 + $1.estimatedCalories(weightKg: weightKg) }
    }

    func session(for day: Weekday) -> WorkoutSession? {
        sessions.first { $0.day == day }
    }

    /// Day 1 is Monday, Day 7 is Sunday.
    func session(forDayNumber dayNumber: Int) -> WorkoutSession? {
        sessions.first { $0.dayNumber == dayNumber }
    }
}
