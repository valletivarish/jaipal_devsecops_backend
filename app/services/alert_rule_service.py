# AlertRule Service
# Handles CRUD operations for alert rules that define notification triggers.
# When sensor readings exceed configured thresholds, alert rules identify
# which zones and pollutants have breached acceptable levels.
# Rule ownership is enforced - only the creator can modify or delete rules.

from sqlalchemy.orm import Session

from app.models.alert_rule import AlertRule
from app.models.monitoring_zone import MonitoringZone
from app.models.pollutant_type import PollutantType
from app.schemas.alert_rule import (
    AlertRuleCreate,
    AlertRuleUpdate,
    AlertRuleResponse,
)
from app.exceptions.handlers import ResourceNotFoundException


def create_alert_rule(
    db: Session, data: AlertRuleCreate, owner_id: int
) -> AlertRuleResponse:
    """
    Create a new alert rule linking a monitoring zone to a pollutant threshold.
    Validates that the referenced zone and pollutant type exist before creation.
    """
    # Verify the referenced monitoring zone exists
    zone = db.query(MonitoringZone).filter(
        MonitoringZone.id == data.zone_id
    ).first()
    if not zone:
        raise ResourceNotFoundException("MonitoringZone", data.zone_id)

    # Verify the referenced pollutant type exists
    pollutant = db.query(PollutantType).filter(
        PollutantType.id == data.pollutant_type_id
    ).first()
    if not pollutant:
        raise ResourceNotFoundException("PollutantType", data.pollutant_type_id)

    new_rule = AlertRule(
        name=data.name,
        threshold_value=data.threshold_value,
        condition=data.condition,
        severity=data.severity,
        is_active=data.is_active,
        zone_id=data.zone_id,
        pollutant_type_id=data.pollutant_type_id,
        owner_id=owner_id,
    )
    db.add(new_rule)
    db.commit()
    db.refresh(new_rule)
    return AlertRuleResponse.model_validate(new_rule)


def get_alert_rule_by_id(db: Session, rule_id: int) -> AlertRuleResponse:
    """Retrieve an alert rule by ID. Raises 404 if not found."""
    rule = db.query(AlertRule).filter(AlertRule.id == rule_id).first()
    if not rule:
        raise ResourceNotFoundException("AlertRule", rule_id)
    return AlertRuleResponse.model_validate(rule)


def get_all_alert_rules(
    db: Session, skip: int = 0, limit: int = 20
) -> list[AlertRuleResponse]:
    """Retrieve all alert rules with pagination."""
    rules = db.query(AlertRule).offset(skip).limit(limit).all()
    return [AlertRuleResponse.model_validate(rule) for rule in rules]


def get_alert_rules_by_zone(
    db: Session, zone_id: int
) -> list[AlertRuleResponse]:
    """Retrieve all alert rules for a specific monitoring zone."""
    rules = db.query(AlertRule).filter(AlertRule.zone_id == zone_id).all()
    return [AlertRuleResponse.model_validate(rule) for rule in rules]


def get_alert_rules_by_owner(
    db: Session, owner_id: int
) -> list[AlertRuleResponse]:
    """Retrieve all alert rules created by a specific user."""
    rules = db.query(AlertRule).filter(AlertRule.owner_id == owner_id).all()
    return [AlertRuleResponse.model_validate(rule) for rule in rules]


def update_alert_rule(
    db: Session, rule_id: int, data: AlertRuleUpdate, owner_id: int
) -> AlertRuleResponse:
    """Update an existing alert rule. Only the owner can modify rules."""
    rule = db.query(AlertRule).filter(AlertRule.id == rule_id).first()
    if not rule:
        raise ResourceNotFoundException("AlertRule", rule_id)

    # Enforce ownership
    if rule.owner_id != owner_id:
        from fastapi import HTTPException, status
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You do not have permission to update this alert rule",
        )

    update_data = data.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(rule, field, value)

    db.commit()
    db.refresh(rule)
    return AlertRuleResponse.model_validate(rule)


def delete_alert_rule(db: Session, rule_id: int, owner_id: int) -> bool:
    """Delete an alert rule. Only the owner can delete their rules."""
    rule = db.query(AlertRule).filter(AlertRule.id == rule_id).first()
    if not rule:
        raise ResourceNotFoundException("AlertRule", rule_id)

    if rule.owner_id != owner_id:
        from fastapi import HTTPException, status
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You do not have permission to delete this alert rule",
        )

    db.delete(rule)
    db.commit()
    return True
