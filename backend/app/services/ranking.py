from datetime import datetime, timezone
from math import log1p

from app.models.tutorial import Tutorial


WEIGHTS = {
    "relevance": 0.40,
    "engagement": 0.25,
    "popularity": 0.20,
    "freshness": 0.15,
}

MIN_RELEVANCE_SCORE = 0.30


def normalize_views(tutorial: Tutorial, maximum_views: int) -> float:
    if maximum_views <= 0:
        return 0.0

    return log1p(tutorial.view_count) / log1p(maximum_views)


def engagement_score(tutorial: Tutorial) -> float | None:
    if tutorial.like_count is None:
        return None

    if tutorial.view_count <= 0:
        return 0.0

    return min(tutorial.like_count / tutorial.view_count, 1.0)


def freshness_score(tutorial: Tutorial) -> float:
    if tutorial.published_at is None:
        return 0.5

    published = tutorial.published_at

    if published.tzinfo is None:
        published = published.replace(tzinfo=timezone.utc)

    age_days = max(
        (datetime.now(timezone.utc) - published).days,
        0
    )

    return 1.0 / (1.0 + age_days / 365.0)


def calculate_score(
    tutorial: Tutorial,
    maximum_views: int
) -> float:
    signals = {
        "relevance": tutorial.relevance_score,
        "engagement": engagement_score(tutorial),
        "popularity": normalize_views(tutorial, maximum_views),
        "freshness": freshness_score(tutorial),
    }

    available_weight = sum(
        WEIGHTS[name]
        for name, value in signals.items()
        if value is not None
    )

    if available_weight == 0:
        return 0.0

    weighted_score = sum(
        WEIGHTS[name] * value
        for name, value in signals.items()
        if value is not None
    )

    return weighted_score / available_weight


def rank_tutorials(
    tutorials: list[Tutorial],
    rejected_video_ids: set[str] | None = None,
) -> list[Tutorial]:
    rejected = rejected_video_ids or set()

    candidates = [
        tutorial
        for tutorial in tutorials
        if tutorial.is_embeddable
        and tutorial.embed_url
        and tutorial.video_id not in rejected
        and tutorial.relevance_score >= MIN_RELEVANCE_SCORE
    ]

    if not candidates:
        return []

    maximum_views = max(
        tutorial.view_count for tutorial in candidates
    )

    return sorted(
        candidates,
        key=lambda tutorial: calculate_score(
            tutorial, maximum_views
        ),
        reverse=True,
    )
