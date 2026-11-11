from fastapi import FastAPI

from app.api.tutorials import router as tutorials_router

app = FastAPI(
    title="FitMate Tutorial API",
    description="Smart exercise tutorial search and recommendation service.",
    version="1.0.0",
)

app.include_router(tutorials_router)


@app.get("/")
def root():
    return {
        "app": "FitMate Tutorial API",
        "status": "running",
        "message": "Welcome to FitMate's tutorial recommendation service!",
    }


@app.get("/health")
def health_check():
    return {
        "status": "healthy",
        "service": "tutorial-api",
    }
