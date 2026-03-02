# PollutantType model - represents different air pollutants being tracked.
# Common pollutants include PM2.5, PM10, NO2, O3, SO2, and CO.
# Each pollutant type has a unit of measurement and safe threshold values
# used to calculate the Air Quality Index (AQI).

from sqlalchemy import Column, Integer, String, Float, DateTime, Text
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func

from app.database import Base


class PollutantType(Base):
    """
    Pollutant type entity storing metadata about tracked air pollutants.
    Includes measurement units, safe/hazardous thresholds, and descriptions.
    Referenced by sensor readings and alert rules to identify pollutant levels.
    """
    __tablename__ = "pollutant_types"

    # Primary key
    id = Column(Integer, primary_key=True, index=True)

    # Pollutant identification - e.g., "PM2.5", "NO2", "O3"
    name = Column(String(50), unique=True, nullable=False)

    # Human-readable description of the pollutant and health effects
    description = Column(Text, nullable=True)

    # Unit of measurement - e.g., "ug/m3" (micrograms per cubic meter), "ppm"
    unit = Column(String(20), nullable=False)

    # Threshold values for AQI categorization
    # safe_threshold: level below which air quality is considered good
    safe_threshold = Column(Float, nullable=False)
    # warning_threshold: level above which sensitive groups may be affected
    warning_threshold = Column(Float, nullable=False)
    # danger_threshold: level above which air quality is hazardous for everyone
    danger_threshold = Column(Float, nullable=False)

    # Timestamps
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())

    # Relationships - pollutant type is referenced by readings and alert rules
    sensor_readings = relationship(
        "SensorReading", back_populates="pollutant_type"
    )
    alert_rules = relationship(
        "AlertRule", back_populates="pollutant_type"
    )
