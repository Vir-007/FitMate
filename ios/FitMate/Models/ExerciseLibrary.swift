import Foundation

/// SINGLETON: `shared` is the one and only instance — the private init stops anyone creating another.
///
/// VIDEOS: every exercise gets a YouTube link from `Exercise.youtubeUrl`.
/// By default that is a YouTube search for "how to do <exercise> with correct
/// form", so the link never goes dead. To pin one specific video for an
/// exercise, pass its full watch URL as `videoUrl: "https://www.youtube.com/watch?v=…"`
/// on that exercise below — that is the one place to change it.
final class ExerciseLibrary {
    static let shared = ExerciseLibrary()

    let all: [Exercise]
    private let byId: [String: Exercise]

    private init() {
        let catalogue: [Exercise] = [
            // Strength
            StrengthExercise(id: "pushups", name: "Push-ups", details: "Keep a straight line from head to heels and lower your chest to the floor.", level: .beginner, met: 3.8, muscleGroup: .chest, sets: 3, reps: 12),
            StrengthExercise(id: "squats", name: "Bodyweight Squats", details: "Feet shoulder-width apart, sit back as if into a chair, keep your chest up.", level: .beginner, met: 5.0, muscleGroup: .legs, sets: 3, reps: 15),
            StrengthExercise(id: "lunges", name: "Walking Lunges", details: "Step forward and lower until both knees bend to 90°, then step through.", level: .beginner, met: 4.0, muscleGroup: .legs, sets: 3, reps: 12),
            StrengthExercise(id: "glute_bridge", name: "Glute Bridges", details: "Lie on your back, feet flat, and drive your hips up by squeezing your glutes.", level: .beginner, met: 3.5, muscleGroup: .legs, sets: 3, reps: 15),
            StrengthExercise(id: "crunches", name: "Crunches", details: "Curl your shoulders off the floor using your abs, not your neck.", level: .beginner, met: 3.8, muscleGroup: .core, sets: 3, reps: 20),
            StrengthExercise(id: "curls", name: "Bicep Curls", details: "Keep elbows pinned to your sides and curl the weights up slowly.", level: .beginner, met: 3.5, muscleGroup: .arms, sets: 3, reps: 12),
            StrengthExercise(id: "rows", name: "Dumbbell Rows", details: "Hinge forward with a flat back and pull the weight to your hip.", level: .intermediate, met: 5.0, muscleGroup: .back, sets: 3, reps: 10),
            StrengthExercise(id: "shoulder_press", name: "Shoulder Press", details: "Press the weights overhead without arching your lower back.", level: .intermediate, met: 5.0, muscleGroup: .shoulders, sets: 3, reps: 10),
            StrengthExercise(id: "dips", name: "Tricep Dips", details: "Use a sturdy chair; lower until elbows reach 90°, then push up.", level: .intermediate, met: 3.8, muscleGroup: .arms, sets: 3, reps: 10),
            StrengthExercise(id: "deadlifts", name: "Romanian Deadlifts", details: "Hinge at the hips with soft knees and a neutral spine.", level: .advanced, met: 6.0, muscleGroup: .fullBody, sets: 3, reps: 8),

            // Cardio
            CardioExercise(id: "brisk_walk", name: "Brisk Walk", details: "Walk fast enough that talking takes a little effort.", level: .beginner, met: 4.3, durationMinutes: 30),
            CardioExercise(id: "cycling", name: "Cycling", details: "Steady pace outdoors or on a stationary bike.", level: .beginner, met: 7.5, durationMinutes: 30),
            CardioExercise(id: "running", name: "Running", details: "Comfortable pace; land softly under your hips.", level: .intermediate, met: 9.8, durationMinutes: 20),
            CardioExercise(id: "jump_rope", name: "Jump Rope", details: "Small, quick hops on the balls of your feet.", level: .intermediate, met: 11.8, durationMinutes: 10),

            // HIIT
            HiitExercise(id: "mountain_climbers", name: "Mountain Climbers", details: "From a plank, drive your knees to your chest as fast as you can.", level: .intermediate, met: 8.0, rounds: 8, workSeconds: 40, restSeconds: 20),
            HiitExercise(id: "tabata", name: "Tabata Circuit", details: "Squat jumps, high knees and push-ups — 20 s all-out, 10 s rest.", level: .intermediate, met: 9.0, rounds: 16, workSeconds: 20, restSeconds: 10),
            HiitExercise(id: "burpees", name: "Burpee Blast", details: "Squat, kick back to plank, push-up, jump up. Repeat.", level: .advanced, met: 8.0, rounds: 10, workSeconds: 30, restSeconds: 30),

            // Flexibility
            FlexibilityExercise(id: "sun_salutation", name: "Sun Salutation", details: "Flow through the yoga sequence, moving with your breath.", level: .beginner, met: 3.3, holdSeconds: 30, rounds: 5),
            FlexibilityExercise(id: "cat_cow", name: "Cat-Cow Flow", details: "On all fours, alternate arching and rounding your spine.", level: .beginner, met: 2.5, holdSeconds: 30, rounds: 4),
            FlexibilityExercise(id: "cobra", name: "Cobra Stretch", details: "Lie face down and gently press your chest up, hips on the floor.", level: .beginner, met: 2.3, holdSeconds: 20, rounds: 4),
            FlexibilityExercise(id: "hamstring_stretch", name: "Hamstring Stretch", details: "Hinge forward over a straight leg until you feel a gentle pull.", level: .beginner, met: 2.3, holdSeconds: 30, rounds: 3),
            FlexibilityExercise(id: "hip_opener", name: "Hip Flexor Stretch", details: "Kneel in a lunge and push your hips forward gently.", level: .beginner, met: 2.3, holdSeconds: 30, rounds: 3),
            FlexibilityExercise(id: "childs_pose", name: "Child's Pose", details: "Sit back on your heels and reach your arms forward on the floor.", level: .beginner, met: 2.0, holdSeconds: 45, rounds: 3)
        ]
        all = catalogue
        byId = Dictionary(uniqueKeysWithValues: catalogue.map { ($0.id, $0) })
    }

    /// Subscript: lets callers write `ExerciseLibrary.shared["squats"]`.
    subscript(id: String) -> Exercise {
        guard let exercise = byId[id] else { fatalError("Unknown exercise id: \(id)") }
        return exercise
    }

    /// GENERICS: `ofType(CardioExercise.self)` returns every cardio exercise —
    /// including HIIT ones, because a HiitExercise *is a* CardioExercise.
    func ofType<T: Exercise>(_ type: T.Type) -> [T] {
        all.compactMap { $0 as? T }
    }

    func byCategory(_ category: ExerciseCategory?) -> [Exercise] {
        guard let category else { return all }
        return all.filter { $0.category == category }
    }
}
