package com.airquality.config;

import com.airquality.model.*;
import com.airquality.repository.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Component
@Profile("!test")
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PollutantTypeRepository pollutantTypeRepository;
    private final MonitoringZoneRepository monitoringZoneRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      PollutantTypeRepository pollutantTypeRepository,
                      MonitoringZoneRepository monitoringZoneRepository,
                      SensorReadingRepository sensorReadingRepository,
                      AlertRuleRepository alertRuleRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.pollutantTypeRepository = pollutantTypeRepository;
        this.monitoringZoneRepository = monitoringZoneRepository;
        this.sensorReadingRepository = sensorReadingRepository;
        this.alertRuleRepository = alertRuleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        User demoUser = createDemoUserIfNotExists("qa@test.com", "qauser", "QA Tester", "SecurePass123");
        createDemoUserIfNotExists("user2@test.com", "demouser2", "Demo User 2", "SecurePass123");

        if (pollutantTypeRepository.count() == 0) {
            seedPollutantTypes();
        }

        if (monitoringZoneRepository.count() == 0 && demoUser != null) {
            seedMonitoringZones(demoUser);
        }

        if (sensorReadingRepository.count() == 0) {
            seedSensorReadings();
        }

        if (alertRuleRepository.count() == 0 && demoUser != null) {
            seedAlertRules(demoUser);
        }
    }

    private User createDemoUserIfNotExists(String email, String username, String fullName, String password) {
        if (!userRepository.existsByEmail(email)) {
            User user = new User();
            user.setEmail(email);
            user.setUsername(username);
            user.setFullName(fullName);
            user.setHashedPassword(passwordEncoder.encode(password));
            user.setIsActive(true);
            return userRepository.save(user);
        }
        return userRepository.findByEmail(email).orElse(null);
    }

    private void seedPollutantTypes() {
        PollutantType pm25 = new PollutantType("PM2.5", "µg/m³", 12.0, 35.4, 55.4);
        pm25.setDescription("Fine particulate matter smaller than 2.5 micrometers");
        pollutantTypeRepository.save(pm25);

        PollutantType pm10 = new PollutantType("PM10", "µg/m³", 54.0, 154.0, 254.0);
        pm10.setDescription("Particulate matter smaller than 10 micrometers");
        pollutantTypeRepository.save(pm10);

        PollutantType o3 = new PollutantType("O3", "ppb", 54.0, 70.0, 85.0);
        o3.setDescription("Ground-level ozone");
        pollutantTypeRepository.save(o3);

        PollutantType no2 = new PollutantType("NO2", "ppb", 53.0, 100.0, 360.0);
        no2.setDescription("Nitrogen dioxide from vehicle emissions and power plants");
        pollutantTypeRepository.save(no2);

        PollutantType co = new PollutantType("CO", "ppm", 4.4, 9.4, 12.4);
        co.setDescription("Carbon monoxide from incomplete combustion");
        pollutantTypeRepository.save(co);

        PollutantType so2 = new PollutantType("SO2", "ppb", 35.0, 75.0, 185.0);
        so2.setDescription("Sulfur dioxide from industrial processes");
        pollutantTypeRepository.save(so2);
    }

    private void seedMonitoringZones(User owner) {
        MonitoringZone zone1 = new MonitoringZone("Dublin City Centre", 53.3498, -6.2603, 2.0, owner);
        zone1.setDescription("Central Dublin monitoring zone covering O'Connell Street and surrounding areas");
        monitoringZoneRepository.save(zone1);

        MonitoringZone zone2 = new MonitoringZone("Dublin Port Area", 53.3478, -6.2097, 1.5, owner);
        zone2.setDescription("Dublin Port and docklands industrial monitoring zone");
        monitoringZoneRepository.save(zone2);

        MonitoringZone zone3 = new MonitoringZone("Phoenix Park", 53.3559, -6.3298, 2.5, owner);
        zone3.setDescription("Phoenix Park and surrounding residential areas");
        monitoringZoneRepository.save(zone3);

        MonitoringZone zone4 = new MonitoringZone("Dublin Airport", 53.4264, -6.2499, 3.0, owner);
        zone4.setDescription("Dublin Airport and surrounding Swords area");
        monitoringZoneRepository.save(zone4);

        MonitoringZone zone5 = new MonitoringZone("Sandymount Coastal", 53.3310, -6.2186, 1.5, owner);
        zone5.setDescription("Sandymount strand and coastal monitoring area");
        monitoringZoneRepository.save(zone5);
    }

    private void seedSensorReadings() {
        List<MonitoringZone> zones = monitoringZoneRepository.findAll();
        List<PollutantType> pollutants = pollutantTypeRepository.findAll();

        if (zones.isEmpty() || pollutants.isEmpty()) {
            return;
        }

        Random random = new Random(42);
        LocalDateTime now = LocalDateTime.now();

        for (MonitoringZone zone : zones) {
            for (PollutantType pollutant : pollutants) {
                for (int day = 30; day >= 0; day--) {
                    int[] hours = {6, 10, 14, 20};
                    for (int hour : hours) {
                        LocalDateTime recordedAt = now.minusDays(day).withHour(hour).withMinute(0).withSecond(0);

                        double baseValue = getBaseValue(pollutant.getName(), zone.getName());
                        double variation = baseValue * 0.3 * random.nextGaussian();
                        if ((hour == 10 || hour == 20)
                                && (zone.getName().contains("City") || zone.getName().contains("Port"))) {
                            variation += baseValue * 0.2;
                        }
                        double value = Math.max(0.1, baseValue + variation);
                        value = Math.round(value * 100.0) / 100.0;

                        int aqi = calculateAqi(value, pollutant);

                        SensorReading reading = new SensorReading();
                        reading.setValue(value);
                        reading.setAqi(aqi);
                        reading.setRecordedAt(recordedAt);
                        reading.setZone(zone);
                        reading.setPollutantType(pollutant);
                        sensorReadingRepository.save(reading);
                    }
                }
            }
        }
    }

    private double getBaseValue(String pollutantName, String zoneName) {
        boolean isUrban = zoneName.contains("City") || zoneName.contains("Port") || zoneName.contains("Airport");
        switch (pollutantName) {
            case "PM2.5": return isUrban ? 18.0 : 8.0;
            case "PM10": return isUrban ? 45.0 : 25.0;
            case "O3": return isUrban ? 40.0 : 50.0;
            case "NO2": return isUrban ? 55.0 : 20.0;
            case "CO": return isUrban ? 3.0 : 1.5;
            case "SO2": return isUrban ? 25.0 : 10.0;
            default: return 10.0;
        }
    }

    private int calculateAqi(double value, PollutantType pollutant) {
        double safe = pollutant.getSafeThreshold();
        double warning = pollutant.getWarningThreshold();
        double danger = pollutant.getDangerThreshold();

        if (value <= safe) {
            return (int) (safe > 0 ? (value / safe) * 50 : 0);
        } else if (value <= warning) {
            double ratio = (warning - safe) > 0 ? (value - safe) / (warning - safe) : 0;
            return (int) (51 + ratio * 49);
        } else if (value <= danger) {
            double ratio = (danger - warning) > 0 ? (value - warning) / (danger - warning) : 0;
            return (int) (101 + ratio * 99);
        } else {
            double ratio = Math.min(danger > 0 ? (value - danger) / danger : 1.0, 1.0);
            return (int) (201 + ratio * 299);
        }
    }

    private void seedAlertRules(User owner) {
        List<MonitoringZone> zones = monitoringZoneRepository.findAll();
        List<PollutantType> pollutants = pollutantTypeRepository.findAll();

        if (zones.isEmpty() || pollutants.isEmpty()) {
            return;
        }

        MonitoringZone cityZone = zones.get(0);
        PollutantType pm25 = pollutants.stream().filter(p -> p.getName().equals("PM2.5")).findFirst().orElse(null);
        PollutantType no2 = pollutants.stream().filter(p -> p.getName().equals("NO2")).findFirst().orElse(null);
        PollutantType o3 = pollutants.stream().filter(p -> p.getName().equals("O3")).findFirst().orElse(null);

        if (pm25 != null) {
            AlertRule rule1 = new AlertRule("PM2.5 High Alert - City Centre", 35.0, "HIGH", cityZone, pm25, owner);
            rule1.setCondition("ABOVE");
            alertRuleRepository.save(rule1);
        }

        if (no2 != null) {
            AlertRule rule2 = new AlertRule("NO2 Warning - City Centre", 100.0, "MEDIUM", cityZone, no2, owner);
            rule2.setCondition("ABOVE");
            alertRuleRepository.save(rule2);
        }

        MonitoringZone parkZone = zones.stream().filter(z -> z.getName().contains("Phoenix")).findFirst().orElse(null);
        if (o3 != null && parkZone != null) {
            AlertRule rule3 = new AlertRule("Ozone Warning - Phoenix Park", 70.0, "MEDIUM", parkZone, o3, owner);
            rule3.setCondition("ABOVE");
            alertRuleRepository.save(rule3);
        }

        MonitoringZone airportZone = zones.stream().filter(z -> z.getName().contains("Airport")).findFirst().orElse(null);
        if (pm25 != null && airportZone != null) {
            AlertRule rule4 = new AlertRule("PM2.5 Critical - Airport Zone", 55.0, "CRITICAL", airportZone, pm25, owner);
            rule4.setCondition("ABOVE");
            alertRuleRepository.save(rule4);
        }
    }
}
