# Air Quality Monitoring Dashboard - Backend

## Overview

FastAPI-based REST API for real-time air quality monitoring. Provides CRUD operations for monitoring zones, pollutant types, alert rules, and sensor readings. Includes JWT authentication, automatic AQI calculation, and ML-based air quality forecasting using linear regression.

## Tech Stack

- Python 3.11
- FastAPI 0.115
- SQLAlchemy 2.0 (ORM)
- PostgreSQL (database)
- python-jose (JWT authentication)
- scikit-learn (ML forecasting)
- Pydantic V2 (request/response validation)

## Project Structure

```
backend/
  app/
    config.py          - Application configuration (environment variables)
    database.py        - SQLAlchemy engine and session setup
    main.py            - FastAPI application entry point
    models/            - SQLAlchemy ORM models (User, MonitoringZone, etc.)
    schemas/           - Pydantic DTOs with validation annotations
    services/          - Business logic layer
    routers/           - API route handlers (controllers)
    middleware/        - JWT authentication middleware
    exceptions/        - Global exception handlers
  tests/               - pytest test suite with SQLite in-memory database
  requirements.txt     - Python dependencies
  pytest.ini           - Test configuration with coverage settings
  .pylintrc            - Pylint configuration
  .flake8              - Flake8 configuration
```

## API Endpoints

### Authentication
- POST /api/auth/register - Register a new user
- POST /api/auth/login - Login with email/password
- GET /api/auth/me - Get current user profile

### Monitoring Zones
- POST /api/zones/ - Create a monitoring zone
- GET /api/zones/ - List all zones (paginated)
- GET /api/zones/{id} - Get zone by ID
- PUT /api/zones/{id} - Update zone
- DELETE /api/zones/{id} - Delete zone

### Pollutant Types
- POST /api/pollutants/ - Create pollutant type
- GET /api/pollutants/ - List all pollutant types
- GET /api/pollutants/{id} - Get pollutant type by ID
- PUT /api/pollutants/{id} - Update pollutant type
- DELETE /api/pollutants/{id} - Delete pollutant type

### Alert Rules
- POST /api/alerts/ - Create alert rule
- GET /api/alerts/ - List all alert rules
- GET /api/alerts/{id} - Get alert rule by ID
- PUT /api/alerts/{id} - Update alert rule
- DELETE /api/alerts/{id} - Delete alert rule

### Sensor Readings
- POST /api/readings/ - Record a sensor reading
- GET /api/readings/ - List readings (with filters)
- GET /api/readings/{id} - Get reading by ID

### Dashboard
- GET /api/dashboard/summary - Dashboard overview statistics
- GET /api/dashboard/trends - Pollutant trend data for charts
- GET /api/dashboard/comparison - Zone comparison data

### Forecast
- GET /api/forecast/ - ML-based air quality forecast

### Health
- GET /api/health - Health check endpoint

## Setup and Running

1. Create a virtual environment:
   ```
   python -m venv venv
   source venv/bin/activate  (Linux/Mac)
   venv\Scripts\activate     (Windows)
   ```

2. Install dependencies:
   ```
   pip install -r requirements.txt
   ```

3. Set environment variables (or create .env file):
   ```
   DATABASE_URL=postgresql://postgres:root@localhost:5432/air_quality_db
   SECRET_KEY=your-secret-key
   ```

4. Create the PostgreSQL database:
   ```
   createdb air_quality_db
   ```

5. Run the application:
   ```
   uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
   ```

6. Access the API documentation:
   - Swagger UI: http://localhost:8000/api/docs
   - ReDoc: http://localhost:8000/api/redoc

## Running Tests

```
pytest
```

Tests use an SQLite in-memory database, so no PostgreSQL setup is required.

## Static Analysis

```
pylint app/
flake8 app/
bandit -r app/
pip-audit
```

## Input Validation Rules

- GPS coordinates: latitude (-90 to 90), longitude (-180 to 180)
- Zone radius: positive number, max 100,000 meters
- Pollutant thresholds: positive decimals, safe < warning < danger
- Alert severity: LOW, MEDIUM, HIGH, CRITICAL
- Sensor reading values: non-negative decimals
- Email: valid email format
- Password: minimum 8 characters, must contain uppercase, lowercase, and digit
