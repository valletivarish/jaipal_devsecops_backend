# AlertRule Tests
# Tests for CRUD operations on alert rules.
# Validates severity levels, threshold values, and ownership enforcement.

import pytest


class TestCreateAlertRule:
    """Test suite for POST /api/alerts/ endpoint."""

    def test_create_alert_success(
        self, client, auth_headers, sample_zone, sample_pollutant
    ):
        """Test creating an alert rule with valid data."""
        response = client.post("/api/alerts/", json={
            "name": "High PM2.5 Alert",
            "threshold_value": 35.0,
            "condition": "ABOVE",
            "severity": "HIGH",
            "zone_id": sample_zone.id,
            "pollutant_type_id": sample_pollutant.id,
        }, headers=auth_headers)
        assert response.status_code == 201
        data = response.json()
        assert data["name"] == "High PM2.5 Alert"
        assert data["severity"] == "HIGH"

    def test_create_alert_invalid_severity(
        self, client, auth_headers, sample_zone, sample_pollutant
    ):
        """Test alert creation fails with invalid severity level."""
        response = client.post("/api/alerts/", json={
            "name": "Bad Alert",
            "threshold_value": 35.0,
            "condition": "ABOVE",
            "severity": "INVALID",
            "zone_id": sample_zone.id,
            "pollutant_type_id": sample_pollutant.id,
        }, headers=auth_headers)
        assert response.status_code == 422

    def test_create_alert_negative_threshold(
        self, client, auth_headers, sample_zone, sample_pollutant
    ):
        """Test alert creation fails with negative threshold value."""
        response = client.post("/api/alerts/", json={
            "name": "Bad Alert",
            "threshold_value": -5.0,
            "condition": "ABOVE",
            "severity": "HIGH",
            "zone_id": sample_zone.id,
            "pollutant_type_id": sample_pollutant.id,
        }, headers=auth_headers)
        assert response.status_code == 422


class TestGetAlertRules:
    """Test suite for GET /api/alerts/ endpoints."""

    def test_get_all_alerts(self, client, auth_headers):
        """Test retrieving all alert rules."""
        response = client.get("/api/alerts/", headers=auth_headers)
        assert response.status_code == 200

    def test_get_alerts_unauthenticated(self, client):
        """Test retrieving alerts fails without authentication."""
        response = client.get("/api/alerts/")
        assert response.status_code == 401
