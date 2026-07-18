from fastapi import FastAPI

from app.routers import chat, health, recommend

app = FastAPI(title="Living Abroad AI Server", version="0.0.1")

app.include_router(health.router)
app.include_router(recommend.router)
app.include_router(chat.router)
