# PollutantType schemas - Pydantic models for pollutant type CRUD operations.
# Validates threshold values (must be positive decimals) and ensures
# safe < warning < danger threshold ordering for proper AQI calculation.

from datetime import datetime
from typing import Optional

from pydantic import BaseModel, Field, model_validator


class PollutantTypeCreate(BaseModel):
    """Schema for creating a new pollutant type with threshold levels."""

    # Pollutant name - e.g., "PM2.5", "PM10", "NO2", "O3", "SO2", "CO"
    name: str = Field(
        ..., min_length=1, max_length=50,
        description="Pollutant identifier (e.g., PM2.5, NO2)"
    )

    # Description of the pollutant and its health effects
    description: Optional[str] = Field(
        None, max_length=1000,
        description="Description of the pollutant"
    )

    # Unit of measurement - e.g., "ug/m3", "ppm", "ppb"
    unit: str = Field(
        ..., min_length=1, max_length=20,
        description="Measurement unit (e.g., ug/m3, ppm)"
    )

    # Threshold values must be positive and in ascending order
    # safe_threshold: below this level, air quality is good
    safe_threshold: float = Field(
        ..., gt=0,
        description="Safe level threshold (positive decimal)"
    )
    # warning_threshold: above this, sensitive groups are affected
    warning_threshold: float = Field(
        ..., gt=0,
        description="Warning level threshold (positive decimal)"
    )
    # danger_threshold: above this, hazardous for all
    danger_threshold: float = Field(
        ..., gt=0,
        description="Danger level threshold (positive decimal)"
    )

    @model_validator(mode="after")
    def validate_threshold_order(self):
        """Ensure thresholds are in ascending order: safe < warning < danger."""
        if self.safe_threshold >= self.warning_threshold:
            raise ValueError("Safe threshold must be less than warning threshold")
        if self.warning_threshold >= self.danger_threshold:
            raise ValueError("Warning threshold must be less than danger threshold")
        return self


class PollutantTypeUpdate(BaseModel):
    """Schema for updating a pollutant type - all fields optional."""
    name: Optional[str] = Field(None, min_length=1, max_length=50)
    description: Optional[str] = Field(None, max_length=1000)
    unit: Optional[str] = Field(None, min_length=1, max_length=20)
    safe_threshold: Optional[float] = Field(None, gt=0)
    warning_threshold: Optional[float] = Field(None, gt=0)
    danger_threshold: Optional[float] = Field(None, gt=0)


class PollutantTypeResponse(BaseModel):
    """Schema for pollutant type data in API responses."""
    id: int
    name: str
    description: Optional[str] = None
    unit: str
    safe_threshold: float
    warning_threshold: float
    danger_threshold: float
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None

    model_config = {"from_attributes": True}
