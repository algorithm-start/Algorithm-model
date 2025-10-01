"""Solver Engine FastAPI application."""

import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from api.routes import health, solve, examples

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)

app = FastAPI(
    title="Solver Engine",
    description="Optimization solver engine for RecPlatform",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(health.router, tags=["health"])
app.include_router(solve.router, tags=["solve"])
app.include_router(examples.router, tags=["examples"])


@app.on_event("startup")
async def startup_event():
    """Initialize resources on startup."""
    logging.info("Solver Engine starting up...")
    # Pre-initialize the registry to discover solvers
    from solvers.registry import get_registry
    registry = get_registry()
    logging.info("Available solvers: %s", list(registry.get_all_solvers().keys()))
    logging.info("Supported problem types: %s", registry.get_supported_types())


@app.on_event("shutdown")
async def shutdown_event():
    """Clean up resources on shutdown."""
    logging.info("Solver Engine shutting down...")
