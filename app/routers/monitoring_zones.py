# MonitoringZones Router
# Handles CRUD endpoints for monitoring zones.
# Zones define geographic areas with GPS coordinates and radius
# where air quality is measured. All endpoints require authentication.

from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.schemas.monitoring_zone import (
    MonitoringZoneCreate,
    MonitoringZoneUpdate,
    MonitoringZoneResponse,
)
from app.services import monitoring_zone_service
from app.middleware.jwt_middleware import get_current_user
from app.models.user import User

router = APIRouter(prefix="/api/zones", tags=["Monitoring Zones"])


@router.post(
    "/",
    response_model=MonitoringZoneResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Create a new monitoring zone",
)
def create_zone(
    zone_data: MonitoringZoneCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    Create a monitoring zone with GPS coordinates and radius.
    Latitude must be -90 to 90, longitude -180 to 180.
    The zone is automatically assigned to the authenticated user.
    """
    return monitoring_zone_service.create_zone(db, zone_data, current_user.id)


@router.get(
    "/",
    response_model=list[MonitoringZoneResponse],
    summary="Get all monitoring zones (paginated)",
)
def get_all_zones(
    skip: int = Query(0, ge=0, description="Records to skip"),
    limit: int = Query(20, ge=1, le=100, description="Max records"),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Retrieve all monitoring zones with pagination support."""
    return monitoring_zone_service.get_all_zones(db, skip=skip, limit=limit)


@router.get(
    "/my",
    response_model=list[MonitoringZoneResponse],
    summary="Get zones owned by current user",
)
def get_my_zones(
    skip: int = Query(0, ge=0),
    limit: int = Query(20, ge=1, le=100),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Retrieve monitoring zones owned by the authenticated user."""
    return monitoring_zone_service.get_zones_by_owner(
        db, current_user.id, skip=skip, limit=limit
    )


@router.get(
    "/{zone_id}",
    response_model=MonitoringZoneResponse,
    summary="Get monitoring zone by ID",
)
def get_zone(
    zone_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Retrieve a specific monitoring zone by its ID."""
    return monitoring_zone_service.get_zone_by_id(db, zone_id)


@router.put(
    "/{zone_id}",
    response_model=MonitoringZoneResponse,
    summary="Update a monitoring zone",
)
def update_zone(
    zone_id: int,
    zone_data: MonitoringZoneUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    Update zone properties. Only the zone owner can make changes.
    All fields are optional - only provided fields are updated.
    """
    return monitoring_zone_service.update_zone(
        db, zone_id, zone_data, current_user.id
    )


@router.delete(
    "/{zone_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Delete a monitoring zone",
)
def delete_zone(
    zone_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    Delete a monitoring zone and all its associated readings and alert rules.
    Only the zone owner can delete it. This action is irreversible.
    """
    monitoring_zone_service.delete_zone(db, zone_id, current_user.id)
