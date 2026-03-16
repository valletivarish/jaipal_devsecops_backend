package com.airquality.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pollutant_types")
public class PollutantType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, unique = true, nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 20, nullable = false)
    private String unit;

    @Column(name = "safe_threshold", nullable = false)
    private Double safeThreshold;

    @Column(name = "warning_threshold", nullable = false)
    private Double warningThreshold;

    @Column(name = "danger_threshold", nullable = false)
    private Double dangerThreshold;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "pollutantType", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<SensorReading> sensorReadings = new ArrayList<>();

    @OneToMany(mappedBy = "pollutantType", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<AlertRule> alertRules = new ArrayList<>();

    public PollutantType() {
    }

    public PollutantType(String name, String unit, Double safeThreshold, Double warningThreshold, Double dangerThreshold) {
        this.name = name;
        this.unit = unit;
        this.safeThreshold = safeThreshold;
        this.warningThreshold = warningThreshold;
        this.dangerThreshold = dangerThreshold;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Double getSafeThreshold() {
        return safeThreshold;
    }

    public void setSafeThreshold(Double safeThreshold) {
        this.safeThreshold = safeThreshold;
    }

    public Double getWarningThreshold() {
        return warningThreshold;
    }

    public void setWarningThreshold(Double warningThreshold) {
        this.warningThreshold = warningThreshold;
    }

    public Double getDangerThreshold() {
        return dangerThreshold;
    }

    public void setDangerThreshold(Double dangerThreshold) {
        this.dangerThreshold = dangerThreshold;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<SensorReading> getSensorReadings() {
        return sensorReadings;
    }

    public void setSensorReadings(List<SensorReading> sensorReadings) {
        this.sensorReadings = sensorReadings;
    }

    public List<AlertRule> getAlertRules() {
        return alertRules;
    }

    public void setAlertRules(List<AlertRule> alertRules) {
        this.alertRules = alertRules;
    }
}
