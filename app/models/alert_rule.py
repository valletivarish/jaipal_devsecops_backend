# AlertRule model - defines threshold-based alert conditions for air quality monitoring.
# When a sensor reading exceeds the configured threshold for a specific pollutant
# in a particular zone, an alert notification is triggered.
# Alert rules have severity levels (LOW, MEDIUM, HIGH, CRITICAL) to prioritize responses.

from sqlalchemy import Column, Integer, String, Float, DateTime, ForeignKey, Boolean
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func

from app.database import Base


class AlertRule(Base):
    """
    Alert rule entity that defines when notifications should be sent.
    Links a monitoring zone to a pollutant type with a threshold value.
    When sensor readings exceed the threshold, alerts are triggered.
    """
    __tablename__ = "alert_rules"

    # Primary key
    id = Column(Integer, primary_key=True, index=True)

    # Rule identification and description
    name = Column(String(200), nullable=False)

    # Threshold value - the pollutant level that triggers the alert
    # Must be a positive decimal value in the pollutant's unit of measurement
    threshold_value = Column(Float, nullable=False)

    # Comparison operator for threshold evaluation
    # "ABOVE" triggers when reading > threshold, "BELOW" when reading < threshold
    condition = Column(String(20), default="ABOVE", nullable=False)

    # Severity level determines notification priority and urgency
    # Valid values: LOW, MEDIUM, HIGH, CRITICAL
    severity = Column(String(20), nullable=False)

    # Whether the rule is currently active and should trigger alerts
    is_active = Column(Boolean, default=True, nullable=False)

    # Foreign keys linking the rule to a zone, pollutant, and owner
    zone_id = Column(Integer, ForeignKey("monitoring_zones.id"), nullable=False)
    pollutant_type_id = Column(Integer, ForeignKey("pollutant_types.id"), nullable=False)
    owner_id = Column(Integer, ForeignKey("users.id"), nullable=False)

    # Timestamps
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())

    # Relationships
    zone = relationship("MonitoringZone", back_populates="alert_rules")
    pollutant_type = relationship("PollutantType", back_populates="alert_rules")
    owner = relationship("User", back_populates="alert_rules")
