# MonitoringZone Tests
# Tests for CRUD operations on monitoring zones.
# Validates GPS coordinate ranges, zone creation, retrieval, update, and deletion.

import pytest


class TestCreateZone:
    """Test suite for POST /api/zones/ endpoint."""

    def test_create_zone_success(self, client, auth_headers):
        """Test creating a monitoring zone with valid GPS coordinates."""
        response = client.post("/api/zones/", json={
            "name": "Test Zone",
            "description": "A test monitoring zone",
            "latitude": 53.3498,
            "longitude": -6.2603,
            "radius": 5000.0,
        }, headers=auth_headers)
        assert response.status_code == 201
        data = response.json()
        assert data["name"] == "Test Zone"
        assert data["latitude"] == 53.3498

    def test_create_zone_invalid_latitude(self, client, auth_headers):
        """Test zone creation fails with latitude outside -90 to 90 range."""
        response = client.post("/api/zones/", json={
            "name": "Invalid Zone",
            "latitude": 91.0,
            "longitude": 0.0,
            "radius": 1000.0,
        }, headers=auth_headers)
        assert response.status_code == 422

    def test_create_zone_invalid_longitude(self, client, auth_headers):
        """Test zone creation fails with longitude outside -180 to 180 range."""
        response = client.post("/api/zones/", json={
            "name": "Invalid Zone",
            "latitude": 0.0,
            "longitude": 181.0,
            "radius": 1000.0,
        }, headers=auth_headers)
        assert response.status_code == 422

    def test_create_zone_negative_radius(self, client, auth_headers):
        """Test zone creation fails with negative radius."""
        response = client.post("/api/zones/", json={
            "name": "Invalid Zone",
            "latitude": 0.0,
            "longitude": 0.0,
            "radius": -100.0,
        }, headers=auth_headers)
        assert response.status_code == 422

    def test_create_zone_unauthenticated(self, client):
        """Test zone creation fails without authentication."""
        response = client.post("/api/zones/", json={
            "name": "No Auth Zone",
            "latitude": 0.0,
            "longitude": 0.0,
            "radius": 1000.0,
        })
        assert response.status_code == 401


class TestGetZones:
    """Test suite for GET /api/zones/ endpoints."""

    def test_get_all_zones(self, client, auth_headers, sample_zone):
        """Test retrieving all monitoring zones."""
        response = client.get("/api/zones/", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()
        assert len(data) >= 1

    def test_get_zone_by_id(self, client, auth_headers, sample_zone):
        """Test retrieving a specific zone by ID."""
        response = client.get(
            f"/api/zones/{sample_zone.id}", headers=auth_headers
        )
        assert response.status_code == 200
        assert response.json()["name"] == "Dublin City Centre"

    def test_get_zone_not_found(self, client, auth_headers):
        """Test retrieving a non-existent zone returns 404."""
        response = client.get("/api/zones/9999", headers=auth_headers)
        assert response.status_code == 404


class TestUpdateZone:
    """Test suite for PUT /api/zones/{id} endpoint."""

    def test_update_zone_success(self, client, auth_headers, sample_zone):
        """Test updating a zone's name and radius."""
        response = client.put(
            f"/api/zones/{sample_zone.id}",
            json={"name": "Updated Zone", "radius": 8000.0},
            headers=auth_headers,
        )
        assert response.status_code == 200
        assert response.json()["name"] == "Updated Zone"
        assert response.json()["radius"] == 8000.0


class TestDeleteZone:
    """Test suite for DELETE /api/zones/{id} endpoint."""

    def test_delete_zone_success(self, client, auth_headers, sample_zone):
        """Test deleting a monitoring zone owned by the user."""
        response = client.delete(
            f"/api/zones/{sample_zone.id}", headers=auth_headers
        )
        assert response.status_code == 204
