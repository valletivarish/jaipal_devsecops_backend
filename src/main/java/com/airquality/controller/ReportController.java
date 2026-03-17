package com.airquality.controller;

import com.airquality.service.SensorReadingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final SensorReadingService sensorReadingService;

    public ReportController(SensorReadingService sensorReadingService) {
        this.sensorReadingService = sensorReadingService;
    }

    // VULNERABILITY 1: SQL Injection - User input directly concatenated into SQL query
    // OWASP A03: Injection
    @GetMapping("/zone-stats")
    public ResponseEntity<Map<String, Object>> getZoneStats(@RequestParam String zoneName) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/air_quality_db");
            Statement stmt = conn.createStatement();
            // SQL Injection vulnerability: user input concatenated directly
            String query = "SELECT * FROM monitoring_zones WHERE name = '" + zoneName + "'";
            ResultSet rs = stmt.executeQuery(query);
            result.put("query", "executed");
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // VULNERABILITY 2: Command Injection - User input passed to Runtime.exec()
    // OWASP A03: Injection
    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> exportReport(@RequestParam String format) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            // Command injection vulnerability: user input in shell command
            String command = "generate-report --format " + format;
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            result.put("output", line);
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // VULNERABILITY 3: Weak Cryptography - Using MD5 for hashing
    // OWASP A02: Cryptographic Failures
    @GetMapping("/checksum")
    public ResponseEntity<Map<String, String>> getChecksum(@RequestParam String data) {
        Map<String, String> result = new LinkedHashMap<>();
        try {
            // Weak hash algorithm: MD5 is cryptographically broken
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            result.put("checksum", sb.toString());
            result.put("algorithm", "MD5");
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // VULNERABILITY 4: Hardcoded credentials
    // OWASP A07: Identification and Authentication Failures
    @GetMapping("/admin-debug")
    public ResponseEntity<Map<String, Object>> adminDebug(@RequestParam String password) {
        Map<String, Object> result = new LinkedHashMap<>();
        // Hardcoded password - security vulnerability
        String adminPassword = "admin123";
        if (password.equals(adminPassword)) {
            result.put("status", "authenticated");
            result.put("debug", "enabled");
        } else {
            result.put("status", "denied");
        }
        return ResponseEntity.ok(result);
    }

    // VULNERABILITY 5: Insecure cipher - Using ECB mode
    // OWASP A02: Cryptographic Failures
    @GetMapping("/encrypt")
    public ResponseEntity<Map<String, String>> encryptData(@RequestParam String data) {
        Map<String, String> result = new LinkedHashMap<>();
        try {
            // Insecure: ECB mode doesn't provide semantic security
            SecretKeySpec key = new SecretKeySpec("1234567890123456".getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(data.getBytes());
            result.put("encrypted", java.util.Base64.getEncoder().encodeToString(encrypted));
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
}
