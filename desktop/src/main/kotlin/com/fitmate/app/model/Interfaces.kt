package com.fitmate.app.model

/** Anything stored in a [com.fitmate.app.data.Repository] needs a stable, unique id. */
interface Identifiable {
    val id: String
}

/**
 * Anything the UI can show as a row with an emoji, a title and a subtitle.
 * Exercises, plan sessions, workout logs, reminders and suggestions all implement it,
 * so one composable (`DescribableRow`) can draw all five — that's polymorphism.
 */
interface Describable {
    val title: String
    val subtitle: String
    val emoji: String
}
