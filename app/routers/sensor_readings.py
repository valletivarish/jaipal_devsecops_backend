# SensorReadings Router
# Handles creation and retrieval of air quality sensor readings.
# Supports filtering by zone, pollutant type, and date range.
# Readings are immutable - no update or delete endpoints for data integrity.
# All endpoints require JWT authentication.

from datetime import datetime
from typing import Optional

from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.schemas.sensor_reading import SensorReadingCreate, SensorReadingResponse
from app.services import sensor_reading_service
from app.middleware.jwt_middleware import get_current_user
from app.models.user import User

router = APIRouter(prefix="/api/readings", tags=["Sensor Readings"])


@router.post(
    "/",
    response_model=SensorReadingResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Record a new sensor reading",
)
def create_reading(
    data: SensorReadingCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    Record a sensor reading with a pollutant value for a monitoring zone.
    AQI is automatically calculated from the raw value if not provided.
    Value must be non-negative. Timestamp defaults to current time.
    """
    return sensor_reading_service.create_sensor_reading(db, data)


@router.get(
    "/",
    response_model=list[SensorReadingResponse],
    summary="Get sensor readings with filters",
)
def get_readings(
    zone_id: Optional[int] = Query(None, gt=0, description="Filter by zone"),
    pollutant_type_id: Optional[int] = Query(None, gt=0, description="Filter by pollutant"),
    start_date: Optional[datetime] = Query(None, description="Start of date range"),
    end_date: Optional[datetime] = Query(None, description="End of date range"),
    skip: int = Query(0, ge=0, description="Records to skip"),
    limit: int = Query(100, ge=1, le=500, description="Max records"),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    Retrieve sensor readings with optional filters for zone, pollutant, and date range.
    Results are sorted by most recent first.
    """
    return sensor_reading_service.get_readings(
        db,
        zone_id=zone_id,
        pollutant_type_id=pollutant_type_id,
        start_date=start_date,
        end_date=end_date,
        skip=skip,
        limit=limit,
    )


@router.get(
    "/{reading_id}",
    response_model=SensorReadingResponse,
    summary="Get sensor reading by ID",
)
def get_reading(
    reading_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Retrieve a specific sensor reading by its ID."""
    return sensor_reading_service.get_reading_by_id(db, reading_id)
