# Forecast Service - ML/Analytics Feature
# Implements air quality trend prediction using scikit-learn's LinearRegression.
# Analyzes historical sensor readings for a given zone and pollutant type
# to forecast pollutant levels for the next 7 days.
# Returns predicted values with confidence intervals and trend direction.

from datetime import datetime, timedelta, timezone

import numpy as np
from sklearn.linear_model import LinearRegression
from sklearn.metrics import r2_score
from sqlalchemy.orm import Session

from app.models.sensor_reading import SensorReading
from app.models.monitoring_zone import MonitoringZone
from app.models.pollutant_type import PollutantType
from app.schemas.dashboard import ForecastResponse, ForecastDataPoint
from app.exceptions.handlers import ResourceNotFoundException


def generate_forecast(
    db: Session,
    zone_id: int,
    pollutant_type_id: int,
    history_days: int = 30,
    forecast_days: int = 7,
) -> ForecastResponse:
    """
    Generate air quality forecast for a zone and pollutant type.
    Uses linear regression on historical sensor data to predict future levels.

    Algorithm:
    1. Fetch historical readings for the specified period
    2. Convert timestamps to numeric features (days from start)
    3. Fit a LinearRegression model on the historical data
    4. Predict values for the next N days
    5. Calculate confidence intervals using residual standard error
    6. Determine trend direction from the regression slope
    """
    # Validate zone and pollutant exist
    zone = db.query(MonitoringZone).filter(MonitoringZone.id == zone_id).first()
    if not zone:
        raise ResourceNotFoundException("MonitoringZone", zone_id)

    pollutant = db.query(PollutantType).filter(
        PollutantType.id == pollutant_type_id
    ).first()
    if not pollutant:
        raise ResourceNotFoundException("PollutantType", pollutant_type_id)

    # Fetch historical readings ordered by time
    start_date = datetime.now(timezone.utc) - timedelta(days=history_days)
    readings = (
        db.query(SensorReading)
        .filter(
            SensorReading.zone_id == zone_id,
            SensorReading.pollutant_type_id == pollutant_type_id,
            SensorReading.recorded_at >= start_date,
        )
        .order_by(SensorReading.recorded_at.asc())
        .all()
    )

    # Need at least 5 data points for meaningful regression
    if len(readings) < 5:
        return ForecastResponse(
            zone_id=zone_id,
            zone_name=zone.name,
            pollutant_name=pollutant.name,
            pollutant_unit=pollutant.unit,
            trend_direction="INSUFFICIENT_DATA",
            confidence_score=0.0,
            forecast_data=[],
        )

    # Prepare training data - X is days since first reading, y is pollutant value
    base_time = readings[0].recorded_at
    x_values = np.array([
        (r.recorded_at - base_time).total_seconds() / 86400.0
        for r in readings
    ]).reshape(-1, 1)
    y_values = np.array([r.value for r in readings])

    # Fit linear regression model
    model = LinearRegression()
    model.fit(x_values, y_values)

    # Calculate R-squared score (model confidence)
    y_predicted = model.predict(x_values)
    confidence = max(0.0, r2_score(y_values, y_predicted))

    # Calculate residual standard error for confidence intervals
    residuals = y_values - y_predicted
    residual_std = np.std(residuals) if len(residuals) > 2 else 0.0

    # Determine trend direction from the regression slope
    slope = model.coef_[0]
    if slope > 0.1:
        trend_direction = "INCREASING"
    elif slope < -0.1:
        trend_direction = "DECREASING"
    else:
        trend_direction = "STABLE"

    # Generate forecast for the next N days
    last_day = x_values[-1][0]
    forecast_data = []
    for i in range(1, forecast_days + 1):
        future_day = last_day + i
        predicted_value = model.predict(np.array([[future_day]]))[0]

        # Ensure predicted value is non-negative (pollutant levels cannot be negative)
        predicted_value = max(0.0, predicted_value)

        # 95% confidence interval using 1.96 standard deviations
        margin = 1.96 * residual_std
        lower = max(0.0, predicted_value - margin)
        upper = predicted_value + margin

        # Calculate the date for this forecast point
        forecast_date = datetime.now(timezone.utc) + timedelta(days=i)

        forecast_data.append(
            ForecastDataPoint(
                date=forecast_date.strftime("%Y-%m-%d"),
                predicted_value=round(predicted_value, 2),
                lower_bound=round(lower, 2),
                upper_bound=round(upper, 2),
            )
        )

    return ForecastResponse(
        zone_id=zone_id,
        zone_name=zone.name,
        pollutant_name=pollutant.name,
        pollutant_unit=pollutant.unit,
        trend_direction=trend_direction,
        confidence_score=round(confidence, 4),
        forecast_data=forecast_data,
    )
