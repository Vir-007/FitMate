package com.fitmate.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.application
import com.fitmate.app.model.*
import com.fitmate.app.planner.WorkoutPlanFactory
import com.fitmate.app.tutorial.Tutorial
import com.fitmate.app.tutorial.TutorialApiClient
import com.fitmate.app.tutorial.TutorialPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.net.URI

private val Background = Color(0xFF101311)
private val Panel = Color(0xFF1B211D)
private val Accent = Color(0xFFB8F36B)
private val Muted = Color(0xFF929B93)
private val White = Color(0xFFF2F5F1)

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "FitMate | Fitness Dashboard"
    ) {
        FitMateApp()
    }
}

@Composable
fun FitMateApp() {
    var selectedPage by remember { mutableStateOf("Dashboard") }
    var selectedDay by remember { mutableIntStateOf(1) }
    var completedByDay by remember { mutableStateOf(WorkoutProgressStore.load()) }
    var workoutHistory by remember { mutableStateOf(WorkoutHistoryStore.load()) }
    val completedExercises = completedByDay.flatMap { (day, ids) -> ids.map { "$day:$it" } }.toSet()

    var profile by remember { mutableStateOf(ProfileStore.load()) }

    var tutorialExercise by remember { mutableStateOf<Exercise?>(null) }
    var selectedTutorial by remember { mutableStateOf<Tutorial?>(null) }
    var tutorialLoading by remember { mutableStateOf(false) }
    var tutorialError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tutorialExercise) {
        val exercise = tutorialExercise ?: return@LaunchedEffect

        selectedTutorial = null
        tutorialError = null
        tutorialLoading = true

        try {
            selectedTutorial = withContext(Dispatchers.IO) {
                TutorialApiClient.searchTutorial(exercise.name)
            }
        } catch (error: Exception) {
            tutorialError = error.message ?: "Unable to load tutorial."
        } finally {
            tutorialLoading = false
        }
    }

    val plan = remember(profile) {
        WorkoutPlanFactory.createPlan(profile)
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Background,
            surface = Panel,
            primary = Accent,
            onPrimary = Background,
            onBackground = White,
            onSurface = White,
            secondary = Muted
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Background
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Sidebar(
                    selectedPage = selectedPage,
                    onPageSelected = { selectedPage = it }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 30.dp, vertical = 24.dp)
                ) {
                    when (selectedPage) {
                        "Dashboard" -> Dashboard(
                            profile = profile,
                            plan = plan,
                            completedExercises = completedExercises,
                            workoutHistory = workoutHistory,
                            onOpenWorkouts = { selectedPage = "Workouts" }
                        )

                        "Workouts" -> WorkoutsPage(
                            plan = plan,
                            selectedDay = selectedDay,
                            completedExercises = completedByDay[selectedDay] ?: emptySet(),
                            onDaySelected = { selectedDay = it },
                            onWatchTutorial = { exercise ->
                                tutorialExercise = exercise
                            },
                            onToggleExercise = { id ->
                                val updated = (completedByDay[selectedDay] ?: emptySet()).let { current ->
                                    if (id in current) current - id else current + id
                                }
                                completedByDay = completedByDay + (selectedDay to updated)
                                WorkoutProgressStore.save(selectedDay, updated)

                                val session = plan.sessionFor(selectedDay)
                                if (session != null && session.exercises.isNotEmpty() &&
                                    session.exercises.all { it.id in updated }
                                ) {
                                    WorkoutHistoryStore.record(
                                        day = selectedDay,
                                        exerciseCount = session.exercises.size,
                                        minutes = session.totalMinutes(),
                                        calories = session.estimatedCalories(profile.weightKg)
                                    )
                                    workoutHistory = WorkoutHistoryStore.load()
                                }
                            }
                        )

                        "Exercise Library" -> ExerciseLibraryPage(
                            onWatchTutorial = { exercise ->
                                tutorialExercise = exercise
                            }
                        )

                        "History" -> HistoryPage(
                            entries = workoutHistory
                        )

                        "Profile" -> ProfilePage(
                            profile = profile,
                            onSave = { updatedProfile ->
                                profile = updatedProfile
                                ProfileStore.save(updatedProfile)
                            }
                        )
                    }
                }
            }
        }

        if (tutorialExercise != null) {
            AlertDialog(
                properties = DialogProperties(usePlatformDefaultWidth = false),
                onDismissRequest = {
                    tutorialExercise = null
                    selectedTutorial = null
                    tutorialError = null
                },
                title = {
                    Text(
                        "Workout Tutorial",
                        color = White
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.widthIn(min = 600.dp, max = 900.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            tutorialExercise?.name ?: "",
                            color = Accent,
                            fontWeight = FontWeight.Bold
                        )

                        when {
                            tutorialLoading -> {
                                CircularProgressIndicator(color = Accent)
                                Text("Finding the best tutorial...", color = Muted)
                            }

                            tutorialError != null -> {
                                Text(
                                    tutorialError ?: "Unable to load tutorial.",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            selectedTutorial != null -> {
                                Text(
                                    selectedTutorial!!.title,
                                    color = White,
                                    fontWeight = FontWeight.SemiBold
                                )

                                TutorialPlayer(
                                    tutorial = selectedTutorial!!,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(480.dp)
                                )

                                Text(
                                    "Views: ${"%,d".format(selectedTutorial!!.views)}" +
                                        (selectedTutorial!!.likes?.let {
                                            "  •  Likes: ${"%,d".format(it)}"
                                        } ?: ""),
                                    color = Muted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            tutorialExercise = null
                            selectedTutorial = null
                            tutorialError = null
                        }
                    ) {
                        Text("Close", color = Accent)
                    }
                },
                containerColor = Panel
            )
        }
    }
}

@Composable
fun Sidebar(
    selectedPage: String,
    onPageSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .width(225.dp)
            .fillMaxHeight()
            .background(Panel)
            .padding(22.dp)
    ) {
        Text(
            "FITMATE",
            color = Accent,
            fontSize = 25.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            "YOUR FITNESS COMPANION",
            color = Muted,
            fontSize = 10.sp
        )

        Spacer(Modifier.height(45.dp))

        listOf(
            "Dashboard",
            "Workouts",
            "Exercise Library",
            "History",
            "Profile"
        ).forEach { page ->
            val active = selectedPage == page

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .background(
                        if (active) Color(0xFF34432C) else Color.Transparent,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onPageSelected(page) }
                    .padding(14.dp)
            ) {
                Text(
                    page,
                    color = if (active) Accent else Muted,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Text("FITMATE DESKTOP", color = Muted, fontSize = 11.sp)
        Text("Version 1.0.0", color = Muted, fontSize = 10.sp)
    }
}

@Composable
fun PageHeader(title: String, subtitle: String) {
    Column {
        Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = White)
        Spacer(Modifier.height(5.dp))
        Text(subtitle, fontSize = 13.sp, color = Muted)
    }
    Spacer(Modifier.height(25.dp))
}

@Composable
fun RowScope.StatCard(title: String, value: String, description: String) {
    Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(title, color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            Text(value, color = Accent, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text(description, color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
fun Dashboard(
    profile: UserProfile,
    plan: WorkoutPlan,
    completedExercises: Set<String>,
    workoutHistory: List<WorkoutHistoryEntry>,
    onOpenWorkouts: () -> Unit
) {
    val totalPlannedExercises = plan.sessions.sumOf { it.exercises.size }
    val completedSessions = workoutHistory.size
    val totalCompletedExercises = workoutHistory.sumOf { it.exerciseCount }
    val totalMinutes = workoutHistory.sumOf { it.minutes }
    val totalCalories = workoutHistory.sumOf { it.calories }

    val today = java.time.LocalDate.now()
    val weekDates = (6 downTo 0).map { today.minusDays(it.toLong()) }

    val weeklyHistory = workoutHistory.filter { entry ->
        runCatching {
            val date = java.time.LocalDate.parse(entry.date)
            !date.isBefore(today.minusDays(6)) && !date.isAfter(today)
        }.getOrDefault(false)
    }

    val weeklySessions = weeklyHistory.size
    val weeklyMinutes = weeklyHistory.sumOf { it.minutes }
    val weeklyCalories = weeklyHistory.sumOf { it.calories }
    val weeklyCompletion = if (plan.activeDays > 0) {
        (weeklySessions * 100 / plan.activeDays).coerceAtMost(100)
    } else 0

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        PageHeader(
            "Good to see you, ${profile.name}!",
            "Your fitness journey starts with one good session."
        )

        Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
            StatCard("WORKOUTS", "$completedSessions", "Completed sessions")
            StatCard("DURATION", "$totalMinutes min", "Actual training time")
            StatCard("CALORIES", "%.0f kcal".format(totalCalories), "Estimated burned")
        }

        Spacer(Modifier.height(15.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
            StatCard(
                "EXERCISES DONE",
                "$totalCompletedExercises",
                "Completed sessions"
            )
            StatCard(
                "EXERCISE PROGRESS",
                "${completedExercises.size}/$totalPlannedExercises",
                "Checked exercises"
            )
        }

        Spacer(Modifier.height(28.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(22.dp)) {
                Text("WEEKLY PROGRESS", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Workout completion", color = White)
                    Text("$weeklyCompletion%", color = Accent, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { weeklyCompletion / 100f },
                    modifier = Modifier.fillMaxWidth().height(9.dp),
                    color = Accent,
                    trackColor = Background
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "$weeklySessions of ${plan.activeDays} planned active days completed in the last 7 days",
                    color = Muted,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "$weeklyMinutes minutes trained  •  %.0f kcal burned".format(weeklyCalories),
                    color = Accent,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(25.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(22.dp)) {
                Text("LAST 7 DAYS", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(18.dp))

                val dailyCounts = weekDates.map { date ->
                    weeklyHistory.count { it.date == date.toString() }
                }
                val maximum = maxOf(1, dailyCounts.maxOrNull() ?: 0)

                Row(
                    Modifier.fillMaxWidth().height(115.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    weekDates.forEachIndexed { index, date ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                "${dailyCounts[index]}",
                                color = Muted,
                                fontSize = 10.sp
                            )
                            Spacer(Modifier.height(5.dp))
                            Box(
                                Modifier
                                    .width(20.dp)
                                    .height((65f * dailyCounts[index] / maximum).coerceAtLeast(4f).dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Accent)
                            )
                            Spacer(Modifier.height(7.dp))
                            Text(
                                date.dayOfWeek.name.take(3),
                                color = Muted,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(25.dp))

        Text("YOUR HEALTH OVERVIEW", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(22.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("BODY MASS INDEX", color = Muted, fontSize = 11.sp)
                        Text(
                            "%.1f".format(profile.bmi),
                            color = Accent,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            profile.bmiCategory.label,
                            color = White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("HEIGHT", color = Muted, fontSize = 10.sp)
                        Text("${profile.heightCm} cm", color = White)
                        Spacer(Modifier.height(10.dp))
                        Text("WEIGHT", color = Muted, fontSize = 10.sp)
                        Text("${profile.weightKg} kg", color = White)
                    }
                }

                Spacer(Modifier.height(15.dp))

                LinearProgressIndicator(
                    progress = { (profile.bmi / 40.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = Accent,
                    trackColor = Background
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    profile.bmiCategory.advice,
                    color = Muted,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(25.dp))

        Text("YOUR FITNESS PROGRAM", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(plan.name, color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "${plan.goal.label}  •  ${plan.level.label}",
                    color = Accent
                )
                Spacer(Modifier.height(15.dp))
                Text(
                    "A personalised seven-day training schedule generated using your fitness goal and experience level.",
                    color = Muted
                )
                Spacer(Modifier.height(20.dp))
                Button(onClick = onOpenWorkouts) {
                    Text("Explore workout plan")
                }
            }
        }

        Spacer(Modifier.height(25.dp))

        Text("THIS WEEK", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))

        plan.sessions.forEach { session ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Panel)
            ) {
                Row(
                    Modifier.padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(session.emoji, fontSize = 24.sp)
                    Spacer(Modifier.width(15.dp))
                    Column(Modifier.weight(1f)) {
                        Text(session.dayLabel, color = White, fontWeight = FontWeight.SemiBold)
                        Text(session.subtitle, color = Muted, fontSize = 12.sp)
                    }
                    Text(
                        if (session.isRestDay) "REST" else "VIEW",
                        color = Accent,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun WorkoutsPage(
    plan: WorkoutPlan,
    selectedDay: Int,
    completedExercises: Set<String>,
    onDaySelected: (Int) -> Unit,
    onToggleExercise: (String) -> Unit,
    onWatchTutorial: (Exercise) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        PageHeader("Workout Planner", "Your CultFit-style weekly training schedule")

        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            (1..7).forEach { day ->
                val active = day == selectedDay

                Button(
                    onClick = { onDaySelected(day) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (active) Accent else Panel,
                        contentColor = if (active) Background else White
                    ),
                    contentPadding = PaddingValues(horizontal = 15.dp, vertical = 10.dp)
                ) {
                    Text("Day $day")
                }
            }
        }

        Spacer(Modifier.height(25.dp))

        val session = plan.sessionFor(selectedDay)

        if (session != null) {
            Text(
                session.dayLabel,
                color = White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(session.subtitle, color = Muted)

            val dayExercises = session.exercises
            val completedCount = dayExercises.count { it.id in completedExercises }
            val progress = if (dayExercises.isEmpty()) 0f else completedCount.toFloat() / dayExercises.size

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "$completedCount of ${dayExercises.size} exercises completed",
                    color = Muted,
                    fontSize = 13.sp
                )
                Text(
                    "${(progress * 100).toInt()}%",
                    color = Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Panel)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(Accent)
                )
            }

            if (dayExercises.isNotEmpty() && completedCount == dayExercises.size) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Workout completed! Great work!",
                    color = Accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.height(20.dp))

            if (session.isRestDay) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Panel)
                ) {
                    Column(Modifier.padding(25.dp)) {
                        Text("Recovery Day", color = Accent, fontSize = 22.sp)
                        Text(
                            "Rest, recover and prepare for your next workout.",
                            color = Muted
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(session.exercises) { exercise ->
                        ExerciseCard(
                            exercise = exercise,
                            completed = exercise.id in completedExercises,
                            onToggle = { onToggleExercise(exercise.id) },
                            onWatchTutorial = { onWatchTutorial(exercise) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseCard(
    exercise: Exercise,
    completed: Boolean,
    onToggle: () -> Unit,
    onWatchTutorial: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (completed) Color(0xFF253524) else Panel),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${exercise.emoji}  ${exercise.name}",
                    color = White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(5.dp))
                Text(exercise.subtitle, color = Accent, fontSize = 12.sp)
                Text(
                    exercise.description,
                    color = Muted,
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Estimated time: ${exercise.estimatedMinutes()} min",
                    color = Muted,
                    fontSize = 11.sp
                )

                Text(
                    "Watch tutorial ↗",
                    color = Accent,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clickable { onWatchTutorial() }
                        .padding(vertical = 5.dp)
                )
            }

            Checkbox(
                checked = completed,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

@Composable
fun ExerciseLibraryPage(onWatchTutorial: (Exercise) -> Unit) {
    var search by remember { mutableStateOf("") }

    val exercises = ExerciseLibrary.all.filter {
        it.name.contains(search, ignoreCase = true) ||
            it.category.label.contains(search, ignoreCase = true)
    }

    Column {
        PageHeader("Exercise Library", "Explore exercises and learn the correct form")

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("Search exercises") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(15.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(exercises) { exercise ->
                ExerciseCard(
                    exercise = exercise,
                    completed = false,
                    onToggle = {},
                    onWatchTutorial = { onWatchTutorial(exercise) }
                )
            }
        }
    }
}

@Composable
fun HistoryPage(entries: List<WorkoutHistoryEntry>) {
    val totalWorkouts = entries.size
    val totalExercises = entries.sumOf { it.exerciseCount }
    val totalMinutes = entries.sumOf { it.minutes }
    val totalCalories = entries.sumOf { it.calories }

    Column {
        PageHeader("Workout History", "Your completed workout sessions")

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard("WORKOUTS", "$totalWorkouts", "Completed sessions")
            StatCard("EXERCISES", "$totalExercises", "Exercises completed")
            StatCard("DURATION", "$totalMinutes min", "Total workout time")
        }

        Spacer(Modifier.height(12.dp))

        Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(20.dp)) {
                Text("Total calories burned", color = Muted)
                Text(
                    "%.0f kcal".format(totalCalories),
                    color = Accent,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "Recent Activity",
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))

        if (entries.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.padding(22.dp)) {
                    Text("No completed workouts yet", color = Color.White)
                    Text(
                        "Complete all exercises in a workout day to see it here.",
                        color = Muted
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(entries) { entry ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Panel
                        )
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Workout Day ${entry.day}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(entry.date, color = Accent)
                            }

                            Spacer(Modifier.height(10.dp))
                            Text(
                                "${entry.exerciseCount} exercises  •  ${entry.minutes} min  •  %.0f kcal".format(entry.calories),
                                color = Muted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfilePage(
    profile: UserProfile,
    onSave: (UserProfile) -> Unit
) {
    var name by remember(profile) { mutableStateOf(profile.name) }
    var age by remember(profile) { mutableStateOf(profile.age.toString()) }
    var height by remember(profile) { mutableStateOf(profile.heightCm.toString()) }
    var weight by remember(profile) { mutableStateOf(profile.weightKg.toString()) }

    var gender by remember(profile) { mutableStateOf(profile.gender) }
    var activity by remember(profile) { mutableStateOf(profile.activityLevel) }
    var goal by remember(profile) { mutableStateOf(profile.goal) }
    var experience by remember(profile) { mutableStateOf(profile.experience) }

    var errorMessage by remember { mutableStateOf("") }
    var savedMessage by remember { mutableStateOf(false) }

    val heightValue = height.toDoubleOrNull()
    val weightValue = weight.toDoubleOrNull()

    val previewBmi = if (
        heightValue != null && weightValue != null &&
        heightValue in UserProfile.MIN_HEIGHT..UserProfile.MAX_HEIGHT &&
        weightValue in UserProfile.MIN_WEIGHT..UserProfile.MAX_WEIGHT
    ) {
        weightValue / ((heightValue / 100.0) * (heightValue / 100.0))
    } else {
        null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        PageHeader("Your Profile", "Manage your personal fitness information")

        Card(
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                Text(
                    "PERSONAL INFORMATION",
                    color = Accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        savedMessage = false
                    },
                    label = { Text("Full name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = age,
                    onValueChange = {
                        age = it
                        savedMessage = false
                    },
                    label = { Text("Age (10–100)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = height,
                        onValueChange = {
                            height = it
                            savedMessage = false
                        },
                        label = { Text("Height (cm)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = weight,
                        onValueChange = {
                            weight = it
                            savedMessage = false
                        },
                        label = { Text("Weight (kg)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                ProfileDropdown(
                    label = "Gender",
                    selected = gender.label,
                    options = Gender.entries.map { it.label },
                    onSelected = { selected ->
                        gender = Gender.entries.first { it.label == selected }
                        savedMessage = false
                    }
                )

                ProfileDropdown(
                    label = "Activity level",
                    selected = activity.label,
                    options = ActivityLevel.entries.map { it.label },
                    onSelected = { selected ->
                        activity = ActivityLevel.entries.first { it.label == selected }
                        savedMessage = false
                    }
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    "FITNESS PREFERENCES",
                    color = Accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                ProfileDropdown(
                    label = "Fitness goal",
                    selected = goal.label,
                    options = FitnessGoal.entries.map { it.label },
                    onSelected = { selected ->
                        goal = FitnessGoal.entries.first { it.label == selected }
                        savedMessage = false
                    }
                )

                ProfileDropdown(
                    label = "Experience level",
                    selected = experience.label,
                    options = Difficulty.entries.map { it.label },
                    onSelected = { selected ->
                        experience = Difficulty.entries.first { it.label == selected }
                        savedMessage = false
                    }
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Background
                    )
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("BODY MASS INDEX", color = Muted, fontSize = 11.sp)
                        Text(
                            previewBmi?.let { "%.1f".format(it) } ?: "--",
                            color = Accent,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            previewBmi?.let { BmiCategory.from(it).label }
                                ?: "Enter valid height and weight",
                            color = Muted
                        )
                    }
                }

                if (errorMessage.isNotBlank()) {
                    Text(errorMessage, color = Color(0xFFFF7777))
                }

                if (savedMessage) {
                    Text(
                        "Profile saved successfully!",
                        color = Accent,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = {
                        try {
                            val updatedProfile = UserProfile(
                                name = name,
                                age = age.toInt(),
                                heightCm = height.toDouble(),
                                weightKg = weight.toDouble(),
                                gender = gender,
                                activityLevel = activity,
                                goal = goal,
                                experience = experience
                            )

                            onSave(updatedProfile)
                            errorMessage = ""
                            savedMessage = true
                        } catch (exception: Exception) {
                            errorMessage = exception.message
                                ?: "Please check your profile information."
                            savedMessage = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = Background
                    )
                ) {
                    Text(
                        "Save Profile",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(label, color = Muted, fontSize = 11.sp)
                    Text(selected, color = White)
                }
                Text("▼", color = Accent)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ProfileRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Muted)
        Text(value, color = White, fontWeight = FontWeight.Medium)
    }
}

fun openLink(url: String) {
    try {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(url))
        }
    } catch (exception: Exception) {
        exception.printStackTrace()
    }
}
