# Global Exception Handlers
# Provides structured JSON error responses for all exception types.
# Ensures consistent error format across all API endpoints:
# { "detail": "error message", "status_code": 400, "errors": [...] }
# This is the FastAPI equivalent of Spring Boot's @RestControllerAdvice.

from fastapi import FastAPI, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse


class ResourceNotFoundException(Exception):
    """Raised when a requested resource is not found in the database."""
    def __init__(self, resource: str, resource_id: int):
        self.resource = resource
        self.resource_id = resource_id
        self.message = f"{resource} with id {resource_id} not found"
        super().__init__(self.message)


class DuplicateResourceException(Exception):
    """Raised when attempting to create a resource that already exists."""
    def __init__(self, resource: str, field: str, value: str):
        self.resource = resource
        self.field = field
        self.value = value
        self.message = f"{resource} with {field} '{value}' already exists"
        super().__init__(self.message)


def register_exception_handlers(app: FastAPI):
    """
    Register all custom exception handlers with the FastAPI application.
    Called during application startup to ensure all errors return
    consistent JSON responses instead of default HTML error pages.
    """

    @app.exception_handler(ResourceNotFoundException)
    async def resource_not_found_handler(
        request: Request, exc: ResourceNotFoundException
    ):
        """Handle 404 Not Found errors for missing resources."""
        return JSONResponse(
            status_code=status.HTTP_404_NOT_FOUND,
            content={
                "detail": exc.message,
                "status_code": 404,
                "errors": [],
            },
        )

    @app.exception_handler(DuplicateResourceException)
    async def duplicate_resource_handler(
        request: Request, exc: DuplicateResourceException
    ):
        """Handle 409 Conflict errors for duplicate resources."""
        return JSONResponse(
            status_code=status.HTTP_409_CONFLICT,
            content={
                "detail": exc.message,
                "status_code": 409,
                "errors": [],
            },
        )

    @app.exception_handler(RequestValidationError)
    async def validation_exception_handler(
        request: Request, exc: RequestValidationError
    ):
        """
        Handle Pydantic validation errors with detailed field-level messages.
        Formats each validation error with the field name and error message
        so the frontend can display specific feedback per form field.
        """
        errors = []
        for error in exc.errors():
            # Extract the field path (e.g., "body -> latitude")
            field = " -> ".join(str(loc) for loc in error["loc"])
            errors.append({
                "field": field,
                "message": error["msg"],
                "type": error["type"],
            })
        return JSONResponse(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            content={
                "detail": "Validation error",
                "status_code": 422,
                "errors": errors,
            },
        )

    @app.exception_handler(Exception)
    async def general_exception_handler(request: Request, exc: Exception):
        """
        Catch-all handler for unexpected server errors.
        Logs the error and returns a generic 500 response
        to avoid leaking internal details to the client.
        """
        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content={
                "detail": "An unexpected error occurred",
                "status_code": 500,
                "errors": [],
            },
        )
