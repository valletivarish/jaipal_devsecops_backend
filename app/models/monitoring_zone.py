# MonitoringZone model - represents a geographic area being monitored for air quality.
# Each zone is defined by GPS coordinates (latitude/longitude) and a radius in meters.
# Zones are owned by users and can have multiple alert rules and sensor readings.

from sqlalchemy import Column, Integer, String, Float, DateTime, ForeignKey, Text
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func

from app.database import Base


class MonitoringZone(Base):
    """
    Monitoring zone entity defining a geographic area for air quality tracking.
    GPS coordinates mark the center point and radius defines the coverage area.
    Sensor readings and alert rules are associated with specific zones.
    """
    __tablename__ = "monitoring_zones"

    # Primary key
    id = Column(Integer, primary_key=True, index=True)

    # Zone identification
    name = Column(String(200), nullable=False)
    description = Column(Text, nullable=True)

    # GPS coordinates defining the center of the monitoring zone
    # Latitude ranges from -90 to 90 (south to north)
    latitude = Column(Float, nullable=False)
    # Longitude ranges from -180 to 180 (west to east)
    longitude = Column(Float, nullable=False)

    # Radius in meters defining the circular monitoring area
    radius = Column(Float, nullable=False)

    # Zone status - active zones receive sensor readings
    status = Column(String(20), default="ACTIVE", nullable=False)

    # Foreign key linking zone to its creator/owner
    owner_id = Column(Integer, ForeignKey("users.id"), nullable=False)

    # Timestamps for tracking zone creation and modifications
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())

    # Relationships
    owner = relationship("User", back_populates="monitoring_zones")
    alert_rules = relationship(
        "AlertRule", back_populates="zone", cascade="all, delete-orphan"
    )
    sensor_readings = relationship(
        "SensorReading", back_populates="zone", cascade="all, delete-orphan"
    )
