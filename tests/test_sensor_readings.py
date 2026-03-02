# SensorReading Tests
# Tests for sensor reading creation and retrieval endpoints.
# Validates value ranges, AQI calculation, and filtering functionality.

import pytest


class TestCreateReading:
    """Test suite for POST /api/readings/ endpoint."""

    def test_create_reading_success(
        self, client, auth_headers, sample_zone, sample_pollutant
    ):
        """Test recording a sensor reading with valid data."""
        response = client.post("/api/readings/", json={
            "value": 25.5,
            "zone_id": sample_zone.id,
            "pollutant_type_id": sample_pollutant.id,
        }, headers=auth_headers)
        assert response.status_code == 201
        data = response.json()
        assert data["value"] == 25.5
        # AQI should be automatically calculated
        assert data["aqi"] is not None

    def test_create_reading_invalid_zone(
        self, client, auth_headers, sample_pollutant
    ):
        """Test reading creation fails with non-existent zone."""
        response = client.post("/api/readings/", json={
            "value": 25.5,
            "zone_id": 9999,
            "pollutant_type_id": sample_pollutant.id,
        }, headers=auth_headers)
        assert response.status_code == 404


class TestGetReadings:
    """Test suite for GET /api/readings/ endpoints."""

    def test_get_readings_empty(self, client, auth_headers):
        """Test retrieving readings returns empty list when none exist."""
        response = client.get("/api/readings/", headers=auth_headers)
        assert response.status_code == 200
        assert response.json() == []

    def test_get_readings_with_filter(
        self, client, auth_headers, sample_zone, sample_pollutant
    ):
        """Test retrieving readings with zone filter."""
        # Create a reading first
        client.post("/api/readings/", json={
            "value": 15.0,
            "zone_id": sample_zone.id,
            "pollutant_type_id": sample_pollutant.id,
        }, headers=auth_headers)

        # Filter by zone
        response = client.get(
            f"/api/readings/?zone_id={sample_zone.id}",
            headers=auth_headers,
        )
        assert response.status_code == 200
        data = response.json()
        assert len(data) >= 1
        assert data[0]["zone_id"] == sample_zone.id


class TestPollutantTypes:
    """Test suite for pollutant type CRUD endpoints."""

    def test_create_pollutant_type(self, client, auth_headers):
        """Test creating a pollutant type with valid thresholds."""
        response = client.post("/api/pollutants/", json={
            "name": "NO2",
            "description": "Nitrogen Dioxide",
            "unit": "ppb",
            "safe_threshold": 53.0,
            "warning_threshold": 100.0,
            "danger_threshold": 360.0,
        }, headers=auth_headers)
        assert response.status_code == 201
        assert response.json()["name"] == "NO2"

    def test_create_pollutant_invalid_thresholds(self, client, auth_headers):
        """Test pollutant creation fails when safe >= warning threshold."""
        response = client.post("/api/pollutants/", json={
            "name": "BAD",
            "unit": "ppm",
            "safe_threshold": 100.0,
            "warning_threshold": 50.0,
            "danger_threshold": 200.0,
        }, headers=auth_headers)
        assert response.status_code == 422

    def test_get_all_pollutants(self, client, auth_headers, sample_pollutant):
        """Test retrieving all pollutant types."""
        response = client.get("/api/pollutants/", headers=auth_headers)
        assert response.status_code == 200
        assert len(response.json()) >= 1


class TestHealthCheck:
    """Test suite for the health check endpoint."""

    def test_health_check(self, client):
        """Test health endpoint returns healthy status."""
        response = client.get("/api/health")
        assert response.status_code == 200
        assert response.json()["status"] == "healthy"
