import Foundation

/// ABSTRACTION + INHERITANCE root of the exercise hierarchy:
///
///     Exercise (abstract)
///      ├── StrengthExercise
///      ├── CardioExercise
///      │    └── HiitExercise
///      └── FlexibilityExercise
///
/// Swift has no `abstract` keyword, so the abstract members below stop the app with
/// `fatalError` — every subclass must override them.
class Exercise: Identifiable, Describable {
    let id: String
    let name: String
    let details: String
    let level: Difficulty
    /// Metabolic Equivalent of Task: how many times resting energy this activity burns.
    let met: Double
    /**
     * Optional link to one specific "how to do it" video on YouTube.
     * Leave it blank and `youtubeUrl` falls back to a YouTube search for
     * this exercise, so the link can never go dead. To pin a single video,
     * set this to its full https://www.youtube.com/watch?v=… URL in
     * `ExerciseLibrary` — that is the one place to change it.
     */
    let videoUrl: String

    init(id: String, name: String, details: String, level: Difficulty, met: Double, videoUrl: String = "") {
        self.id = id
        self.name = name
        self.details = details
        self.level = level
        self.met = met
        self.videoUrl = videoUrl
    }

    /// YouTube link for this exercise: the pinned video, or a YouTube search for it.
    var youtubeUrl: String {
        if !videoUrl.isEmpty { return videoUrl }
        let query = "\(name) correct form how to".replacingOccurrences(of: " ", with: "+")
        return "https://www.youtube.com/results?search_query=\(query)"
    }

    // MARK: Abstract members — subclasses must override

    var category: ExerciseCategory { fatalError("Subclasses must override category") }

    var subtitle: String { fatalError("Subclasses must override subtitle") }

    /// How long this exercise usually takes, in minutes.
    func estimatedMinutes() -> Int { fatalError("Subclasses must override estimatedMinutes()") }

    /// Returns a copy tuned to an experience level.
    func scaled(for level: Difficulty) -> Exercise { fatalError("Subclasses must override scaled(for:)") }

    // MARK: Shared behaviour

    /// Standard MET formula: kcal per minute = MET × 3.5 × weight(kg) / 200.
    /// Subclasses may override it (see HiitExercise).
    func caloriesBurned(weightKg: Double, minutes: Int) -> Double {
        met * 3.5 * weightKg / 200 * Double(minutes)
    }

    var title: String { name }
    var emoji: String { category.emoji }
}

final class StrengthExercise: Exercise {
    let muscleGroup: MuscleGroup
    let sets: Int
    let reps: Int

    init(id: String, name: String, details: String, level: Difficulty, met: Double,
         muscleGroup: MuscleGroup, sets: Int, reps: Int, videoUrl: String = "") {
        self.muscleGroup = muscleGroup
        self.sets = sets
        self.reps = reps
        super.init(id: id, name: name, details: details, level: level, met: met, videoUrl: videoUrl)
    }

    override var category: ExerciseCategory { .strength }

    override var subtitle: String { "\(sets) sets × \(reps) reps · \(muscleGroup.label)" }

    // About 4 seconds per rep plus 90 seconds of rest between sets.
    override func estimatedMinutes() -> Int {
        let seconds = sets * reps * 4 + (sets - 1) * 90
        return Int((Double(seconds) / 60).rounded(.up))
    }

    override func scaled(for level: Difficulty) -> Exercise {
        StrengthExercise(id: id, name: name, details: details, level: level, met: met,
                         muscleGroup: muscleGroup, sets: sets + level.extraSets, reps: reps, videoUrl: videoUrl)
    }
}

/// Not `final`, so that HiitExercise can inherit from it.
class CardioExercise: Exercise {
    let durationMinutes: Int

    init(id: String, name: String, details: String, level: Difficulty, met: Double, durationMinutes: Int, videoUrl: String = "") {
        self.durationMinutes = durationMinutes
        super.init(id: id, name: name, details: details, level: level, met: met, videoUrl: videoUrl)
    }

    override var category: ExerciseCategory { .cardio }

    override var subtitle: String { "\(durationMinutes) min" }

    override func estimatedMinutes() -> Int { durationMinutes }

    override func scaled(for level: Difficulty) -> Exercise {
        let minutes = Int((Double(durationMinutes) * level.durationFactor).rounded())
        return CardioExercise(id: id, name: name, details: details, level: level, met: met, durationMinutes: minutes, videoUrl: videoUrl)
    }
}

/// Multi-level inheritance: Exercise → CardioExercise → HiitExercise.
final class HiitExercise: CardioExercise {
    private static let afterburnBonus = 1.15

    let rounds: Int
    let workSeconds: Int
    let restSeconds: Int

    init(id: String, name: String, details: String, level: Difficulty, met: Double,
         rounds: Int, workSeconds: Int, restSeconds: Int, videoUrl: String = "") {
        self.rounds = rounds
        self.workSeconds = workSeconds
        self.restSeconds = restSeconds
        let minutes = Int((Double(rounds * (workSeconds + restSeconds)) / 60).rounded(.up))
        super.init(id: id, name: name, details: details, level: level, met: met, durationMinutes: minutes, videoUrl: videoUrl)
    }

    override var category: ExerciseCategory { .hiit }

    override var subtitle: String { "\(rounds) rounds · \(workSeconds)s on / \(restSeconds)s off" }

    /// HIIT keeps burning calories after you stop (the "afterburn"), so add a bonus to the parent's result.
    override func caloriesBurned(weightKg: Double, minutes: Int) -> Double {
        super.caloriesBurned(weightKg: weightKg, minutes: minutes) * HiitExercise.afterburnBonus
    }

    override func scaled(for level: Difficulty) -> Exercise {
        HiitExercise(id: id, name: name, details: details, level: level, met: met,
                     rounds: rounds + level.extraSets * 2, workSeconds: workSeconds, restSeconds: restSeconds, videoUrl: videoUrl)
    }
}

final class FlexibilityExercise: Exercise {
    let holdSeconds: Int
    let rounds: Int

    init(id: String, name: String, details: String, level: Difficulty, met: Double,
         holdSeconds: Int, rounds: Int, videoUrl: String = "") {
        self.holdSeconds = holdSeconds
        self.rounds = rounds
        super.init(id: id, name: name, details: details, level: level, met: met, videoUrl: videoUrl)
    }

    override var category: ExerciseCategory { .flexibility }

    override var subtitle: String { "Hold \(holdSeconds)s × \(rounds)" }

    // Both sides of the body, so each round is done twice.
    override func estimatedMinutes() -> Int {
        max(1, Int((Double(holdSeconds * rounds * 2) / 60).rounded(.up)))
    }

    override func scaled(for level: Difficulty) -> Exercise {
        FlexibilityExercise(id: id, name: name, details: details, level: level, met: met,
                            holdSeconds: holdSeconds, rounds: rounds + level.extraSets, videoUrl: videoUrl)
    }
}
