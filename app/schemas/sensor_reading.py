# SensorReading schemas - Pydantic models for sensor reading operations.
# Validates pollutant values (positive decimals), timestamps,
# and foreign key references to zones and pollutant types.

from datetime import datetime
from typing import Optional

from pydantic import BaseModel, Field


class SensorReadingCreate(BaseModel):
    """Schema for recording a new sensor reading with pollutant measurement."""

    # Measured pollutant value - must be non-negative
    value: float = Field(
        ..., ge=0,
        description="Measured pollutant value (non-negative decimal)"
    )

    # Optional pre-calculated AQI - if not provided, server calculates it
    aqi: Optional[int] = Field(
        None, ge=0, le=500,
        description="Air Quality Index (0-500 scale)"
    )

    # Optional custom timestamp - defaults to current time if not provided
    recorded_at: Optional[datetime] = Field(
        None,
        description="Timestamp of the reading (defaults to current time)"
    )

    # Foreign key references identifying the source zone and pollutant type
    zone_id: int = Field(..., gt=0, description="Monitoring zone ID")
    pollutant_type_id: int = Field(..., gt=0, description="Pollutant type ID")


class SensorReadingResponse(BaseModel):
    """Schema for sensor reading data in API responses."""
    id: int
    value: float
    aqi: Optional[int] = None
    recorded_at: Optional[datetime] = None
    zone_id: int
    pollutant_type_id: int
    created_at: Optional[datetime] = None

    model_config = {"from_attributes": True}


class SensorReadingFilter(BaseModel):
    """Schema for filtering sensor readings by zone, pollutant, and date range."""
    zone_id: Optional[int] = Field(None, gt=0)
    pollutant_type_id: Optional[int] = Field(None, gt=0)
    start_date: Optional[datetime] = None
    end_date: Optional[datetime] = None
