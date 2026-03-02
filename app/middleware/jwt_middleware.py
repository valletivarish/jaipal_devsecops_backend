# JWT Authentication Middleware
# Provides the get_current_user dependency for protecting API endpoints.
# Extracts and validates JWT tokens from the Authorization header.
# Tokens are created during login and must be included in subsequent requests.
# Uses the python-jose library for JWT encoding/decoding with HS256 algorithm.

from datetime import datetime, timedelta, timezone

from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from jose import JWTError, jwt
from sqlalchemy.orm import Session

from app.config import settings
from app.database import get_db
from app.models.user import User

# OAuth2 scheme extracts the Bearer token from the Authorization header
# The tokenUrl points to the login endpoint for Swagger UI integration
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/auth/login")


def create_access_token(data: dict) -> str:
    """
    Create a JWT access token with an expiration time.
    The token payload includes the user's email as the 'sub' (subject) claim
    and an 'exp' (expiration) claim for automatic token invalidation.
    """
    to_encode = data.copy()
    expire = datetime.now(timezone.utc) + timedelta(
        minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES
    )
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(
        to_encode, settings.SECRET_KEY, algorithm=settings.ALGORITHM
    )
    return encoded_jwt


def get_current_user(
    token: str = Depends(oauth2_scheme),
    db: Session = Depends(get_db),
) -> User:
    """
    FastAPI dependency that extracts and validates the JWT token,
    then returns the corresponding User object from the database.
    Raises 401 Unauthorized if the token is invalid, expired, or
    the user no longer exists in the database.
    """
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )
    try:
        # Decode the JWT token and extract the email (subject claim)
        payload = jwt.decode(
            token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM]
        )
        email: str = payload.get("sub")
        if email is None:
            raise credentials_exception
    except JWTError:
        # Token is invalid, expired, or tampered with
        raise credentials_exception

    # Look up the user in the database by email
    user = db.query(User).filter(User.email == email).first()
    if user is None:
        raise credentials_exception

    # Check if the user account is still active
    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="User account is deactivated",
        )

    return user
