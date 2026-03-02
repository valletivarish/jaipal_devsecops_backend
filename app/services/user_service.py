# User Service
# Handles user profile management operations (read, update).
# User creation is handled by auth_service (registration flow).
# All operations require authentication via JWT token.

from sqlalchemy.orm import Session

from app.models.user import User
from app.schemas.user import UserUpdate, UserResponse
from app.exceptions.handlers import ResourceNotFoundException


def get_user_by_id(db: Session, user_id: int) -> UserResponse:
    """Retrieve a user profile by ID. Raises 404 if not found."""
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise ResourceNotFoundException("User", user_id)
    return UserResponse.model_validate(user)


def update_user(db: Session, user_id: int, user_data: UserUpdate) -> UserResponse:
    """
    Update user profile fields. Only updates fields that are provided
    (non-None) in the request, leaving other fields unchanged.
    """
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise ResourceNotFoundException("User", user_id)

    # Only update fields that were explicitly provided in the request
    update_data = user_data.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(user, field, value)

    db.commit()
    db.refresh(user)
    return UserResponse.model_validate(user)


def get_all_users(db: Session, skip: int = 0, limit: int = 20) -> list[UserResponse]:
    """Retrieve a paginated list of all users."""
    users = db.query(User).offset(skip).limit(limit).all()
    return [UserResponse.model_validate(user) for user in users]
