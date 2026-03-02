# Dashboard Router
# Provides aggregated data endpoints for the dashboard overview page.
# Includes summary statistics, pollutant trend data for charts,
# and zone comparison analytics. All endpoints require authentication.

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.database import get_db
from app.schemas.dashboard import (
    DashboardSummary,
    PollutantTrend,
    ZoneComparison,
)
from app.services import dashboard_service
from app.middleware.jwt_middleware import get_current_user
from app.models.user import User

router = APIRouter(prefix="/api/dashboard", tags=["Dashboard"])


@router.get(
    "/summary",
    response_model=DashboardSummary,
    summary="Get dashboard summary with zone statistics",
)
def get_summary(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    Retrieve the dashboard overview including total zones, active alerts,
    today's reading count, average AQI, and per-zone status summaries.
    """
    return dashboard_service.get_dashboard_summary(db)


@router.get(
    "/trends",
    response_model=PollutantTrend,
    summary="Get pollutant level trends for a zone",
)
def get_trends(
    zone_id: int = Query(..., gt=0, description="Monitoring zone ID"),
    pollutant_type_id: int = Query(..., gt=0, description="Pollutant type ID"),
    days: int = Query(30, ge=1, le=365, description="Number of days of history"),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    Retrieve historical pollutant level trend data for charting.
    Returns time-series data points for the specified zone and pollutant
    over the given number of days.
    """
    return dashboard_service.get_pollutant_trend(
        db, zone_id, pollutant_type_id, days
    )


@router.get(
    "/comparison",
    response_model=list[ZoneComparison],
    summary="Compare pollutant levels across zones",
)
def get_comparison(
    pollutant_type_id: int = Query(..., gt=0, description="Pollutant type ID"),
    days: int = Query(7, ge=1, le=90, description="Number of days to compare"),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    Compare average pollutant levels across all monitoring zones.
    Returns aggregated statistics (average, max, min) per zone
    for the specified pollutant over the given time period.
    """
    return dashboard_service.get_zone_comparison(db, pollutant_type_id, days)
