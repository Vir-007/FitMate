import SwiftUI
import Charts

struct HomeView: View {
    @EnvironmentObject var store: FitnessStore
    let profile: UserProfile
    let openWorkouts: () -> Void

    var body: some View {
        let stats = store.stats
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(Date().formatted(.dateTime.weekday(.wide).day().month(.wide)))
                        .foregroundStyle(.secondary)
                    HStack(spacing: 10) {
                        StatTile(emoji: "🔥", value: "\(stats.currentStreak)", label: "day streak")
                        StatTile(emoji: "🏋️", value: "\(stats.workoutsThisWeek)", label: "workouts · 7d")
                        StatTile(emoji: "⚡", value: "\(Int(stats.caloriesThisWeek.rounded()))", label: "kcal · 7d")
                    }
                    WeeklyActivityCard(stats: stats)
                    TodayCard(session: store.plan?.session(for: .today), openWorkouts: openWorkouts)

                    SectionTitle("Suggestions for you")
                    ForEach(store.suggestions) { suggestion in
                        SuggestionCard(suggestion: suggestion)
                    }

                    SectionTitle("Recent activity")
                    if store.logs.isEmpty {
                        Text("No workouts logged yet. Your history will show up here.")
                            .foregroundStyle(.secondary)
                    } else {
                        ForEach(store.logs.prefix(5)) { log in
                            CardView {
                                DescribableRow(item: log) {
                                    Button(role: .destructive) {
                                        store.deleteLog(log)
                                    } label: {
                                        Image(systemName: "trash")
                                    }
                                    .buttonStyle(.borderless)
                                }
                            }
                        }
                    }
                }
                .padding()
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitle("Hi, \(profile.name) 👋")
        }
    }
}

private struct WeeklyActivityCard: View {
    let stats: WorkoutStats

    var body: some View {
        CardView {
            VStack(alignment: .leading, spacing: 8) {
                Text("Last 7 days").font(.headline)
                Text("\(stats.minutesThisWeek) of \(WorkoutStats.weeklyMinutesTarget) active minutes")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                ProgressView(value: stats.weeklyTargetProgress)
                Chart(stats.dailyCalories()) { day in
                    BarMark(
                        x: .value("Day", day.date, unit: .day),
                        y: .value("kcal", day.calories)
                    )
                    .foregroundStyle(Color.brand.gradient)
                    .cornerRadius(4)
                }
                .chartXAxis {
                    AxisMarks(values: .stride(by: .day)) { _ in
                        AxisValueLabel(format: .dateTime.weekday(.narrow))
                    }
                }
                .frame(height: 150)
                Text("Calories burned per day")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
    }
}

private struct TodayCard: View {
    let session: WorkoutSession?
    let openWorkouts: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("TODAY'S PLAN").font(.caption.bold()).foregroundStyle(.secondary)
            if let session {
                Text("\(session.emoji) \(session.dayLabel)").font(.title2.bold())
                Text(session.subtitle)
                if !session.isRestDay {
                    Text(session.exercises.map(\.name).joined(separator: " · "))
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                    HStack(spacing: 8) {
                        Button("Start workout", action: openWorkouts)
                            .buttonStyle(.borderedProminent)
                        if let first = session.exercises.first,
                           let url = URL(string: first.youtubeUrl) {
                            Link("▶ Watch", destination: url)
                        }
                    }
                    .padding(.top, 4)
                }
            } else {
                Text("No session planned today.")
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.purple.opacity(0.12), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}
