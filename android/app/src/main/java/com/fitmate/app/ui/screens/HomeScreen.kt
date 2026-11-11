package com.fitmate.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitmate.app.model.UserProfile
import com.fitmate.app.model.WorkoutSession
import com.fitmate.app.tracking.WorkoutStats
import com.fitmate.app.ui.components.DescribableRow
import com.fitmate.app.ui.components.ScreenHeader
import com.fitmate.app.ui.components.SectionTitle
import com.fitmate.app.ui.components.StatCard
import com.fitmate.app.ui.components.SuggestionCard
import com.fitmate.app.ui.components.WeeklyBarChart
import com.fitmate.app.viewmodel.FitnessViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun HomeScreen(viewModel: FitnessViewModel, profile: UserProfile, onOpenWorkouts: () -> Unit) {
    val stats = viewModel.stats
    val today = LocalDate.now()
    val todaySession = viewModel.plan?.sessionFor(today.dayOfWeek)

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = "Hi, ${profile.name} 👋",
                subtitle = today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("🔥", "${stats.currentStreak}", "day streak", Modifier.weight(1f))
                StatCard("🏋️", "${stats.workoutsThisWeek}", "workouts · 7d", Modifier.weight(1f))
                StatCard("⚡", "${stats.caloriesThisWeek.roundToInt()}", "kcal · 7d", Modifier.weight(1f))
            }
        }
        item { WeeklyActivityCard(stats) }
        item { TodayCard(todaySession, onOpenWorkouts) }

        item { SectionTitle("Suggestions for you") }
        items(viewModel.suggestions) { SuggestionCard(it) }

        item { SectionTitle("Recent activity") }
        if (viewModel.logs.isEmpty()) {
            item {
                Text(
                    "No workouts logged yet. Your history will show up here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(viewModel.logs.take(5), key = { it.id }) { log ->
                Card(Modifier.fillMaxWidth()) {
                    DescribableRow(log, Modifier.padding(horizontal = 12.dp)) {
                        IconButton(onClick = { viewModel.deleteLog(log) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete workout")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyActivityCard(stats: WorkoutStats) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Last 7 days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "${stats.minutesThisWeek} of ${WorkoutStats.WEEKLY_MINUTES_TARGET} active minutes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { stats.weeklyTargetProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
            Spacer(Modifier.height(12.dp))
            WeeklyBarChart(stats.dailyCalories())
            Text(
                "Calories burned per day",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TodayCard(session: WorkoutSession?, onOpenWorkouts: () -> Unit) {
    val context = LocalContext.current
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("TODAY'S PLAN", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            if (session == null) {
                Text("No session planned today.")
            } else {
                Text(
                    "${session.emoji} ${session.dayLabel}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(session.subtitle)
                if (!session.isRestDay) {
                    Text(
                        session.exercises.joinToString(" · ") { it.name },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onOpenWorkouts) { Text("Start workout") }
                        OutlinedButton(onClick = {
                            val first = session.exercises.first()
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(first.youtubeUrl))
                            )
                        }) { Text("▶ Watch") }
                    }
                }
            }
        }
    }
}
