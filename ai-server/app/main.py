import logging
import os
import uuid

import httpx
import sentry_sdk
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from sentry_sdk.integrations.fastapi import FastApiIntegration
from sentry_sdk.integrations.starlette import StarletteIntegration
from starlette.background import BackgroundTask

from app.routers import chat, health, recommend

logger = logging.getLogger("app")

SLACK_WEBHOOK_URL = os.getenv("SLACK_WEBHOOK_URL", "")
APP_ENVIRONMENT = os.getenv("SENTRY_ENVIRONMENT", "local")


# Slack Incoming Webhook URL은 그 자체로 비밀값이라 서버 사이드에서만 호출한다(프론트엔드 노출 금지).
async def notify_slack(trace_id: str, path: str, detail: str) -> None:
    if not SLACK_WEBHOOK_URL:
        return
    try:
        async with httpx.AsyncClient(timeout=3.0) as client:
            await client.post(
                SLACK_WEBHOOK_URL,
                json={
                    "text": (
                        f":rotating_light: *AI 서버 오류*\n"
                        f"환경: {APP_ENVIRONMENT}\n경로: {path}\ntraceId: {trace_id}\n{detail}"
                    )
                },
            )
    except Exception:
        logger.warning("Slack 알림 발송 실패", exc_info=True)

sentry_sdk.init(
    dsn=os.getenv("SENTRY_DSN", ""),
    environment=os.getenv("SENTRY_ENVIRONMENT", "local"),
    server_name="living-abroad-ai-server",
    integrations=[StarletteIntegration(), FastApiIntegration()],
    traces_sample_rate=float(os.getenv("SENTRY_TRACES_SAMPLE_RATE", "0.1")),
)

app = FastAPI(title="Living Abroad AI Server", version="0.0.1")

app.include_router(health.router)
app.include_router(recommend.router)
app.include_router(chat.router)


@app.exception_handler(Exception)
async def handle_unexpected_exception(request: Request, exc: Exception) -> JSONResponse:
    """예상하지 못한 모든 예외의 최종 안전망. 서버 내부 정보는 로그로만 남기고
    클라이언트에는 안전한 메시지만 반환한다 (NFR-03: 개인정보·내부 오류 노출 금지)."""
    trace_id = str(uuid.uuid4())
    logger.exception("예상하지 못한 오류 발생 (traceId=%s, path=%s)", trace_id, request.url.path)
    sentry_sdk.capture_exception(exc)
    return JSONResponse(
        status_code=500,
        content={
            "error": {
                "code": "INTERNAL_ERROR",
                "message": f"일시적인 오류가 발생했습니다. 문제가 계속되면 traceId {trace_id}와 함께 문의해 주세요.",
                "traceId": trace_id,
            }
        },
        background=BackgroundTask(notify_slack, trace_id, str(request.url.path), str(exc)),
    )
