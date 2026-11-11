from app.models.tutorial import Tutorial
from app.services.ranking import rank_tutorials


def make_tutorial(
    video_id,
    views,
    likes,
    relevance,
    embeddable=True,
):
    return Tutorial(
        provider="youtube",
        video_id=video_id,
        title=f"Tutorial {video_id}",
        watch_url=f"https://www.youtube.com/watch?v={video_id}",
        embed_url=f"https://www.youtube.com/embed/{video_id}",
        view_count=views,
        like_count=likes,
        relevance_score=relevance,
        is_embeddable=embeddable,
    )


def test_best_tutorial_is_ranked_first():
    tutorials = [
        make_tutorial("A", 1000, 50, 0.5),
        make_tutorial("B", 100000, 9000, 0.95),
    ]

    ranked = rank_tutorials(tutorials)

    assert ranked[0].video_id == "B"


def test_non_embeddable_tutorial_is_excluded():
    tutorials = [
        make_tutorial("A", 100000, 9000, 0.95, False),
        make_tutorial("B", 1000, 50, 0.8),
    ]

    ranked = rank_tutorials(tutorials)

    assert len(ranked) == 1
    assert ranked[0].video_id == "B"


def test_rejected_tutorial_is_skipped():
    tutorials = [
        make_tutorial("A", 100000, 9000, 0.95),
        make_tutorial("B", 1000, 50, 0.8),
    ]

    ranked = rank_tutorials(
        tutorials,
        rejected_video_ids={"A"},
    )

    assert ranked[0].video_id == "B"


def test_missing_like_count_does_not_break_ranking():
    tutorials = [
        make_tutorial("A", 1000, None, 0.8),
        make_tutorial("B", 500, 30, 0.7),
    ]

    ranked = rank_tutorials(tutorials)

    assert len(ranked) == 2


def test_all_rejected_returns_empty_list():
    tutorials = [
        make_tutorial("A", 1000, 50, 0.8),
    ]

    ranked = rank_tutorials(
        tutorials,
        rejected_video_ids={"A"},
    )

    assert ranked == []
