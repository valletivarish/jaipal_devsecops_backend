# Forecast Router
# Provides the ML-based air quality forecasting endpoint.
# Uses linear regression on historical sensor data to predict
# pollutant levels for the next 7 days.
# Returns predictions with confidence intervals and trend direction.

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.database import get_db
from app.schemas.dashboard import ForecastResponse
from app.services import forecast_service
from app.middleware.jwt_middleware import get_current_user
from app.models.user import User

router = APIRouter(prefix="/api/forecast", tags=["Forecast"])


@router.get(
    "/",
    response_model=ForecastResponse,
    summary="Get air quality forecast for a zone and pollutant",
)
def get_forecast(
    zone_id: int = Query(..., gt=0, description="Monitoring zone ID"),
    pollutant_type_id: int = Query(..., gt=0, description="Pollutant type ID"),
    history_days: int = Query(
        30, ge=7, le=365,
        description="Days of historical data to use for prediction"
    ),
    forecast_days: int = Query(
        7, ge=1, le=30,
        description="Number of days to forecast"
    ),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    Generate air quality forecast using linear regression.
    Requires at least 5 historical data points for the specified zone and pollutant.
    Returns predicted daily values with 95% confidence intervals,
    trend direction (INCREASING/DECREASING/STABLE), and model confidence score (R-squared).
    """
    return forecast_service.generate_forecast(
        db, zone_id, pollutant_type_id, history_days, forecast_days
    )
