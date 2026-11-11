
from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel, Field

from app.providers.youtube import YouTubeProvider
from app.services.ranking import rank_tutorials
from app.services.feedback_store import (
    initialize_database,
    save_feedback,
    get_rejected_video_ids,
)

router = APIRouter(prefix="/tutorials", tags=["Tutorials"])

youtube_provider = YouTubeProvider()

# Ensure the feedback table exists.
initialize_database()


class FeedbackRequest(BaseModel):
    exercise_id: str = Field(..., min_length=2, max_length=100)
    provider: str = Field(..., min_length=2, max_length=30)
    video_id: str = Field(..., min_length=1, max_length=100)
    liked: bool
    user_id: str = Field(default="anonymous", min_length=1, max_length=100)


@router.get("/search")
async def search_tutorials(
    exercise: str = Query(..., min_length=2, max_length=100),
    limit: int = Query(10, ge=1, le=25),
    user_id: str = Query("anonymous", min_length=1, max_length=100),
):
    try:
        tutorials = await youtube_provider.search_tutorials(
            exercise_name=exercise,
            max_results=limit,
        )

        rejected_video_ids = get_rejected_video_ids(
            exercise_id=exercise,
            user_id=user_id,
        )

        ranked = rank_tutorials(
            tutorials,
            rejected_video_ids=rejected_video_ids,
        )

        return {
            "exercise": exercise,
            "total_results": len(ranked),
            "selected_tutorial": (
                ranked[0].model_dump() if ranked else None
            ),
            "tutorials": [
                tutorial.model_dump() for tutorial in ranked
            ],
        }

    except Exception as error:
        raise HTTPException(
            status_code=502,
            detail=f"Tutorial search failed: {str(error)}",
        )


@router.post("/feedback")
async def submit_tutorial_feedback(feedback: FeedbackRequest):
    try:
        save_feedback(
            exercise_id=feedback.exercise_id,
            provider=feedback.provider,
            video_id=feedback.video_id,
            liked=feedback.liked,
            user_id=feedback.user_id,
        )

        return {
            "message": "Feedback saved successfully",
            "exercise_id": feedback.exercise_id,
            "video_id": feedback.video_id,
            "liked": feedback.liked,
        }

    except Exception as error:
        raise HTTPException(
            status_code=500,
            detail=f"Could not save feedback: {str(error)}",
        )
