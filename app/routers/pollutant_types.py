# PollutantTypes Router
# Handles CRUD endpoints for air pollutant types (PM2.5, PM10, NO2, O3, etc.).
# Pollutant types define measurement units and threshold levels
# used for AQI calculation and alert evaluation.
# All endpoints require JWT authentication.

from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.schemas.pollutant_type import (
    PollutantTypeCreate,
    PollutantTypeUpdate,
    PollutantTypeResponse,
)
from app.services import pollutant_type_service
from app.middleware.jwt_middleware import get_current_user
from app.models.user import User

router = APIRouter(prefix="/api/pollutants", tags=["Pollutant Types"])


@router.post(
    "/",
    response_model=PollutantTypeResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Create a new pollutant type",
)
def create_pollutant(
    data: PollutantTypeCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    Create a pollutant type with name, unit, and threshold values.
    Thresholds must be in ascending order: safe < warning < danger.
    Pollutant names must be unique across the system.
    """
    return pollutant_type_service.create_pollutant_type(db, data)


@router.get(
    "/",
    response_model=list[PollutantTypeResponse],
    summary="Get all pollutant types",
)
def get_all_pollutants(
    skip: int = Query(0, ge=0),
    limit: int = Query(50, ge=1, le=100),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Retrieve all pollutant types with pagination."""
    return pollutant_type_service.get_all_pollutant_types(db, skip=skip, limit=limit)


@router.get(
    "/{pollutant_id}",
    response_model=PollutantTypeResponse,
    summary="Get pollutant type by ID",
)
def get_pollutant(
    pollutant_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Retrieve a specific pollutant type by its ID."""
    return pollutant_type_service.get_pollutant_type_by_id(db, pollutant_id)


@router.put(
    "/{pollutant_id}",
    response_model=PollutantTypeResponse,
    summary="Update a pollutant type",
)
def update_pollutant(
    pollutant_id: int,
    data: PollutantTypeUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Update pollutant type properties. Only provided fields are changed."""
    return pollutant_type_service.update_pollutant_type(db, pollutant_id, data)


@router.delete(
    "/{pollutant_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Delete a pollutant type",
)
def delete_pollutant(
    pollutant_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Delete a pollutant type. Related readings may be affected."""
    pollutant_type_service.delete_pollutant_type(db, pollutant_id)
