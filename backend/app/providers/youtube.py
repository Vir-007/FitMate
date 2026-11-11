import os
from pathlib import Path
import re
from datetime import datetime
from typing import Optional

import httpx
from dotenv import load_dotenv

from app.models.tutorial import Tutorial


load_dotenv(Path(__file__).resolve().parents[2] / ".env")

YOUTUBE_API_KEY = os.getenv("YOUTUBE_API_KEY")
BASE_URL = "https://www.googleapis.com/youtube/v3"


VARIATIONS = {
    "knee", "incline", "decline", "diamond", "pike",
    "wide", "archer", "explosive", "clap", "wall",
    "weighted", "deficit", "staggered", "one", "arm",
    "single", "close", "grip", "elevated",
}

POSITIVE_INTENT = {
    "how to": 0.15,
    "tutorial": 0.15,
    "proper form": 0.15,
    "correct form": 0.15,
    "technique": 0.12,
    "beginner guide": 0.12,
    "step by step": 0.10,
    "demonstration": 0.10,
}

NEGATIVE_INTENT = {
    "mistakes": 0.08,
    "reaction": 0.15,
    "challenge": 0.12,
    "compilation": 0.15,
    "fails": 0.20,
}


def normalize(value: str) -> set[str]:
    value = value.lower()
    value = re.sub(r"push[\s-]?ups?", "pushup", value)
    words = re.findall(r"[a-z0-9]+", value)

    normalized = set()

    for word in words:
        if word.endswith("ies") and len(word) > 4:
            word = word[:-3] + "y"
        elif (
            word.endswith("s")
            and not word.endswith("ss")
            and len(word) > 3
        ):
            word = word[:-1]

        normalized.add(word)

    return normalized


def calculate_relevance(
    exercise_name: str,
    title: str,
    description: str,
) -> float:

    requested_words = normalize(exercise_name)
    title_words = normalize(title)
    description_words = normalize(description)

    if not requested_words:
        return 0.0

    requested_variations = requested_words.intersection(VARIATIONS)
    title_variations = title_words.intersection(VARIATIONS)

    requested_core = requested_words - VARIATIONS
    title_core = title_words - VARIATIONS

    if not requested_core:
        return 0.0

    core_matches = requested_core.intersection(title_core)
    title_match = len(core_matches) / len(requested_core)

    description_matches = requested_core.intersection(
        description_words
    )
    description_match = len(description_matches) / len(requested_core)

    score = title_match * 0.75 + description_match * 0.25

    if requested_variations != title_variations:
        if title_variations and not requested_variations:
            score *= 0.30
        elif requested_variations and not title_variations:
            score *= 0.65
        else:
            score *= 0.45

    return round(min(score, 1.0), 3)


def calculate_intent_score(title: str) -> float:
    normalized_title = title.lower()

    score = 0.5

    for phrase, bonus in POSITIVE_INTENT.items():
        if phrase in normalized_title:
            score += bonus

    for phrase, penalty in NEGATIVE_INTENT.items():
        if phrase in normalized_title:
            score -= penalty

    return round(max(0.0, min(score, 1.0)), 3)


def parse_duration(value: str) -> int:
    pattern = re.compile(r"PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?")
    match = pattern.fullmatch(value)

    if not match:
        return 0

    hours, minutes, seconds = (
        int(part or 0) for part in match.groups()
    )

    return hours * 3600 + minutes * 60 + seconds


class YouTubeProvider:

    async def search_tutorials(
        self,
        exercise_name: str,
        max_results: int = 10,
    ) -> list[Tutorial]:

        if not YOUTUBE_API_KEY:
            raise RuntimeError(
                "YOUTUBE_API_KEY is missing from backend/.env"
            )

        max_results = max(1, min(max_results, 25))

        async with httpx.AsyncClient(timeout=20.0) as client:

            search_response = await client.get(
                f"{BASE_URL}/search",
                params={
                    "key": YOUTUBE_API_KEY,
                    "part": "snippet",
                    "q": (
                        f"{exercise_name} "
                        "exercise tutorial proper form"
                    ),
                    "type": "video",
                    "videoEmbeddable": "true",
                    "videoSyndicated": "true",
                    "maxResults": max_results,
                    "regionCode": "IN",
                    "relevanceLanguage": "en",
                    "safeSearch": "moderate",
                },
            )

            search_response.raise_for_status()
            search_data = search_response.json()

            video_ids = [
                item["id"]["videoId"]
                for item in search_data.get("items", [])
                if item.get("id", {}).get("videoId")
            ]

            if not video_ids:
                return []

            details_response = await client.get(
                f"{BASE_URL}/videos",
                params={
                    "key": YOUTUBE_API_KEY,
                    "part": (
                        "snippet,statistics,"
                        "contentDetails,status"
                    ),
                    "id": ",".join(video_ids),
                },
            )

            details_response.raise_for_status()
            details_data = details_response.json()

        tutorials = []

        for item in details_data.get("items", []):

            video_id = item["id"]

            snippet = item.get("snippet", {})
            statistics = item.get("statistics", {})
            content = item.get("contentDetails", {})
            status = item.get("status", {})

            if not status.get("embeddable", False):
                continue

            title = snippet.get("title", "")
            description = snippet.get("description", "")

            thumbnails = snippet.get("thumbnails", {})

            thumbnail = (
                thumbnails.get("high")
                or thumbnails.get("medium")
                or thumbnails.get("default")
                or {}
            )

            published_at = snippet.get("publishedAt")
            parsed_date: Optional[datetime] = None

            if published_at:
                parsed_date = datetime.fromisoformat(
                    published_at.replace("Z", "+00:00")
                )

            view_count = int(statistics.get("viewCount", 0))

            like_count = (
                int(statistics["likeCount"])
                if "likeCount" in statistics
                else None
            )

            relevance = calculate_relevance(
                exercise_name,
                title,
                description,
            )

            intent = calculate_intent_score(title)

            # Combine exercise relevance with tutorial intent.
            final_relevance = round(
                relevance * 0.85 + intent * 0.15,
                3,
            )

            tutorials.append(
                Tutorial(
                    provider="youtube",
                    video_id=video_id,
                    title=title,
                    description=description,
                    thumbnail_url=thumbnail.get("url"),
                    watch_url=(
                        f"https://www.youtube.com/watch?v={video_id}"
                    ),
                    embed_url=(
                        f"https://www.youtube.com/embed/{video_id}"
                    ),
                    view_count=view_count,
                    like_count=like_count,
                    published_at=parsed_date,
                    duration_seconds=parse_duration(
                        content.get("duration", "")
                    ),
                    relevance_score=final_relevance,
                    is_embeddable=True,
                )
            )

        return tutorials
