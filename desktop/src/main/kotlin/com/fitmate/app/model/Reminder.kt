package com.fitmate.app.model

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class ReminderType(
    val label: String,
    val emoji: String,
    val message: String,
    val defaultHour: Int,
    val defaultMinute: Int
) {
    WORKOUT("Workout", "💪", "Time to move! Your workout is waiting.", 7, 0),
    WATER("Drink water", "💧", "Have a glass of water to stay hydrated.", 11, 0),
    MEAL("Healthy meal", "🥗", "Fuel up with a balanced, protein-rich meal.", 13, 0),
    STRETCH("Stretch break", "🧘", "Stand up and stretch for two minutes.", 16, 0),
    SLEEP("Wind down", "🌙", "Put the screens away — sleep is when muscles recover.", 22, 30)
}

/** A daily reminder. Immutable: changes are made with `copy(...)`. */
data class Reminder(
    override val id: String = UUID.randomUUID().toString(),
    val type: ReminderType,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true
) : Identifiable, Describable {

    init {
        require(hour in 0..23 && minute in 0..59) { "Invalid reminder time $hour:$minute" }
    }

    val timeLabel: String get() = LocalTime.of(hour, minute).format(TIME_FORMAT)

    override val title: String get() = type.label
    override val subtitle: String get() = "Every day at $timeLabel"
    override val emoji: String get() = type.emoji

    companion object {
        private val TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a")
    }
}
