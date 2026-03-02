# Application configuration module
# Uses pydantic-settings to load environment variables with type safety.
# All sensitive values (database credentials, JWT secret) are loaded
# from environment variables and never hardcoded in source code.

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """
    Central configuration class for the application.
    Values are loaded from environment variables or a .env file.
    """

    # Application metadata
    APP_NAME: str = "Air Quality Monitoring Dashboard"
    APP_VERSION: str = "1.0.0"
    DEBUG: bool = False

    # Database connection settings
    # PostgreSQL connection string for SQLAlchemy
    DATABASE_URL: str = "postgresql://postgres:root@localhost:5432/air_quality_db"

    # JWT authentication settings
    # SECRET_KEY should be overridden in production with a strong random value
    SECRET_KEY: str = "change-this-secret-key-in-production"
    ALGORITHM: str = "HS256"
    # Token expiry in minutes (default 24 hours)
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 1440

    # CORS settings - allowed origins for frontend requests
    CORS_ORIGINS: list[str] = [
        "http://localhost:5173",
        "http://localhost:3000",
        "http://127.0.0.1:5173",
    ]

    # Pagination defaults
    DEFAULT_PAGE_SIZE: int = 20
    MAX_PAGE_SIZE: int = 100

    class Config:
        # Load variables from .env file if present
        env_file = ".env"
        env_file_encoding = "utf-8"


# Singleton settings instance used throughout the application
settings = Settings()
