# Authentication Tests
# Tests for user registration, login, and profile retrieval endpoints.
# Validates input validation, error handling, and JWT token generation.

import pytest


class TestRegistration:
    """Test suite for the POST /api/auth/register endpoint."""

    def test_register_success(self, client):
        """Test successful user registration with valid data."""
        response = client.post("/api/auth/register", json={
            "email": "new@example.com",
            "username": "newuser",
            "password": "ValidPass123",
            "full_name": "New User",
        })
        assert response.status_code == 201
        data = response.json()
        assert "access_token" in data
        assert data["user"]["email"] == "new@example.com"

    def test_register_duplicate_email(self, client, test_user):
        """Test registration fails with duplicate email."""
        response = client.post("/api/auth/register", json={
            "email": "test@example.com",
            "username": "different",
            "password": "ValidPass123",
            "full_name": "Another User",
        })
        assert response.status_code == 409

    def test_register_invalid_email(self, client):
        """Test registration fails with invalid email format."""
        response = client.post("/api/auth/register", json={
            "email": "not-an-email",
            "username": "user1",
            "password": "ValidPass123",
            "full_name": "Test",
        })
        assert response.status_code == 422

    def test_register_weak_password(self, client):
        """Test registration fails with password lacking uppercase."""
        response = client.post("/api/auth/register", json={
            "email": "test2@example.com",
            "username": "user2",
            "password": "weakpass1",
            "full_name": "Test",
        })
        assert response.status_code == 422

    def test_register_short_password(self, client):
        """Test registration fails with password shorter than 8 characters."""
        response = client.post("/api/auth/register", json={
            "email": "test3@example.com",
            "username": "user3",
            "password": "Ab1",
            "full_name": "Test",
        })
        assert response.status_code == 422


class TestLogin:
    """Test suite for the POST /api/auth/login endpoint."""

    def test_login_success(self, client, test_user):
        """Test successful login with correct credentials."""
        response = client.post("/api/auth/login", json={
            "email": "test@example.com",
            "password": "TestPass123",
        })
        assert response.status_code == 200
        data = response.json()
        assert "access_token" in data
        assert data["token_type"] == "bearer"

    def test_login_wrong_password(self, client, test_user):
        """Test login fails with incorrect password."""
        response = client.post("/api/auth/login", json={
            "email": "test@example.com",
            "password": "WrongPass123",
        })
        assert response.status_code == 401

    def test_login_nonexistent_email(self, client):
        """Test login fails with unregistered email."""
        response = client.post("/api/auth/login", json={
            "email": "nobody@example.com",
            "password": "SomePass123",
        })
        assert response.status_code == 401


class TestCurrentUser:
    """Test suite for the GET /api/auth/me endpoint."""

    def test_get_current_user(self, client, auth_headers):
        """Test retrieving the current user profile with valid token."""
        response = client.get("/api/auth/me", headers=auth_headers)
        assert response.status_code == 200
        assert response.json()["email"] == "test@example.com"

    def test_get_current_user_no_token(self, client):
        """Test profile retrieval fails without authentication."""
        response = client.get("/api/auth/me")
        assert response.status_code == 401
