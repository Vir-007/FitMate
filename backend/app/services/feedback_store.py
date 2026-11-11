
import sqlite3
from pathlib import Path
from typing import Optional

DATABASE_PATH = Path(__file__).resolve().parents[2] / "fitmate.db"


def get_connection():
    connection = sqlite3.connect(DATABASE_PATH)
    connection.row_factory = sqlite3.Row
    return connection


def initialize_database():
    with get_connection() as connection:
        connection.execute("""
            CREATE TABLE IF NOT EXISTS tutorial_feedback (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                exercise_id TEXT NOT NULL,
                provider TEXT NOT NULL,
                video_id TEXT NOT NULL,
                user_id TEXT NOT NULL,
                liked INTEGER NOT NULL CHECK (liked IN (0, 1)),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE (exercise_id, provider, video_id, user_id)
            )
        """)
        connection.commit()


def save_feedback(
    exercise_id: str,
    provider: str,
    video_id: str,
    liked: bool,
    user_id: str = "anonymous",
):
    with get_connection() as connection:
        connection.execute("""
            INSERT INTO tutorial_feedback
                (exercise_id, provider, video_id, user_id, liked)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (exercise_id, provider, video_id, user_id)
            DO UPDATE SET
                liked = excluded.liked,
                created_at = CURRENT_TIMESTAMP
        """, (
            exercise_id,
            provider,
            video_id,
            user_id,
            int(liked),
        ))
        connection.commit()


def get_rejected_video_ids(
    exercise_id: str,
    user_id: str = "anonymous",
) -> set[str]:
    with get_connection() as connection:
        rows = connection.execute("""
            SELECT video_id
            FROM tutorial_feedback
            WHERE exercise_id = ?
              AND user_id = ?
              AND liked = 0
        """, (exercise_id, user_id)).fetchall()

    return {row["video_id"] for row in rows}


def get_liked_video_ids(
    exercise_id: str,
    user_id: str = "anonymous",
) -> set[str]:
    with get_connection() as connection:
        rows = connection.execute("""
            SELECT video_id
            FROM tutorial_feedback
            WHERE exercise_id = ?
              AND user_id = ?
              AND liked = 1
        """, (exercise_id, user_id)).fetchall()

    return {row["video_id"] for row in rows}
