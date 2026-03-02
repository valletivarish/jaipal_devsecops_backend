# Users Router
# Handles user profile management endpoints (read, update).
# All endpoints require JWT authentication.
# Users can view and update their own profile.

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.database import get_db
from app.schemas.user import UserUpdate, UserResponse
from app.services import user_service
from app.middleware.jwt_middleware import get_current_user
from app.models.user import User

router = APIRouter(prefix="/api/users", tags=["Users"])


@router.get(
    "/",
    response_model=list[UserResponse],
    summary="Get all users (paginated)",
)
def get_all_users(
    skip: int = Query(0, ge=0, description="Number of records to skip"),
    limit: int = Query(20, ge=1, le=100, description="Max records to return"),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Retrieve a paginated list of all registered users."""
    return user_service.get_all_users(db, skip=skip, limit=limit)


@router.get(
    "/{user_id}",
    response_model=UserResponse,
    summary="Get user by ID",
)
def get_user(
    user_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Retrieve a specific user's profile by their ID."""
    return user_service.get_user_by_id(db, user_id)


@router.put(
    "/{user_id}",
    response_model=UserResponse,
    summary="Update user profile",
)
def update_user(
    user_id: int,
    user_data: UserUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    Update a user's profile information.
    Only the user themselves can update their own profile.
    """
    # Enforce that users can only update their own profile
    if current_user.id != user_id:
        from fastapi import HTTPException, status
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You can only update your own profile",
        )
    return user_service.update_user(db, user_id, user_data)
