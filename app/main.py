# Main Application Entry Point
# Creates and configures the FastAPI application instance.
# Registers all routers (API endpoints), middleware (CORS, JWT),
# exception handlers, and creates database tables on startup.
# The application serves as the backend for the Air Quality Monitoring Dashboard.

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.database import engine, Base
from app.exceptions.handlers import register_exception_handlers

# Import all routers for API endpoints
from app.routers import (
    auth,
    users,
    monitoring_zones,
    pollutant_types,
    alert_rules,
    sensor_readings,
    dashboard,
    forecast,
)

# Create the FastAPI application with OpenAPI metadata
app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    description=(
        "REST API for real-time air quality monitoring. "
        "Track pollutant levels across monitoring zones, set alert rules, "
        "view historical trends, and get ML-based forecasts."
    ),
    docs_url="/api/docs",
    redoc_url="/api/redoc",
    openapi_url="/api/openapi.json",
)

# Configure CORS middleware to allow frontend cross-origin requests
# This is essential for the React frontend to communicate with the API
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Register custom exception handlers for structured error responses
register_exception_handlers(app)

# Register all API routers with the application
app.include_router(auth.router)
app.include_router(users.router)
app.include_router(monitoring_zones.router)
app.include_router(pollutant_types.router)
app.include_router(alert_rules.router)
app.include_router(sensor_readings.router)
app.include_router(dashboard.router)
app.include_router(forecast.router)


@app.on_event("startup")
def on_startup():
    """
    Create all database tables on application startup.
    Uses SQLAlchemy's create_all which only creates tables
    that do not already exist (safe for repeated startups).
    """
    Base.metadata.create_all(bind=engine)


@app.get("/api/health", tags=["Health"])
def health_check():
    """
    Health check endpoint for monitoring and deployment verification.
    Returns a simple JSON response confirming the API is running.
    Used by CI/CD pipeline smoke tests and load balancer health checks.
    """
    return {"status": "healthy", "service": settings.APP_NAME}
