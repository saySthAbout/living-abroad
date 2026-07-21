import logging
import uuid

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from app.routers import chat, health, recommend

logger = logging.getLogger("app")

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
    return JSONResponse(
        status_code=500,
        content={
            "error": {
                "code": "INTERNAL_ERROR",
                "message": f"일시적인 오류가 발생했습니다. 문제가 계속되면 traceId {trace_id}와 함께 문의해 주세요.",
                "traceId": trace_id,
            }
        },
    )
