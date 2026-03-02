# Test Configuration and Fixtures
# Sets up an in-memory SQLite database for isolated test execution.
# Provides reusable fixtures for database sessions, test client,
# authenticated user, and sample data objects.
# Using SQLite in-memory avoids needing PostgreSQL for CI/CD testing.

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.database import Base, get_db
from app.main import app
from app.models.user import User
from app.models.monitoring_zone import MonitoringZone
from app.models.pollutant_type import PollutantType
from app.models.alert_rule import AlertRule
from app.models.sensor_reading import SensorReading
from app.services.auth_service import hash_password
from app.middleware.jwt_middleware import create_access_token

# SQLite in-memory database for testing
# StaticPool ensures the same connection is reused across threads
TEST_DATABASE_URL = "sqlite://"

engine = create_engine(
    TEST_DATABASE_URL,
    connect_args={"check_same_thread": False},
    poolclass=StaticPool,
)
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


@pytest.fixture(scope="function")
def db_session():
    """
    Create a fresh database for each test function.
    Tables are created before the test and dropped after it completes.
    """
    Base.metadata.create_all(bind=engine)
    session = TestingSessionLocal()
    try:
        yield session
    finally:
        session.close()
        Base.metadata.drop_all(bind=engine)


@pytest.fixture(scope="function")
def client(db_session):
    """
    Create a FastAPI test client with the test database session.
    Overrides the get_db dependency to use the test database.
    """
    def override_get_db():
        try:
            yield db_session
        finally:
            pass

    app.dependency_overrides[get_db] = override_get_db
    with TestClient(app) as test_client:
        yield test_client
    app.dependency_overrides.clear()


@pytest.fixture
def test_user(db_session):
    """Create a test user in the database and return the user object."""
    user = User(
        email="test@example.com",
        username="testuser",
        hashed_password=hash_password("TestPass123"),
        full_name="Test User",
    )
    db_session.add(user)
    db_session.commit()
    db_session.refresh(user)
    return user


@pytest.fixture
def auth_headers(test_user):
    """Generate JWT authentication headers for the test user."""
    token = create_access_token(data={"sub": test_user.email})
    return {"Authorization": f"Bearer {token}"}


@pytest.fixture
def sample_pollutant(db_session):
    """Create a sample PM2.5 pollutant type for testing."""
    pollutant = PollutantType(
        name="PM2.5",
        description="Fine particulate matter smaller than 2.5 micrometers",
        unit="ug/m3",
        safe_threshold=12.0,
        warning_threshold=35.4,
        danger_threshold=55.4,
    )
    db_session.add(pollutant)
    db_session.commit()
    db_session.refresh(pollutant)
    return pollutant


@pytest.fixture
def sample_zone(db_session, test_user):
    """Create a sample monitoring zone for testing."""
    zone = MonitoringZone(
        name="Dublin City Centre",
        description="Central Dublin monitoring zone",
        latitude=53.3498,
        longitude=-6.2603,
        radius=5000.0,
        owner_id=test_user.id,
    )
    db_session.add(zone)
    db_session.commit()
    db_session.refresh(zone)
    return zone
