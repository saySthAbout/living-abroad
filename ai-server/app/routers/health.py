from datetime import datetime, timezone

from fastapi import APIRouter

from app.schemas import HealthResponse

router = APIRouter()


@router.get("/ai/health", response_model=HealthResponse)
def get_health() -> HealthResponse:
    return HealthResponse(
        status="UP",
        timestamp=datetime.now(timezone.utc).isoformat(),
        version="0.0.1",
    )
