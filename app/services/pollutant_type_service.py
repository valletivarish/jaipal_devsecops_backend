# PollutantType Service
# Handles CRUD operations for pollutant types (PM2.5, PM10, NO2, O3, etc.).
# Pollutant types define measurement units and threshold levels
# used for AQI calculation and alert rule evaluation.

from sqlalchemy.orm import Session

from app.models.pollutant_type import PollutantType
from app.schemas.pollutant_type import (
    PollutantTypeCreate,
    PollutantTypeUpdate,
    PollutantTypeResponse,
)
from app.exceptions.handlers import (
    ResourceNotFoundException,
    DuplicateResourceException,
)


def create_pollutant_type(
    db: Session, data: PollutantTypeCreate
) -> PollutantTypeResponse:
    """
    Create a new pollutant type with threshold values.
    Ensures the pollutant name is unique to prevent duplicates.
    """
    # Check for duplicate pollutant name
    existing = db.query(PollutantType).filter(
        PollutantType.name == data.name
    ).first()
    if existing:
        raise DuplicateResourceException("PollutantType", "name", data.name)

    new_pollutant = PollutantType(
        name=data.name,
        description=data.description,
        unit=data.unit,
        safe_threshold=data.safe_threshold,
        warning_threshold=data.warning_threshold,
        danger_threshold=data.danger_threshold,
    )
    db.add(new_pollutant)
    db.commit()
    db.refresh(new_pollutant)
    return PollutantTypeResponse.model_validate(new_pollutant)


def get_pollutant_type_by_id(db: Session, pollutant_id: int) -> PollutantTypeResponse:
    """Retrieve a pollutant type by ID. Raises 404 if not found."""
    pollutant = db.query(PollutantType).filter(
        PollutantType.id == pollutant_id
    ).first()
    if not pollutant:
        raise ResourceNotFoundException("PollutantType", pollutant_id)
    return PollutantTypeResponse.model_validate(pollutant)


def get_all_pollutant_types(
    db: Session, skip: int = 0, limit: int = 50
) -> list[PollutantTypeResponse]:
    """Retrieve all pollutant types with pagination."""
    pollutants = db.query(PollutantType).offset(skip).limit(limit).all()
    return [PollutantTypeResponse.model_validate(p) for p in pollutants]


def update_pollutant_type(
    db: Session, pollutant_id: int, data: PollutantTypeUpdate
) -> PollutantTypeResponse:
    """Update an existing pollutant type. Only provided fields are updated."""
    pollutant = db.query(PollutantType).filter(
        PollutantType.id == pollutant_id
    ).first()
    if not pollutant:
        raise ResourceNotFoundException("PollutantType", pollutant_id)

    update_data = data.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(pollutant, field, value)

    db.commit()
    db.refresh(pollutant)
    return PollutantTypeResponse.model_validate(pollutant)


def delete_pollutant_type(db: Session, pollutant_id: int) -> bool:
    """Delete a pollutant type by ID. Raises 404 if not found."""
    pollutant = db.query(PollutantType).filter(
        PollutantType.id == pollutant_id
    ).first()
    if not pollutant:
        raise ResourceNotFoundException("PollutantType", pollutant_id)

    db.delete(pollutant)
    db.commit()
    return True
