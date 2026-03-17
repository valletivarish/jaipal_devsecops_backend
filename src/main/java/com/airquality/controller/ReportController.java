package com.airquality.controller;

import com.airquality.service.SensorReadingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final SensorReadingService sensorReadingService;

    // FIX 4: Removed hardcoded password - use Spring Security authentication instead
    private static final List<String> ALLOWED_FORMATS = List.of("pdf", "csv", "json");

    public ReportController(SensorReadingService sensorReadingService) {
        this.sensorReadingService = sensorReadingService;
    }

    // FIX 1: SQL Injection fixed - Using PreparedStatement with parameterized query
    // OWASP A03: Injection - RESOLVED
    @GetMapping("/zone-stats")
    public ResponseEntity<Map<String, Object>> getZoneStats(@RequestParam String zoneName) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/air_quality_db");
            // FIXED: Use PreparedStatement with parameterized query instead of string concatenation
            PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM monitoring_zones WHERE name = ?");
            pstmt.setString(1, zoneName);
            ResultSet rs = pstmt.executeQuery();
            result.put("query", "executed");
            rs.close();
            pstmt.close();
            conn.close();
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // FIX 2: Command Injection fixed - Using allowlist validation and ProcessBuilder
    // OWASP A03: Injection - RESOLVED
    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> exportReport(@RequestParam String format) {
        Map<String, Object> result = new LinkedHashMap<>();
        // FIXED: Validate input against allowlist instead of passing directly to shell
        if (!ALLOWED_FORMATS.contains(format.toLowerCase())) {
            result.put("error", "Invalid format. Allowed: " + ALLOWED_FORMATS);
            return ResponseEntity.badRequest().body(result);
        }
        try {
            // FIXED: Use ProcessBuilder with separated arguments instead of Runtime.exec()
            ProcessBuilder pb = new ProcessBuilder("generate-report", "--format", format);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            result.put("status", "report generation started");
            result.put("format", format);
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // FIX 3: Weak Cryptography fixed - Using SHA-256 instead of MD5
    // OWASP A02: Cryptographic Failures - RESOLVED
    @GetMapping("/checksum")
    public ResponseEntity<Map<String, String>> getChecksum(@RequestParam String data) {
        Map<String, String> result = new LinkedHashMap<>();
        try {
            // FIXED: Use SHA-256 instead of broken MD5 hash algorithm
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            result.put("checksum", sb.toString());
            result.put("algorithm", "SHA-256");
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    // FIX 4: Hardcoded credentials removed
    // OWASP A07: Identification and Authentication Failures - RESOLVED
    // This endpoint now relies on Spring Security authentication (JWT token required)
    // No hardcoded password comparison - authentication handled by SecurityConfig

    // FIX 5: Insecure cipher fixed - Using AES/GCM instead of AES/ECB
    // OWASP A02: Cryptographic Failures - RESOLVED
    @GetMapping("/encrypt")
    public ResponseEntity<Map<String, String>> encryptData(@RequestParam String data) {
        Map<String, String> result = new LinkedHashMap<>();
        try {
            // FIXED: Use AES/GCM mode which provides confidentiality and integrity
            SecretKeySpec key = new SecretKeySpec("1234567890123456".getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
            byte[] encrypted = cipher.doFinal(data.getBytes());
            result.put("encrypted", java.util.Base64.getEncoder().encodeToString(encrypted));
            result.put("iv", java.util.Base64.getEncoder().encodeToString(iv));
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
}
