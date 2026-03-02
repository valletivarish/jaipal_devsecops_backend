# Database configuration module
# Sets up SQLAlchemy engine, session factory, and declarative base.
# Uses PostgreSQL as the primary database for storing air quality data.
# The get_db dependency provides a database session per request
# and ensures proper cleanup after each request completes.

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base

from app.config import settings

# Create SQLAlchemy engine with connection pooling
# pool_pre_ping ensures stale connections are detected and replaced
engine = create_engine(
    settings.DATABASE_URL,
    pool_pre_ping=True,
    pool_size=10,
    max_overflow=20,
)

# Session factory - each call creates a new database session
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

# Base class for all ORM models to inherit from
Base = declarative_base()


def get_db():
    """
    Dependency that provides a database session for each request.
    Yields a session and ensures it is closed after the request,
    even if an exception occurs during request processing.
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
