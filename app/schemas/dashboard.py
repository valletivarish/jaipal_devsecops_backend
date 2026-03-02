# Dashboard schemas - Pydantic models for dashboard summary data,
# zone comparison data, and forecast responses.
# These schemas define the structure for aggregated analytics endpoints.

from datetime import datetime
from typing import Optional

from pydantic import BaseModel


class ZoneSummary(BaseModel):
    """Summary of current air quality status for a single monitoring zone."""
    zone_id: int
    zone_name: str
    latest_aqi: Optional[int] = None
    # AQI category: Good, Moderate, Unhealthy for Sensitive Groups, Unhealthy, Very Unhealthy, Hazardous
    aqi_category: str = "Unknown"
    # Number of active alert rules in the zone
    active_alerts: int = 0
    # Total sensor readings recorded for this zone
    total_readings: int = 0


class DashboardSummary(BaseModel):
    """Aggregated dashboard data for the main overview page."""
    # Total counts across all zones
    total_zones: int = 0
    total_active_alerts: int = 0
    total_readings_today: int = 0
    # Average AQI across all zones with recent readings
    average_aqi: Optional[float] = None
    # Per-zone summaries for the zone cards
    zone_summaries: list[ZoneSummary] = []


class TrendDataPoint(BaseModel):
    """Single data point in a pollutant trend time series."""
    timestamp: datetime
    value: float
    aqi: Optional[int] = None


class PollutantTrend(BaseModel):
    """Historical trend data for a specific pollutant in a zone."""
    zone_id: int
    zone_name: str
    pollutant_name: str
    pollutant_unit: str
    data_points: list[TrendDataPoint] = []


class ForecastDataPoint(BaseModel):
    """Single predicted data point from the ML forecasting model."""
    date: str
    predicted_value: float
    # Upper and lower bounds of the prediction confidence interval
    lower_bound: float
    upper_bound: float


class ForecastResponse(BaseModel):
    """Response from the air quality forecasting endpoint."""
    zone_id: int
    zone_name: str
    pollutant_name: str
    pollutant_unit: str
    # Direction of the predicted trend: INCREASING, DECREASING, or STABLE
    trend_direction: str
    # R-squared confidence score of the regression model (0 to 1)
    confidence_score: float
    # Predicted values for the next 7 days
    forecast_data: list[ForecastDataPoint] = []


class ZoneComparison(BaseModel):
    """Comparison data for multiple zones showing average pollutant levels."""
    zone_id: int
    zone_name: str
    pollutant_name: str
    average_value: float
    max_value: float
    min_value: float
    reading_count: int
