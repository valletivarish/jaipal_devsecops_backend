# Dashboard Service
# Aggregates data from multiple entities to build the dashboard overview.
# Provides zone summaries with current AQI, trend data for charts,
# and zone comparison analytics for the dashboard page.

from datetime import datetime, timedelta, timezone

from sqlalchemy.orm import Session
from sqlalchemy import func

from app.models.monitoring_zone import MonitoringZone
from app.models.sensor_reading import SensorReading
from app.models.alert_rule import AlertRule
from app.models.pollutant_type import PollutantType
from app.schemas.dashboard import (
    DashboardSummary,
    ZoneSummary,
    PollutantTrend,
    TrendDataPoint,
    ZoneComparison,
)


def get_aqi_category(aqi: int) -> str:
    """
    Convert a numeric AQI value to its category label.
    Categories follow the EPA Air Quality Index standard:
    0-50: Good, 51-100: Moderate, 101-150: Unhealthy for Sensitive Groups,
    151-200: Unhealthy, 201-300: Very Unhealthy, 301-500: Hazardous
    """
    if aqi <= 50:
        return "Good"
    elif aqi <= 100:
        return "Moderate"
    elif aqi <= 150:
        return "Unhealthy for Sensitive Groups"
    elif aqi <= 200:
        return "Unhealthy"
    elif aqi <= 300:
        return "Very Unhealthy"
    else:
        return "Hazardous"


def get_dashboard_summary(db: Session) -> DashboardSummary:
    """
    Build the dashboard summary with zone counts, alert counts,
    today's reading count, average AQI, and per-zone summaries.
    """
    # Count total monitoring zones
    total_zones = db.query(func.count(MonitoringZone.id)).scalar()

    # Count active alert rules across all zones
    total_active_alerts = (
        db.query(func.count(AlertRule.id))
        .filter(AlertRule.is_active == True)
        .scalar()
    )

    # Count sensor readings recorded today
    today_start = datetime.now(timezone.utc).replace(
        hour=0, minute=0, second=0, microsecond=0
    )
    total_readings_today = (
        db.query(func.count(SensorReading.id))
        .filter(SensorReading.recorded_at >= today_start)
        .scalar()
    )

    # Calculate average AQI from the latest readings across all zones
    latest_aqi_values = (
        db.query(func.avg(SensorReading.aqi))
        .filter(SensorReading.recorded_at >= today_start)
        .scalar()
    )
    average_aqi = round(float(latest_aqi_values), 1) if latest_aqi_values else None

    # Build per-zone summaries
    zone_summaries = []
    zones = db.query(MonitoringZone).all()
    for zone in zones:
        # Get the latest AQI for this zone
        latest_reading = (
            db.query(SensorReading)
            .filter(SensorReading.zone_id == zone.id)
            .order_by(SensorReading.recorded_at.desc())
            .first()
        )
        latest_aqi = latest_reading.aqi if latest_reading else None
        aqi_category = get_aqi_category(latest_aqi) if latest_aqi else "Unknown"

        # Count active alerts for this zone
        zone_active_alerts = (
            db.query(func.count(AlertRule.id))
            .filter(AlertRule.zone_id == zone.id, AlertRule.is_active == True)
            .scalar()
        )

        # Count total readings for this zone
        zone_total_readings = (
            db.query(func.count(SensorReading.id))
            .filter(SensorReading.zone_id == zone.id)
            .scalar()
        )

        zone_summaries.append(
            ZoneSummary(
                zone_id=zone.id,
                zone_name=zone.name,
                latest_aqi=latest_aqi,
                aqi_category=aqi_category,
                active_alerts=zone_active_alerts,
                total_readings=zone_total_readings,
            )
        )

    return DashboardSummary(
        total_zones=total_zones,
        total_active_alerts=total_active_alerts,
        total_readings_today=total_readings_today,
        average_aqi=average_aqi,
        zone_summaries=zone_summaries,
    )


def get_pollutant_trend(
    db: Session, zone_id: int, pollutant_type_id: int, days: int = 30
) -> PollutantTrend:
    """
    Get historical pollutant level trend data for a specific zone and pollutant.
    Returns time-series data points for charting over the specified number of days.
    """
    # Get zone and pollutant info for the response
    zone = db.query(MonitoringZone).filter(MonitoringZone.id == zone_id).first()
    pollutant = db.query(PollutantType).filter(
        PollutantType.id == pollutant_type_id
    ).first()

    zone_name = zone.name if zone else "Unknown"
    pollutant_name = pollutant.name if pollutant else "Unknown"
    pollutant_unit = pollutant.unit if pollutant else ""

    # Query readings for the specified time period
    start_date = datetime.now(timezone.utc) - timedelta(days=days)
    readings = (
        db.query(SensorReading)
        .filter(
            SensorReading.zone_id == zone_id,
            SensorReading.pollutant_type_id == pollutant_type_id,
            SensorReading.recorded_at >= start_date,
        )
        .order_by(SensorReading.recorded_at.asc())
        .all()
    )

    # Convert to trend data points
    data_points = [
        TrendDataPoint(
            timestamp=r.recorded_at,
            value=r.value,
            aqi=r.aqi,
        )
        for r in readings
    ]

    return PollutantTrend(
        zone_id=zone_id,
        zone_name=zone_name,
        pollutant_name=pollutant_name,
        pollutant_unit=pollutant_unit,
        data_points=data_points,
    )


def get_zone_comparison(
    db: Session, pollutant_type_id: int, days: int = 7
) -> list[ZoneComparison]:
    """
    Compare pollutant levels across all monitoring zones.
    Returns average, max, and min values per zone for the specified period.
    Used for the zone comparison chart on the dashboard.
    """
    start_date = datetime.now(timezone.utc) - timedelta(days=days)

    # Get pollutant info
    pollutant = db.query(PollutantType).filter(
        PollutantType.id == pollutant_type_id
    ).first()
    pollutant_name = pollutant.name if pollutant else "Unknown"

    # Aggregate readings per zone
    results = (
        db.query(
            SensorReading.zone_id,
            MonitoringZone.name.label("zone_name"),
            func.avg(SensorReading.value).label("avg_value"),
            func.max(SensorReading.value).label("max_value"),
            func.min(SensorReading.value).label("min_value"),
            func.count(SensorReading.id).label("count"),
        )
        .join(MonitoringZone, SensorReading.zone_id == MonitoringZone.id)
        .filter(
            SensorReading.pollutant_type_id == pollutant_type_id,
            SensorReading.recorded_at >= start_date,
        )
        .group_by(SensorReading.zone_id, MonitoringZone.name)
        .all()
    )

    return [
        ZoneComparison(
            zone_id=r.zone_id,
            zone_name=r.zone_name,
            pollutant_name=pollutant_name,
            average_value=round(float(r.avg_value), 2),
            max_value=float(r.max_value),
            min_value=float(r.min_value),
            reading_count=r.count,
        )
        for r in results
    ]
