# User model - represents registered users of the air quality monitoring system.
# Users can create monitoring zones, set alert rules, and view sensor data.
# Password hashing is handled at the service layer using passlib bcrypt.

from sqlalchemy import Column, Integer, String, Boolean, DateTime
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func

from app.database import Base


class User(Base):
    """
    User entity for authentication and authorization.
    Stores credentials and profile information.
    Each user can own multiple monitoring zones and alert rules.
    """
    __tablename__ = "users"

    # Primary key - auto-incrementing integer
    id = Column(Integer, primary_key=True, index=True)

    # User credentials - email must be unique for login
    email = Column(String(255), unique=True, index=True, nullable=False)
    username = Column(String(100), unique=True, index=True, nullable=False)

    # Hashed password - never store plain text passwords
    hashed_password = Column(String(255), nullable=False)

    # Profile information
    full_name = Column(String(200), nullable=False)

    # Account status - allows disabling accounts without deletion
    is_active = Column(Boolean, default=True, nullable=False)

    # Timestamps for audit trail
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())

    # Relationships - a user owns monitoring zones and alert rules
    monitoring_zones = relationship(
        "MonitoringZone", back_populates="owner", cascade="all, delete-orphan"
    )
    alert_rules = relationship(
        "AlertRule", back_populates="owner", cascade="all, delete-orphan"
    )
