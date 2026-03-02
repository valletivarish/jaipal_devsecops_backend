# SensorReading Service
# Handles creation and retrieval of air quality sensor readings.
# Sensor readings are immutable once created (no update/delete for data integrity).
# Supports filtering by zone, pollutant type, and date range.
# Calculates AQI (Air Quality Index) automatically from raw pollutant values.

from datetime import datetime, timezone

from sqlalchemy.orm import Session
from sqlalchemy import func

from app.models.sensor_reading import SensorReading
from app.models.pollutant_type import PollutantType
from app.models.monitoring_zone import MonitoringZone
from app.schemas.sensor_reading import SensorReadingCreate, SensorReadingResponse
from app.exceptions.handlers import ResourceNotFoundException


def calculate_aqi(value: float, pollutant: PollutantType) -> int:
    """
    Calculate the Air Quality Index (AQI) from a raw pollutant value.
    Uses a simplified linear interpolation based on the pollutant's
    safe, warning, and danger thresholds mapped to AQI ranges:
    - 0-50 (Good): value <= safe_threshold
    - 51-100 (Moderate): safe < value <= warning
    - 101-200 (Unhealthy): warning < value <= danger
    - 201-500 (Hazardous): value > danger
    """
    if value <= pollutant.safe_threshold:
        # Good range: scale 0-50 based on how close to safe threshold
        ratio = value / pollutant.safe_threshold if pollutant.safe_threshold > 0 else 0
        return int(ratio * 50)
    elif value <= pollutant.warning_threshold:
        # Moderate range: scale 51-100
        range_size = pollutant.warning_threshold - pollutant.safe_threshold
        ratio = (value - pollutant.safe_threshold) / range_size if range_size > 0 else 0
        return int(51 + ratio * 49)
    elif value <= pollutant.danger_threshold:
        # Unhealthy range: scale 101-200
        range_size = pollutant.danger_threshold - pollutant.warning_threshold
        ratio = (value - pollutant.warning_threshold) / range_size if range_size > 0 else 0
        return int(101 + ratio * 99)
    else:
        # Hazardous range: scale 201-500
        overshoot = value - pollutant.danger_threshold
        ratio = min(overshoot / pollutant.danger_threshold, 1.0)
        return int(201 + ratio * 299)


def create_sensor_reading(
    db: Session, data: SensorReadingCreate
) -> SensorReadingResponse:
    """
    Record a new sensor reading with automatic AQI calculation.
    Validates that the referenced zone and pollutant type exist.
    """
    # Verify the monitoring zone exists
    zone = db.query(MonitoringZone).filter(
        MonitoringZone.id == data.zone_id
    ).first()
    if not zone:
        raise ResourceNotFoundException("MonitoringZone", data.zone_id)

    # Verify the pollutant type exists and retrieve for AQI calculation
    pollutant = db.query(PollutantType).filter(
        PollutantType.id == data.pollutant_type_id
    ).first()
    if not pollutant:
        raise ResourceNotFoundException("PollutantType", data.pollutant_type_id)

    # Calculate AQI if not provided in the request
    aqi_value = data.aqi if data.aqi is not None else calculate_aqi(data.value, pollutant)

    new_reading = SensorReading(
        value=data.value,
        aqi=aqi_value,
        recorded_at=data.recorded_at or datetime.now(timezone.utc),
        zone_id=data.zone_id,
        pollutant_type_id=data.pollutant_type_id,
    )
    db.add(new_reading)
    db.commit()
    db.refresh(new_reading)
    return SensorReadingResponse.model_validate(new_reading)


def get_reading_by_id(db: Session, reading_id: int) -> SensorReadingResponse:
    """Retrieve a single sensor reading by ID."""
    reading = db.query(SensorReading).filter(
        SensorReading.id == reading_id
    ).first()
    if not reading:
        raise ResourceNotFoundException("SensorReading", reading_id)
    return SensorReadingResponse.model_validate(reading)


def get_readings(
    db: Session,
    zone_id: int = None,
    pollutant_type_id: int = None,
    start_date: datetime = None,
    end_date: datetime = None,
    skip: int = 0,
    limit: int = 100,
) -> list[SensorReadingResponse]:
    """
    Retrieve sensor readings with optional filters.
    Supports filtering by zone, pollutant type, and date range.
    Results are ordered by recorded_at descending (newest first).
    """
    query = db.query(SensorReading)

    # Apply optional filters
    if zone_id:
        query = query.filter(SensorReading.zone_id == zone_id)
    if pollutant_type_id:
        query = query.filter(SensorReading.pollutant_type_id == pollutant_type_id)
    if start_date:
        query = query.filter(SensorReading.recorded_at >= start_date)
    if end_date:
        query = query.filter(SensorReading.recorded_at <= end_date)

    # Order by most recent first and apply pagination
    readings = (
        query.order_by(SensorReading.recorded_at.desc())
        .offset(skip)
        .limit(limit)
        .all()
    )
    return [SensorReadingResponse.model_validate(r) for r in readings]


def get_latest_reading_per_zone(db: Session) -> list[dict]:
    """
    Retrieve the most recent sensor reading for each monitoring zone.
    Used by the dashboard to show current air quality status per zone.
    """
    # Subquery to find the latest reading timestamp per zone
    subquery = (
        db.query(
            SensorReading.zone_id,
            func.max(SensorReading.recorded_at).label("max_recorded_at"),
        )
        .group_by(SensorReading.zone_id)
        .subquery()
    )

    # Join with the readings table to get the full reading data
    readings = (
        db.query(SensorReading)
        .join(
            subquery,
            (SensorReading.zone_id == subquery.c.zone_id)
            & (SensorReading.recorded_at == subquery.c.max_recorded_at),
        )
        .all()
    )

    return [
        {
            "zone_id": r.zone_id,
            "value": r.value,
            "aqi": r.aqi,
            "recorded_at": r.recorded_at,
            "pollutant_type_id": r.pollutant_type_id,
        }
        for r in readings
    ]
