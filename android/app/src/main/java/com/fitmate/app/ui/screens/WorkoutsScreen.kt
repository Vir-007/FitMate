package com.fitmate.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitmate.app.tutorial.Tutorial
import com.fitmate.app.tutorial.TutorialApiClient
import com.fitmate.app.tutorial.TutorialWebPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.fitmate.app.model.Exercise
import com.fitmate.app.model.ExerciseCategory
import com.fitmate.app.model.ExerciseLibrary
import com.fitmate.app.model.UserProfile
import com.fitmate.app.model.WorkoutPlan
import com.fitmate.app.model.WorkoutSession
import com.fitmate.app.ui.components.ChoiceChips
import com.fitmate.app.ui.components.DescribableRow
import com.fitmate.app.ui.components.LogWorkoutDialog
import com.fitmate.app.ui.components.ScreenHeader
import com.fitmate.app.viewmodel.FitnessViewModel
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun WorkoutsScreen(viewModel: FitnessViewModel, profile: UserProfile) {
    val context = LocalContext.current
    var showLibrary by rememberSaveable { mutableStateOf(false) }
    var category by rememberSaveable { mutableStateOf<ExerciseCategory?>(null) }
    var exerciseToLog by remember { mutableStateOf<Exercise?>(null) }
    var tutorialExercise by remember { mutableStateOf<Exercise?>(null) }
    var tutorial by remember { mutableStateOf<Tutorial?>(null) }
    var tutorialLoading by remember { mutableStateOf(false) }
    var tutorialError by remember { mutableStateOf<String?>(null) }
    val tutorialScope = rememberCoroutineScope()

    val plan = viewModel.plan

    // CultFit-style day strip: Day 1 is Monday. Today is selected first.
    var selectedDay by rememberSaveable { mutableStateOf(LocalDate.now().dayOfWeek.value) }

    val openVideo: (Exercise) -> Unit = { exercise ->
        tutorialExercise = exercise
        tutorial = null
        tutorialError = null
        tutorialLoading = true

        tutorialScope.launch {
            try {
                tutorial = withContext(Dispatchers.IO) {
                    TutorialApiClient.searchTutorial(exercise.name)
                }
            } catch (exception: Exception) {
                tutorialError = exception.message ?: "Unable to load tutorial."
            } finally {
                tutorialLoading = false
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenHeader("Workouts", "Your day-by-day plan — tap a day, watch each move on YouTube") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !showLibrary, onClick = { showLibrary = false }, label = { Text("My plan") })
                FilterChip(selected = showLibrary, onClick = { showLibrary = true }, label = { Text("Exercise library") })
            }
        }

        if (!showLibrary && plan != null) {
            item { PlanSummaryCard(plan, profile.weightKg) }
            // Day 1–7 strip, like CultFit's weekly schedule.
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    plan.sessions.forEach { session ->
                        FilterChip(
                            selected = session.dayNumber == selectedDay,
                            onClick = { selectedDay = session.dayNumber },
                            label = {
                                Text(
                                    if (session.isRestDay) "Day ${session.dayNumber} · Rest"
                                    else "Day ${session.dayNumber} · ${session.dayName}"
                                )
                            }
                        )
                    }
                }
            }
            items(plan.sessions, key = { it.day.name }) { session ->
                SessionCard(
                    session = session,
                    isToday = session.day == LocalDate.now().dayOfWeek,
                    expanded = session.dayNumber == selectedDay,
                    onToggle = { selectedDay = session.dayNumber },
                    onLog = { exerciseToLog = it },
                    onWatch = openVideo
                )
            }
        } else {
            item {
                ChoiceChips(
                    options = listOf<ExerciseCategory?>(null) + ExerciseCategory.entries,
                    selected = category,
                    label = { it?.let { c -> "${c.emoji} ${c.label}" } ?: "All" },
                    onSelect = { category = it }
                )
            }
            items(ExerciseLibrary.byCategory(category), key = { it.id }) { exercise ->
                ExerciseCard(exercise, profile.weightKg, onLog = { exerciseToLog = exercise }, onWatch = openVideo)
            }
        }
    }

    tutorialExercise?.let { exercise ->
        AlertDialog(
            onDismissRequest = {
                tutorialExercise = null
                tutorial = null
                tutorialError = null
            },
            title = {
                Text("How to do ${exercise.name}")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    when {
                        tutorialLoading -> {
                            CircularProgressIndicator()
                            Text("Finding the best tutorial...")
                        }

                        tutorialError != null -> {
                            Text(
                                tutorialError ?: "Unable to load tutorial.",
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        tutorial != null -> {
                            Text(
                                tutorial!!.title,
                                style = MaterialTheme.typography.titleSmall
                            )
                            TutorialWebPlayer(tutorial = tutorial!!)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        tutorialExercise = null
                        tutorial = null
                        tutorialError = null
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }

    exerciseToLog?.let { exercise ->
        LogWorkoutDialog(
            exercise = exercise,
            weightKg = profile.weightKg,
            onDismiss = { exerciseToLog = null },
            onConfirm = { minutes ->
                viewModel.logWorkout(exercise, minutes)
                exerciseToLog = null
                Toast.makeText(context, "Logged ${exercise.name} 💪", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun PlanSummaryCard(plan: WorkoutPlan, weightKg: Double) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${plan.goal.emoji} ${plan.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Built for: ${plan.goal.label} · ${plan.level.label}")
            Row(Modifier.fillMaxWidth()) {
                PlanFigure("${plan.activeDays}", "active days", Modifier.weight(1f))
                PlanFigure("${plan.weeklyMinutes}", "min / week", Modifier.weight(1f))
                PlanFigure("${plan.weeklyCalories(weightKg).roundToInt()}", "kcal / week", Modifier.weight(1f))
            }
            Text(
                "Day-by-day split: Day 1 is Monday, Day 7 is Sunday. " +
                    "Tap a day above to see its exercises, then tap ▶ Watch to see how to do each move on YouTube. " +
                    "Change your goal or experience in Profile to get a new plan.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun PlanFigure(value: String, label: String, modifier: Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SessionCard(
    session: WorkoutSession,
    isToday: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onLog: (Exercise) -> Unit,
    onWatch: (Exercise) -> Unit
) {
    val colors = if (isToday) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    } else {
        CardDefaults.cardColors()
    }
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = !session.isRestDay) { onToggle() },
        colors = colors
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            DescribableRow(session) {
                if (isToday) {
                    Text(
                        "TODAY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            if (expanded && !session.isRestDay) {
                HorizontalDivider()
                session.exercises.forEach { exercise ->
                    DescribableRow(exercise) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { onWatch(exercise) }) { Text("▶ Watch") }
                            TextButton(onClick = { onLog(exercise) }) { Text("Log") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseCard(exercise: Exercise, weightKg: Double, onLog: () -> Unit, onWatch: (Exercise) -> Unit) {
    val minutes = exercise.estimatedMinutes()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            DescribableRow(exercise) {
                FilledTonalButton(onClick = onLog) { Text("Log") }
            }
            Text(exercise.description, style = MaterialTheme.typography.bodySmall)
            Text(
                "${exercise.category.label} · ${exercise.level.label} · " +
                    "≈${exercise.caloriesBurned(weightKg, minutes).roundToInt()} kcal in $minutes min",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp)
            )
            OutlinedButton(
                onClick = { onWatch(exercise) },
                modifier = Modifier.padding(top = 8.dp)
            ) { Text("▶ Watch how to do it on YouTube") }
        }
    }
}
