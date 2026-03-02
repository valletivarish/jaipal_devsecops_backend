# MonitoringZone Service
# Handles all CRUD operations for monitoring zones.
# Zones define geographic areas with GPS coordinates and radius
# where air quality sensors collect data.
# Zone ownership is enforced - users can only modify their own zones.

from sqlalchemy.orm import Session

from app.models.monitoring_zone import MonitoringZone
from app.schemas.monitoring_zone import (
    MonitoringZoneCreate,
    MonitoringZoneUpdate,
    MonitoringZoneResponse,
)
from app.exceptions.handlers import ResourceNotFoundException


def create_zone(
    db: Session, zone_data: MonitoringZoneCreate, owner_id: int
) -> MonitoringZoneResponse:
    """
    Create a new monitoring zone with the provided GPS coordinates.
    The zone is automatically assigned to the authenticated user.
    """
    new_zone = MonitoringZone(
        name=zone_data.name,
        description=zone_data.description,
        latitude=zone_data.latitude,
        longitude=zone_data.longitude,
        radius=zone_data.radius,
        owner_id=owner_id,
    )
    db.add(new_zone)
    db.commit()
    db.refresh(new_zone)
    return MonitoringZoneResponse.model_validate(new_zone)


def get_zone_by_id(db: Session, zone_id: int) -> MonitoringZoneResponse:
    """Retrieve a monitoring zone by its ID. Raises 404 if not found."""
    zone = db.query(MonitoringZone).filter(MonitoringZone.id == zone_id).first()
    if not zone:
        raise ResourceNotFoundException("MonitoringZone", zone_id)
    return MonitoringZoneResponse.model_validate(zone)


def get_all_zones(
    db: Session, skip: int = 0, limit: int = 20
) -> list[MonitoringZoneResponse]:
    """Retrieve a paginated list of all monitoring zones."""
    zones = db.query(MonitoringZone).offset(skip).limit(limit).all()
    return [MonitoringZoneResponse.model_validate(zone) for zone in zones]


def get_zones_by_owner(
    db: Session, owner_id: int, skip: int = 0, limit: int = 20
) -> list[MonitoringZoneResponse]:
    """Retrieve all monitoring zones owned by a specific user."""
    zones = (
        db.query(MonitoringZone)
        .filter(MonitoringZone.owner_id == owner_id)
        .offset(skip)
        .limit(limit)
        .all()
    )
    return [MonitoringZoneResponse.model_validate(zone) for zone in zones]


def update_zone(
    db: Session, zone_id: int, zone_data: MonitoringZoneUpdate, owner_id: int
) -> MonitoringZoneResponse:
    """
    Update an existing monitoring zone. Only the zone owner can update it.
    Only fields provided in the request are updated.
    """
    zone = db.query(MonitoringZone).filter(MonitoringZone.id == zone_id).first()
    if not zone:
        raise ResourceNotFoundException("MonitoringZone", zone_id)

    # Enforce ownership - only the creator can modify the zone
    if zone.owner_id != owner_id:
        from fastapi import HTTPException, status
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You do not have permission to update this zone",
        )

    # Apply partial updates - only change fields that were provided
    update_data = zone_data.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(zone, field, value)

    db.commit()
    db.refresh(zone)
    return MonitoringZoneResponse.model_validate(zone)


def delete_zone(db: Session, zone_id: int, owner_id: int) -> bool:
    """
    Delete a monitoring zone and all associated readings and alert rules.
    Only the zone owner can delete it. Cascade delete removes related records.
    """
    zone = db.query(MonitoringZone).filter(MonitoringZone.id == zone_id).first()
    if not zone:
        raise ResourceNotFoundException("MonitoringZone", zone_id)

    # Enforce ownership
    if zone.owner_id != owner_id:
        from fastapi import HTTPException, status
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You do not have permission to delete this zone",
        )

    db.delete(zone)
    db.commit()
    return True
