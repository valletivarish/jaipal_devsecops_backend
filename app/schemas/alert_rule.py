# AlertRule schemas - Pydantic models for alert rule CRUD operations.
# Validates severity levels (LOW/MEDIUM/HIGH/CRITICAL), threshold values,
# and condition operators (ABOVE/BELOW) for air quality alert triggers.

from datetime import datetime
from typing import Optional

from pydantic import BaseModel, Field, field_validator


# Valid severity levels for alert rules
VALID_SEVERITIES = {"LOW", "MEDIUM", "HIGH", "CRITICAL"}
# Valid condition operators for threshold comparison
VALID_CONDITIONS = {"ABOVE", "BELOW"}


class AlertRuleCreate(BaseModel):
    """Schema for creating an alert rule linking a zone to a pollutant threshold."""

    # Rule name for identification
    name: str = Field(
        ..., min_length=1, max_length=200,
        description="Alert rule name"
    )

    # Threshold value that triggers the alert (must be positive)
    threshold_value: float = Field(
        ..., gt=0,
        description="Pollutant level threshold (positive decimal)"
    )

    # Condition operator - ABOVE means alert when reading > threshold
    condition: str = Field(
        default="ABOVE",
        description="Comparison condition: ABOVE or BELOW"
    )

    # Severity determines notification priority
    severity: str = Field(
        ...,
        description="Alert severity: LOW, MEDIUM, HIGH, or CRITICAL"
    )

    # Whether the rule is active and should trigger alerts
    is_active: bool = Field(default=True, description="Rule active status")

    # Foreign key references to the zone and pollutant type
    zone_id: int = Field(..., gt=0, description="Monitoring zone ID")
    pollutant_type_id: int = Field(..., gt=0, description="Pollutant type ID")

    @field_validator("severity")
    @classmethod
    def validate_severity(cls, value):
        """Ensure severity is one of the valid enum values."""
        upper_val = value.upper()
        if upper_val not in VALID_SEVERITIES:
            raise ValueError(
                f"Severity must be one of: {', '.join(sorted(VALID_SEVERITIES))}"
            )
        return upper_val

    @field_validator("condition")
    @classmethod
    def validate_condition(cls, value):
        """Ensure condition is either ABOVE or BELOW."""
        upper_val = value.upper()
        if upper_val not in VALID_CONDITIONS:
            raise ValueError(
                f"Condition must be one of: {', '.join(sorted(VALID_CONDITIONS))}"
            )
        return upper_val


class AlertRuleUpdate(BaseModel):
    """Schema for updating an alert rule - all fields optional."""
    name: Optional[str] = Field(None, min_length=1, max_length=200)
    threshold_value: Optional[float] = Field(None, gt=0)
    condition: Optional[str] = None
    severity: Optional[str] = None
    is_active: Optional[bool] = None
    zone_id: Optional[int] = Field(None, gt=0)
    pollutant_type_id: Optional[int] = Field(None, gt=0)

    @field_validator("severity")
    @classmethod
    def validate_severity(cls, value):
        """Validate severity if provided."""
        if value is not None:
            upper_val = value.upper()
            if upper_val not in VALID_SEVERITIES:
                raise ValueError(
                    f"Severity must be one of: {', '.join(sorted(VALID_SEVERITIES))}"
                )
            return upper_val
        return value


class AlertRuleResponse(BaseModel):
    """Schema for alert rule data in API responses."""
    id: int
    name: str
    threshold_value: float
    condition: str
    severity: str
    is_active: bool
    zone_id: int
    pollutant_type_id: int
    owner_id: int
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None

    model_config = {"from_attributes": True}
