# AlertRules Router
# Handles CRUD endpoints for alert rules that define notification triggers.
# Alert rules link monitoring zones to pollutant thresholds.
# When readings exceed thresholds, alerts are generated.
# All endpoints require JWT authentication.

from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.schemas.alert_rule import (
    AlertRuleCreate,
    AlertRuleUpdate,
    AlertRuleResponse,
)
from app.services import alert_rule_service
from app.middleware.jwt_middleware import get_current_user
from app.models.user import User

router = APIRouter(prefix="/api/alerts", tags=["Alert Rules"])


@router.post(
    "/",
    response_model=AlertRuleResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Create a new alert rule",
)
def create_alert(
    data: AlertRuleCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    Create an alert rule with a pollutant threshold for a monitoring zone.
    Severity must be LOW, MEDIUM, HIGH, or CRITICAL.
    Threshold value must be a positive decimal.
    """
    return alert_rule_service.create_alert_rule(db, data, current_user.id)


@router.get(
    "/",
    response_model=list[AlertRuleResponse],
    summary="Get all alert rules",
)
def get_all_alerts(
    skip: int = Query(0, ge=0),
    limit: int = Query(20, ge=1, le=100),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Retrieve all alert rules with pagination."""
    return alert_rule_service.get_all_alert_rules(db, skip=skip, limit=limit)


@router.get(
    "/my",
    response_model=list[AlertRuleResponse],
    summary="Get alert rules owned by current user",
)
def get_my_alerts(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Retrieve all alert rules created by the authenticated user."""
    return alert_rule_service.get_alert_rules_by_owner(db, current_user.id)


@router.get(
    "/zone/{zone_id}",
    response_model=list[AlertRuleResponse],
    summary="Get alert rules for a specific zone",
)
def get_alerts_by_zone(
    zone_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Retrieve all alert rules configured for a specific monitoring zone."""
    return alert_rule_service.get_alert_rules_by_zone(db, zone_id)


@router.get(
    "/{rule_id}",
    response_model=AlertRuleResponse,
    summary="Get alert rule by ID",
)
def get_alert(
    rule_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Retrieve a specific alert rule by its ID."""
    return alert_rule_service.get_alert_rule_by_id(db, rule_id)


@router.put(
    "/{rule_id}",
    response_model=AlertRuleResponse,
    summary="Update an alert rule",
)
def update_alert(
    rule_id: int,
    data: AlertRuleUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Update an alert rule. Only the owner can modify their rules."""
    return alert_rule_service.update_alert_rule(db, rule_id, data, current_user.id)


@router.delete(
    "/{rule_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Delete an alert rule",
)
def delete_alert(
    rule_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Delete an alert rule. Only the owner can delete their rules."""
    alert_rule_service.delete_alert_rule(db, rule_id, current_user.id)
