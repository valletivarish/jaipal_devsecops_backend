# SensorReading model - stores individual air quality measurements from sensors.
# Each reading records the pollutant level at a specific zone and time.
# Historical readings are used for trend analysis, dashboard visualization,
# and the ML-based forecasting feature that predicts future pollutant levels.

from sqlalchemy import Column, Integer, Float, DateTime, ForeignKey
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func

from app.database import Base


class SensorReading(Base):
    """
    Sensor reading entity capturing a single air quality measurement.
    Stores the measured pollutant value with zone and pollutant type references.
    Timestamps enable historical analysis and time-series forecasting.
    """
    __tablename__ = "sensor_readings"

    # Primary key
    id = Column(Integer, primary_key=True, index=True)

    # Measured pollutant value in the unit defined by the pollutant type
    # For example, PM2.5 measured in ug/m3
    value = Column(Float, nullable=False)

    # Air Quality Index (AQI) calculated from the raw value
    # AQI provides a standardized 0-500 scale for public communication
    aqi = Column(Integer, nullable=True)

    # Timestamp when the reading was recorded by the sensor
    recorded_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)

    # Foreign keys linking reading to its source zone and pollutant type
    zone_id = Column(Integer, ForeignKey("monitoring_zones.id"), nullable=False)
    pollutant_type_id = Column(Integer, ForeignKey("pollutant_types.id"), nullable=False)

    # Timestamp for database record creation
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    # Relationships for accessing related zone and pollutant information
    zone = relationship("MonitoringZone", back_populates="sensor_readings")
    pollutant_type = relationship("PollutantType", back_populates="sensor_readings")
