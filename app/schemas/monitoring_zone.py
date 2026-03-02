# MonitoringZone schemas - Pydantic models for zone CRUD operations.
# Validates GPS coordinates (latitude -90 to 90, longitude -180 to 180),
# zone radius (positive number in meters), and other zone properties.

from datetime import datetime
from typing import Optional

from pydantic import BaseModel, Field, field_validator


class MonitoringZoneCreate(BaseModel):
    """Schema for creating a new monitoring zone with GPS coordinates and radius."""

    # Zone name - required, max 200 characters
    name: str = Field(
        ..., min_length=1, max_length=200,
        description="Name of the monitoring zone"
    )

    # Optional description of the zone's purpose or location details
    description: Optional[str] = Field(
        None, max_length=1000,
        description="Description of the monitoring zone"
    )

    # GPS latitude - must be between -90 (South Pole) and 90 (North Pole)
    latitude: float = Field(
        ..., ge=-90.0, le=90.0,
        description="GPS latitude (-90 to 90)"
    )

    # GPS longitude - must be between -180 (West) and 180 (East)
    longitude: float = Field(
        ..., ge=-180.0, le=180.0,
        description="GPS longitude (-180 to 180)"
    )

    # Radius in meters defining the circular monitoring area
    radius: float = Field(
        ..., gt=0, le=100000,
        description="Zone radius in meters (must be positive, max 100km)"
    )

    @field_validator("name")
    @classmethod
    def validate_name_not_blank(cls, value):
        """Ensure zone name is not just whitespace."""
        if not value.strip():
            raise ValueError("Zone name cannot be blank")
        return value.strip()


class MonitoringZoneUpdate(BaseModel):
    """Schema for updating an existing monitoring zone - all fields optional."""
    name: Optional[str] = Field(None, min_length=1, max_length=200)
    description: Optional[str] = Field(None, max_length=1000)
    latitude: Optional[float] = Field(None, ge=-90.0, le=90.0)
    longitude: Optional[float] = Field(None, ge=-180.0, le=180.0)
    radius: Optional[float] = Field(None, gt=0, le=100000)
    status: Optional[str] = Field(None, pattern="^(ACTIVE|INACTIVE)$")


class MonitoringZoneResponse(BaseModel):
    """Schema for zone data in API responses including owner info."""
    id: int
    name: str
    description: Optional[str] = None
    latitude: float
    longitude: float
    radius: float
    status: str
    owner_id: int
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None

    model_config = {"from_attributes": True}
