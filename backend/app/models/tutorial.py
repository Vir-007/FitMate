from datetime import datetime
from typing import Optional

from pydantic import BaseModel, Field


class Tutorial(BaseModel):
    provider: str
    video_id: str

    title: str
    description: Optional[str] = None

    thumbnail_url: Optional[str] = None
    watch_url: str
    embed_url: Optional[str] = None

    view_count: int = Field(default=0, ge=0)
    like_count: Optional[int] = Field(default=None, ge=0)

    published_at: Optional[datetime] = None
    duration_seconds: Optional[int] = Field(default=None, ge=0)

    relevance_score: float = Field(default=0.0, ge=0.0, le=1.0)

    is_embeddable: bool = False


class TutorialFeedback(BaseModel):
    exercise_id: str
    provider: str
    video_id: str

    liked: bool
    user_id: Optional[str] = None


class TutorialRecommendation(BaseModel):
    exercise_id: str
    tutorials: list[Tutorial]
    selected_index: int = 0
