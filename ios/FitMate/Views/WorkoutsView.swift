import SwiftUI

struct WorkoutsView: View {
    enum Mode: String, CaseIterable, Identifiable {
        case plan = "My plan"
        case library = "Exercise library"

        var id: Self { self }
    }

    private static let categoryOptions: [ExerciseCategory?] = [nil] + ExerciseCategory.allCases.map { Optional($0) }

    @EnvironmentObject var store: FitnessStore
    let profile: UserProfile

    @State private var mode: Mode = .plan
    @State private var category: ExerciseCategory? = nil
    @State private var exerciseToLog: Exercise? = nil
    // CultFit-style day strip: Day 1 is Monday. Today is selected first.
    @State private var selectedDay: Int = Weekday.today.rawValue

    var body: some View {
        NavigationStack {
            List {
                Picker("View", selection: $mode) {
                    ForEach(Mode.allCases) { mode in
                        Text(mode.rawValue).tag(mode)
                    }
                }
                .pickerStyle(.segmented)
                .listRowBackground(Color.clear)

                if mode == .plan, let plan = store.plan {
                    Section {
                        PlanSummary(plan: plan, weightKg: profile.weightKg)
                    }
                    // Day 1–7 strip, like CultFit's weekly schedule.
                    Section("Pick a day") {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                ForEach(plan.sessions) { session in
                                    Button(session.isRestDay ? "Day \(session.dayNumber) · Rest" : "Day \(session.dayNumber) · \(session.day.shortName)") {
                                        selectedDay = session.dayNumber
                                    }
                                    .buttonStyle(.bordered)
                                    .tint(session.dayNumber == selectedDay ? Color.brand : .secondary)
                                }
                            }
                        }
                    }
                    if let day = plan.session(forDayNumber: selectedDay), !day.isRestDay {
                        Section(day.dayLabel) {
                            ForEach(day.exercises) { exercise in
                                DescribableRow(item: exercise) {
                                    HStack(spacing: 8) {
                                        if let url = URL(string: exercise.youtubeUrl) {
                                            Link("▶ Watch", destination: url)
                                        }
                                        Button("Log") { exerciseToLog = exercise }
                                            .buttonStyle(.bordered)
                                    }
                                }
                            }
                        }
                    }
                    Section("This week") {
                        ForEach(plan.sessions) { session in
                            SessionRow(session: session) { exerciseToLog = $0 }
                        }
                    }
                } else {
                    ChoiceChips(options: Self.categoryOptions, selection: $category) { option in
                        guard let option else { return "All" }
                        return "\(option.emoji) \(option.label)"
                    }
                    .listRowBackground(Color.clear)

                    ForEach(ExerciseLibrary.shared.byCategory(category)) { exercise in
                        ExerciseRow(exercise: exercise, weightKg: profile.weightKg) {
                            exerciseToLog = exercise
                        }
                    }
                }
            }
            .navigationTitle("Workouts")
            .sheet(item: $exerciseToLog) { exercise in
                LogWorkoutSheet(exercise: exercise, weightKg: profile.weightKg) { minutes in
                    store.logWorkout(exercise, minutes: minutes)
                }
            }
        }
    }
}

private struct PlanSummary: View {
    let plan: WorkoutPlan
    let weightKg: Double

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("\(plan.goal.emoji) \(plan.name)").font(.title3.bold())
            Text("Built for: \(plan.goal.label) · \(plan.level.label)")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            HStack {
                figure("\(plan.activeDays)", "active days")
                figure("\(plan.weeklyMinutes)", "min / week")
                figure("\(Int(plan.weeklyCalories(weightKg: weightKg).rounded()))", "kcal / week")
            }
            Text("Day-by-day split: Day 1 is Monday, Day 7 is Sunday. Tap a day above to see its exercises, then tap ▶ Watch to see how to do each move on YouTube. Change your goal or experience in Profile to get a new plan.")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(.vertical, 4)
    }

    private func figure(_ value: String, _ label: String) -> some View {
        VStack(alignment: .leading) {
            Text(value).font(.title3.bold())
            Text(label).font(.caption).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct SessionRow: View {
    let session: WorkoutSession
    let onLog: (Exercise) -> Void

    private var isToday: Bool { session.day == .today }

    var body: some View {
        if session.isRestDay {
            DescribableRow(item: session)
        } else {
            DisclosureGroup {
                ForEach(session.exercises) { exercise in
                    DescribableRow(item: exercise) {
                        HStack(spacing: 8) {
                            if let url = URL(string: exercise.youtubeUrl) {
                                Link("▶ Watch", destination: url)
                            }
                            Button("Log") { onLog(exercise) }
                                .buttonStyle(.bordered)
                        }
                    }
                }
            } label: {
                DescribableRow(item: session) {
                    if isToday {
                        Text("TODAY").font(.caption2.bold()).foregroundStyle(.teal)
                    }
                }
            }
        }
    }
}

private struct ExerciseRow: View {
    let exercise: Exercise
    let weightKg: Double
    let onLog: () -> Void

    private var minutes: Int { exercise.estimatedMinutes() }

    private var calories: Int {
        Int(exercise.caloriesBurned(weightKg: weightKg, minutes: minutes).rounded())
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            DescribableRow(item: exercise) {
                Button("Log", action: onLog)
                    .buttonStyle(.borderedProminent)
            }
            Text(exercise.details).font(.footnote)
            Text("\(exercise.category.label) · \(exercise.level.label) · ≈\(calories) kcal in \(minutes) min")
                .font(.caption.weight(.medium))
                .foregroundStyle(Color.brand)
            if let url = URL(string: exercise.youtubeUrl) {
                Link("▶ Watch how to do it on YouTube", destination: url)
                    .font(.subheadline.weight(.semibold))
            }
        }
        .padding(.vertical, 4)
    }
}
