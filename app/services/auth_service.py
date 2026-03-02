# Authentication Service
# Handles user registration, login, and password management.
# Uses passlib with bcrypt for secure password hashing.
# Generates JWT tokens upon successful authentication.

from sqlalchemy.orm import Session
from passlib.context import CryptContext

from app.models.user import User
from app.schemas.user import UserCreate, UserLogin, TokenResponse, UserResponse
from app.middleware.jwt_middleware import create_access_token
from app.exceptions.handlers import DuplicateResourceException

# Password hashing context using bcrypt algorithm
# bcrypt automatically handles salting and is resistant to rainbow table attacks
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


def hash_password(password: str) -> str:
    """Hash a plain text password using bcrypt."""
    return pwd_context.hash(password)


def verify_password(plain_password: str, hashed_password: str) -> bool:
    """Verify a plain text password against its bcrypt hash."""
    return pwd_context.verify(plain_password, hashed_password)


def register_user(db: Session, user_data: UserCreate) -> TokenResponse:
    """
    Register a new user account.
    Checks for duplicate email and username before creating the user.
    Returns a JWT token so the user is automatically logged in after registration.
    """
    # Check if email is already registered
    existing_email = db.query(User).filter(User.email == user_data.email).first()
    if existing_email:
        raise DuplicateResourceException("User", "email", user_data.email)

    # Check if username is already taken
    existing_username = db.query(User).filter(
        User.username == user_data.username
    ).first()
    if existing_username:
        raise DuplicateResourceException("User", "username", user_data.username)

    # Create the user with a hashed password
    new_user = User(
        email=user_data.email,
        username=user_data.username,
        hashed_password=hash_password(user_data.password),
        full_name=user_data.full_name,
    )
    db.add(new_user)
    db.commit()
    db.refresh(new_user)

    # Generate JWT token for immediate login after registration
    access_token = create_access_token(data={"sub": new_user.email})

    return TokenResponse(
        access_token=access_token,
        user=UserResponse.model_validate(new_user),
    )


def login_user(db: Session, login_data: UserLogin) -> TokenResponse:
    """
    Authenticate a user with email and password.
    Returns a JWT token on success, raises 401 on invalid credentials.
    """
    # Find user by email
    user = db.query(User).filter(User.email == login_data.email).first()

    # Verify the user exists and password matches
    # Using constant-time comparison via passlib to prevent timing attacks
    if not user or not verify_password(login_data.password, user.hashed_password):
        from fastapi import HTTPException, status
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid email or password",
        )

    # Check if account is active
    if not user.is_active:
        from fastapi import HTTPException, status
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="User account is deactivated",
        )

    # Generate and return JWT token
    access_token = create_access_token(data={"sub": user.email})

    return TokenResponse(
        access_token=access_token,
        user=UserResponse.model_validate(user),
    )
