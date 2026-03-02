# User schemas - Pydantic models for user registration, login, and profile responses.
# Validation rules enforce email format, password strength, and field length limits.
# Separate schemas for creation (with password) and response (without password).

from datetime import datetime
from typing import Optional

from pydantic import BaseModel, EmailStr, Field, field_validator


class UserCreate(BaseModel):
    """Schema for user registration - validates all required fields."""

    # Email must be valid format and is used as the login identifier
    email: EmailStr = Field(..., description="Valid email address for login")

    # Username must be 3-100 characters, alphanumeric with underscores
    username: str = Field(
        ..., min_length=3, max_length=100,
        description="Unique username (3-100 characters)"
    )

    # Password requires minimum 8 characters for security
    password: str = Field(
        ..., min_length=8, max_length=128,
        description="Password (minimum 8 characters)"
    )

    # Full name for display purposes
    full_name: str = Field(
        ..., min_length=1, max_length=200,
        description="User's full name"
    )

    @field_validator("username")
    @classmethod
    def validate_username(cls, value):
        """Ensure username contains only alphanumeric characters and underscores."""
        if not value.replace("_", "").isalnum():
            raise ValueError("Username must contain only letters, numbers, and underscores")
        return value

    @field_validator("password")
    @classmethod
    def validate_password_strength(cls, value):
        """Enforce minimum password complexity requirements."""
        if not any(c.isupper() for c in value):
            raise ValueError("Password must contain at least one uppercase letter")
        if not any(c.islower() for c in value):
            raise ValueError("Password must contain at least one lowercase letter")
        if not any(c.isdigit() for c in value):
            raise ValueError("Password must contain at least one digit")
        return value


class UserLogin(BaseModel):
    """Schema for user login - requires email and password."""
    email: EmailStr = Field(..., description="Registered email address")
    password: str = Field(..., min_length=1, description="Account password")


class UserUpdate(BaseModel):
    """Schema for updating user profile - all fields optional."""
    full_name: Optional[str] = Field(None, min_length=1, max_length=200)
    username: Optional[str] = Field(None, min_length=3, max_length=100)


class UserResponse(BaseModel):
    """Schema for user data in API responses - excludes sensitive password field."""
    id: int
    email: str
    username: str
    full_name: str
    is_active: bool
    created_at: Optional[datetime] = None

    model_config = {"from_attributes": True}


class TokenResponse(BaseModel):
    """Schema for JWT token response after successful authentication."""
    access_token: str
    token_type: str = "bearer"
    user: UserResponse
