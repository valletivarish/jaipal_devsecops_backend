package com.airquality.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_readings")
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "\"value\"", nullable = false)
    private Double value;

    @Column
    private Integer aqi;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private MonitoringZone zone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pollutant_type_id", nullable = false)
    private PollutantType pollutantType;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public SensorReading() {
    }

    public SensorReading(Double value, Integer aqi, MonitoringZone zone, PollutantType pollutantType) {
        this.value = value;
        this.aqi = aqi;
        this.zone = zone;
        this.pollutantType = pollutantType;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Integer getAqi() {
        return aqi;
    }

    public void setAqi(Integer aqi) {
        this.aqi = aqi;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public MonitoringZone getZone() {
        return zone;
    }

    public void setZone(MonitoringZone zone) {
        this.zone = zone;
    }

    public PollutantType getPollutantType() {
        return pollutantType;
    }

    public void setPollutantType(PollutantType pollutantType) {
        this.pollutantType = pollutantType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
