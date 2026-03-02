# Authentication Router
# Handles user registration and login endpoints.
# Registration creates a new user account and returns a JWT token.
# Login validates credentials and returns a JWT token for subsequent requests.
# These endpoints are public (no authentication required).

from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.schemas.user import UserCreate, UserLogin, TokenResponse, UserResponse
from app.services import auth_service
from app.middleware.jwt_middleware import get_current_user
from app.models.user import User

# Router prefix groups all auth endpoints under /api/auth
router = APIRouter(prefix="/api/auth", tags=["Authentication"])


@router.post(
    "/register",
    response_model=TokenResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Register a new user account",
)
def register(user_data: UserCreate, db: Session = Depends(get_db)):
    """
    Register a new user with email, username, password, and full name.
    Returns a JWT access token so the user is logged in immediately.
    Validates email format, password strength, and unique constraints.
    """
    return auth_service.register_user(db, user_data)


@router.post(
    "/login",
    response_model=TokenResponse,
    summary="Login with email and password",
)
def login(login_data: UserLogin, db: Session = Depends(get_db)):
    """
    Authenticate user with email and password.
    Returns a JWT access token valid for 24 hours.
    Returns 401 if credentials are invalid.
    """
    return auth_service.login_user(db, login_data)


@router.get(
    "/me",
    response_model=UserResponse,
    summary="Get current user profile",
)
def get_current_user_profile(current_user: User = Depends(get_current_user)):
    """
    Retrieve the profile of the currently authenticated user.
    Requires a valid JWT token in the Authorization header.
    """
    return UserResponse.model_validate(current_user)
