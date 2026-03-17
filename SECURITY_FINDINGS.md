# Security Findings and Fixes Report
## Real-Time Air Quality Monitoring Dashboard
**Student:** Jaipal Kasireddy (25156381)
**Date:** 17 March 2026
**Module:** H9CDOS - Cloud DevOpsSec
**Stack:** Java 17 + Spring Boot 3.2.5 | React 18 + Vite | PostgreSQL 15 | AWS (EC2, RDS, S3)

---

## 1. Security Tools Used in CI/CD Pipeline

### 1.1 Backend CI Pipeline Tools

| Tool | Purpose | Configuration | Pipeline Stage |
|------|---------|--------------|----------------|
| **SpotBugs 4.8.3** | Static Application Security Testing (SAST) - detects potential bugs, performance issues, and security vulnerabilities in Java bytecode | Max effort, Medium threshold | CI - after build |
| **OWASP Dependency-Check 9.0.9** | Dependency vulnerability scanning - scans Maven dependencies against the National Vulnerability Database (NVD) for known CVEs | failBuildOnCVSS=7 | CI - after build |
| **Checkstyle 3.3.1** | Code quality and style enforcement using Google Checks standard - ensures consistent code formatting and naming conventions | Google Checks, failsOnError=false | CI - after build |
| **JaCoCo 0.8.11** | Code coverage measurement - enforces minimum test coverage thresholds to ensure adequate testing | Minimum 60% line coverage | CI - after test |

### 1.2 Frontend CI Pipeline Tools

| Tool | Purpose | Configuration | Pipeline Stage |
|------|---------|--------------|----------------|
| **ESLint 8.57** | JavaScript/React static code analysis - checks for code quality issues, unused variables, React best practices | eslint:recommended + react/recommended + react-hooks/recommended | CI - after install |
| **npm audit** | Node.js dependency vulnerability scanning - checks npm packages against the npm advisory database | audit-level=moderate | CI - after build |

### 1.3 Manual Security Testing Tools

| Tool | Purpose | Tests Conducted |
|------|---------|----------------|
| **Playwright (Browser MCP)** | UI-level penetration testing, input validation verification, authorization testing, XSS/SQL injection testing | 40 UI tests |
| **JUnit 5 + Spring Boot Test** | Backend unit and integration testing for controllers, services, security filters | 50 backend tests |

---

## 2. CI Pipeline Results Summary

| Stage | Tool | Result | Details |
|-------|------|--------|---------|
| Build & Test | `mvn clean verify` | **PASS** | 50 tests executed, 0 failures |
| Code Style | Checkstyle (Google Checks) | **PASS** | 0 violations detected |
| SAST | SpotBugs | **50 bugs** | All EI_EXPOSE_REP/EI_EXPOSE_REP2 (mutable object exposure) |
| Dependency Scan | OWASP Dependency-Check | **Configured** | Scans Maven dependencies against NVD |
| Code Coverage | JaCoCo | **78.6% line coverage** | Above 60% minimum threshold |
| Frontend Lint | ESLint | **PASS** | 0 errors, 28 warnings (react-refresh, unused vars) |
| Frontend Build | Vite | **PASS** | Production bundle built successfully |
| Frontend Audit | npm audit | **2 moderate** | esbuild vulnerability via vite dependency |

---

## 3. Static Code Analysis Findings and Fixes

### Finding SAST-01: SpotBugs - Mutable Object Exposure (EI_EXPOSE_REP / EI_EXPOSE_REP2)

| Attribute | Detail |
|-----------|--------|
| **Severity** | Medium |
| **Tool** | SpotBugs 4.8.3 |
| **Count** | 50 bugs |
| **Bug Pattern** | EI_EXPOSE_REP (returning mutable field), EI_EXPOSE_REP2 (storing mutable parameter) |
| **Affected Classes** | User.java, MonitoringZone.java, SensorReading.java, AlertRule.java, Pollutant.java, and all DTO classes |
| **Description** | JPA entity getter methods return direct references to mutable objects such as `List<MonitoringZone>` and `LocalDateTime`. Setter methods store the parameter reference directly without creating a defensive copy. This means external code holding the returned reference can modify the internal state of the entity without going through the setter. |
| **OWASP Category** | A04:2021 - Insecure Design |

**Example of vulnerable code (User.java):**
```java
// SpotBugs EI_EXPOSE_REP: returns mutable List reference
public List<MonitoringZone> getMonitoringZones() {
    return monitoringZones;
}

// SpotBugs EI_EXPOSE_REP2: stores mutable parameter directly
public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
}
```

**Fix - Defensive Copies:**
```java
// Fix: return unmodifiable copy to prevent external modification
public List<MonitoringZone> getMonitoringZones() {
    return Collections.unmodifiableList(monitoringZones);
}

// Fix: LocalDateTime is immutable in Java, so no copy needed
// For mutable Date objects, use: this.createdAt = new Date(createdAt.getTime());
```

**Resolution:** In this project, `LocalDateTime` is inherently immutable in Java, so the EI_EXPOSE_REP warnings for timestamp fields are false positives. For `List` fields, the `@JsonIgnore` annotation prevents serialisation exposure. The JPA `@OneToMany(cascade = ALL, orphanRemoval = true)` ensures Hibernate manages the collection lifecycle. These are accepted as low-risk in the context of a JPA-managed entity where direct field access is controlled by the persistence layer.

---

### Finding SAST-02: SpotBugs Non-Blocking in CI Pipeline

| Attribute | Detail |
|-----------|--------|
| **Severity** | High |
| **Tool** | SpotBugs (CI Pipeline Configuration) |
| **File** | `.github/workflows/ci-cd.yml` |
| **Description** | SpotBugs runs with `continue-on-error: true` in the CI pipeline. Security bugs detected by SpotBugs will not fail the build, allowing developers to merge code with known security issues. |
| **Impact** | Code with potential null pointer dereferences, SQL injection patterns, or other bugs can reach production without review. |

**Vulnerable pipeline configuration:**
```yaml
- name: Run SpotBugs (static bug analysis)
  run: |
    mvn spotbugs:check -B > spotbugs-report.txt 2>&1 || true
    cat spotbugs-report.txt
  continue-on-error: true   # <-- Security checks never block the build
```

**Fix - Make SpotBugs blocking with severity filter:**
```yaml
- name: Run SpotBugs (static bug analysis)
  run: mvn spotbugs:check -B
  # Remove continue-on-error to fail the build on bugs
```

**Additional fix - Add SpotBugs filter to focus on security bugs (spotbugs-security-include.xml):**
```xml
<FindBugsFilter>
  <Match>
    <Bug category="SECURITY"/>
  </Match>
  <Match>
    <Bug category="MALICIOUS_CODE"/>
  </Match>
</FindBugsFilter>
```

---

### Finding SAST-03: Checkstyle - 0 Violations (Resolved)

| Attribute | Detail |
|-----------|--------|
| **Severity** | Informational |
| **Tool** | Checkstyle 3.3.1 (Google Checks) |
| **Result** | **0 violations** |
| **Description** | All Java source code passes Google Checks standard for code formatting, naming conventions, Javadoc requirements, and import ordering. No code quality issues detected. |

**Status:** No fix required. Code quality is maintained through consistent adherence to Google Java Style Guide.

---

### Finding SAST-04: JaCoCo Code Coverage Below Security Threshold

| Attribute | Detail |
|-----------|--------|
| **Severity** | Medium |
| **Tool** | JaCoCo 0.8.11 |
| **Result** | Line: 78.6%, Instruction: 42.3% |
| **Description** | While overall line coverage (78.6%) exceeds the 60% threshold, instruction coverage is 42.3%. Security-critical packages such as `com.airquality.security` (JWT filter, authentication) should have higher dedicated coverage to ensure security logic is thoroughly tested. |
| **Impact** | Untested code paths in authentication filters, JWT validation, and authorization checks may contain exploitable vulnerabilities. |

**Fix - Add package-specific coverage rules in pom.xml:**
```xml
<rule>
  <element>PACKAGE</element>
  <includes>
    <include>com.airquality.security</include>
  </includes>
  <limits>
    <limit>
      <counter>LINE</counter>
      <value>COVEREDRATIO</value>
      <minimum>0.90</minimum>
    </limit>
  </limits>
</rule>
```

**Resolution:** Added ForecastControllerTest (4 tests) and UserControllerTest (5 tests) to increase coverage of previously untested controllers. Total backend tests increased to 50 across 10 test classes. Line coverage improved to 78.6%.

---

## 4. Dependency Vulnerability Scanning Findings and Fixes

### Finding DEP-01: OWASP Dependency-Check CVSS Threshold Too Lenient

| Attribute | Detail |
|-----------|--------|
| **Severity** | High |
| **Tool** | OWASP Dependency-Check 9.0.9 |
| **File** | `pom.xml` |
| **Description** | The `failBuildOnCVSS` threshold is set to 7 (High severity). This means Medium-severity CVEs (CVSS 4.0-6.9) in Maven dependencies are ignored and will not fail the build. |
| **Impact** | Medium-severity vulnerabilities such as information disclosure, partial denial of service, or limited injection attacks can be deployed to production. |
| **OWASP Category** | A06:2021 - Vulnerable and Outdated Components |

**Vulnerable configuration (pom.xml):**
```xml
<failBuildOnCVSS>7</failBuildOnCVSS>
```

**Fix:**
```xml
<failBuildOnCVSS>5</failBuildOnCVSS>
```

---

### Finding DEP-02: OWASP Dependency-Check Non-Blocking in CI

| Attribute | Detail |
|-----------|--------|
| **Severity** | High |
| **Tool** | OWASP Dependency-Check (CI Pipeline) |
| **File** | `.github/workflows/ci-cd.yml` |
| **Description** | OWASP check runs with `continue-on-error: true` and `|| true`, meaning builds with known CVEs in dependencies will pass the pipeline. |
| **Impact** | Known vulnerable dependencies (e.g., Log4Shell, Spring4Shell-type vulnerabilities) can be deployed to production without blocking. |

**Fix - Remove continue-on-error:**
```yaml
- name: Run OWASP Dependency-Check
  run: mvn org.owasp:dependency-check-maven:check -B
  # Removed: continue-on-error: true
```

---

### Finding DEP-03: npm audit - 2 Moderate Vulnerabilities (esbuild)

| Attribute | Detail |
|-----------|--------|
| **Severity** | Medium |
| **Tool** | npm audit |
| **Package** | esbuild (transitive dependency via vite) |
| **Count** | 2 moderate vulnerabilities |
| **Description** | The `esbuild` package bundled with Vite has 2 known moderate vulnerabilities. Since esbuild is a build-time tool and not included in the production bundle, these do not affect the deployed application directly. |
| **Impact** | Build-time supply chain risk. A compromised esbuild could inject malicious code during the build process. |

**Fix:**
```bash
npm audit fix
# Or update vite to a version with patched esbuild
npm install vite@latest
```

**Resolution:** These are build-time-only dependencies and do not ship in the production `dist/` bundle. Risk is accepted for development but should be resolved by upgrading Vite when a patched version is available.

---

### Finding DEP-04: npm audit Non-Blocking in CI

| Attribute | Detail |
|-----------|--------|
| **Severity** | Medium |
| **Tool** | npm audit (Frontend CI Pipeline) |
| **File** | `frontend/.github/workflows/ci-cd.yml` |
| **Description** | `npm audit` runs with `continue-on-error: true` and `|| true`. Frontend dependencies with known vulnerabilities will not block deployment. |
| **Impact** | XSS, prototype pollution, and other client-side vulnerabilities in npm packages may reach production. |

**Fix:**
```yaml
- name: Run npm audit
  run: npm audit --audit-level=moderate
  # Removed: continue-on-error: true
```

---

## 5. Security Vulnerability Analysis Findings and Fixes

### Finding AUTH-01: Hardcoded JWT Secret Key

| Attribute | Detail |
|-----------|--------|
| **Severity** | CRITICAL |
| **File** | `backend/src/main/resources/application.yml` |
| **Description** | The JWT signing secret is hardcoded as a plaintext string in the application configuration file and committed to the Git repository: `secret: "change-this-secret-key-in-production-must-be-at-least-32-characters-long"` |
| **Impact** | Anyone with repository access can forge valid JWT tokens, impersonating any user. This results in a complete authentication bypass. |
| **OWASP Category** | A07:2021 - Identification and Authentication Failures |

**Vulnerable code (application.yml):**
```yaml
jwt:
  secret: "change-this-secret-key-in-production-must-be-at-least-32-characters-long"
  expiration: 86400000
```

**Fix - Use environment variables:**
```yaml
jwt:
  secret: ${JWT_SECRET}
  expiration: ${JWT_EXPIRATION:86400000}
```

**Additional fix - Generate a cryptographically random secret:**
```bash
openssl rand -base64 64
```

---

### Finding AUTH-02: Hardcoded Database Credentials

| Attribute | Detail |
|-----------|--------|
| **Severity** | CRITICAL |
| **File** | `backend/src/main/resources/application.yml` |
| **Description** | Database credentials (`username: root`, `password: root`) are stored in plaintext in the application configuration file committed to source control. |
| **Impact** | Direct database access for anyone with repository access. Risk of data breach, data manipulation, and data exfiltration. |
| **OWASP Category** | A07:2021 - Identification and Authentication Failures |

**Vulnerable code:**
```yaml
datasource:
  url: jdbc:postgresql://localhost:5432/air_quality_db
  username: root
  password: root
```

**Fix:**
```yaml
datasource:
  url: ${SPRING_DATASOURCE_URL}
  username: ${SPRING_DATASOURCE_USERNAME}
  password: ${SPRING_DATASOURCE_PASSWORD}
```

---

### Finding AUTH-03: No Token Revocation Mechanism

| Attribute | Detail |
|-----------|--------|
| **Severity** | High |
| **File** | `backend/src/main/java/com/airquality/security/JwtUtils.java` |
| **Description** | There is no mechanism to revoke or invalidate JWT tokens after issuance. Once issued, tokens remain valid until the 24-hour expiration. Frontend logout only removes the token from localStorage but does not invalidate it server-side. |
| **Impact** | Stolen tokens remain valid even after user logout or password change. Session hijacking persists until token expiry. |
| **OWASP Category** | A07:2021 - Identification and Authentication Failures |

**Fix - Implement token blacklist:**
```java
@Service
public class TokenBlacklistService {
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

    public void blacklist(String token) {
        blacklistedTokens.add(token);
    }

    public boolean isBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }
}

// In JwtAuthenticationFilter: check blacklist before processing token
if (tokenBlacklistService.isBlacklisted(jwt)) {
    filterChain.doFilter(request, response);
    return;
}
```

---

### Finding AUTH-04: No Rate Limiting on Authentication Endpoints

| Attribute | Detail |
|-----------|--------|
| **Severity** | High |
| **File** | `backend/src/main/java/com/airquality/controller/AuthController.java` |
| **Description** | Login (`POST /api/auth/login`) and registration (`POST /api/auth/register`) endpoints have no rate limiting. An attacker can attempt unlimited login attempts for brute-force or credential stuffing attacks. |
| **Impact** | Credential stuffing and brute-force attacks are not mitigated. No lockout mechanism after failed attempts. |
| **OWASP Category** | A07:2021 - Identification and Authentication Failures |

**Fix - Add rate limiting with Bucket4j:**
```java
@PostMapping("/login")
@RateLimiter(name = "authLogin", fallbackMethod = "rateLimitFallback")
public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
    // existing login logic
}
```

---

### Finding AUTH-05: H2 Database Console Exposed Without Authentication

| Attribute | Detail |
|-----------|--------|
| **Severity** | CRITICAL |
| **File** | `backend/src/main/java/com/airquality/security/SecurityConfig.java` |
| **Description** | The H2 database console (`/h2-console/**`) is permitted without authentication in the Spring Security configuration. If this configuration reaches production, it exposes full SQL execution capability on the database. |
| **Impact** | Direct SQL execution, data exfiltration, data manipulation, and potential remote code execution through H2 SQL functions. |
| **OWASP Category** | A01:2021 - Broken Access Control |

**Vulnerable code (SecurityConfig.java):**
```java
.requestMatchers("/h2-console/**").permitAll()
```

**Fix - Restrict H2 console to dev profile:**
```java
@Configuration
@Profile("dev")
public class H2ConsoleSecurityConfig {
    @Bean
    public SecurityFilterChain h2ConsoleFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/h2-console/**")
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(f -> f.disable()));
        return http.build();
    }
}
```

---

### Finding API-01: CORS Wildcard Methods and Headers with Credentials

| Attribute | Detail |
|-----------|--------|
| **Severity** | CRITICAL |
| **File** | `backend/src/main/java/com/airquality/config/CorsConfig.java` |
| **Description** | CORS configuration uses `.allowedMethods("*")` and `.allowedHeaders("*")` combined with `.allowCredentials(true)`. This allows any HTTP method (including TRACE, PATCH, DELETE) and any header from configured origins. |
| **Impact** | Enables CSRF-like attacks via non-standard HTTP methods. Allows `X-Forwarded-For` header spoofing. Potential credential leakage to any allowed origin. |
| **OWASP Category** | A05:2021 - Security Misconfiguration |

**Vulnerable code:**
```java
.allowedMethods("*")
.allowedHeaders("*")
.allowCredentials(true)
```

**Fix - Restrict to required methods and headers only:**
```java
.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
.allowedHeaders("Content-Type", "Authorization", "Accept")
.allowCredentials(true)
.maxAge(3600)
```

---

### Finding API-02: Missing Security Response Headers

| Attribute | Detail |
|-----------|--------|
| **Severity** | Medium |
| **File** | `backend/src/main/java/com/airquality/security/SecurityConfig.java` |
| **Description** | The API does not set security response headers: Content-Security-Policy, X-Content-Type-Options, Strict-Transport-Security, X-XSS-Protection, or Referrer-Policy. |
| **Impact** | Browsers cannot enforce security policies, increasing the risk of XSS, clickjacking, and MIME-type confusion attacks. |
| **OWASP Category** | A05:2021 - Security Misconfiguration |

**Fix - Add security headers in SecurityConfig:**
```java
.headers(headers -> headers
    .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
    .frameOptions(frame -> frame.deny())
    .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31536000))
)
```

---

### Finding API-03: Stored XSS via API - No Server-Side Input Sanitisation

| Attribute | Detail |
|-----------|--------|
| **Severity** | Medium |
| **File** | Service layer (all controllers accept raw string input) |
| **Description** | While Spring Data JPA prevents SQL injection through parameterised queries, there is no server-side sanitisation for stored XSS. During Playwright testing (Test 34), `<script>alert('XSS')</script>` was successfully stored as a zone name in the database. React's JSX escaping prevents script execution on the frontend, but any non-React consumer of the API (mobile app, third-party integration) would receive the raw XSS payload. |
| **Evidence** | Playwright Test 34: XSS payload `<script>alert('XSS')</script>` stored in database via `POST /api/zones`. The payload was rendered as plain text in the React UI (safe) but stored verbatim in the database (unsafe for non-React consumers). |
| **OWASP Category** | A03:2021 - Injection |

**Fix - Add OWASP Java HTML Sanitizer:**
```xml
<!-- pom.xml -->
<dependency>
  <groupId>com.googlecode.owasp-java-html-sanitizer</groupId>
  <artifactId>owasp-java-html-sanitizer</artifactId>
  <version>20240325.1</version>
</dependency>
```
```java
// Sanitize input at the service layer
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

private static final PolicyFactory POLICY = Sanitizers.FORMATTING;

public Zone createZone(ZoneRequest request) {
    String safeName = POLICY.sanitize(request.getName());
    // use safeName instead of raw input
}
```

---

### Finding INFRA-01: SSH Port Open to 0.0.0.0/0

| Attribute | Detail |
|-----------|--------|
| **Severity** | CRITICAL |
| **File** | `backend/terraform/ec2.tf` |
| **Description** | EC2 security group allows SSH (port 22) inbound from `0.0.0.0/0` (any IP address worldwide). This exposes the server to brute-force SSH attacks from the entire internet. |
| **Impact** | Brute-force SSH attacks, unauthorised server access if the key is compromised, and potential lateral movement within the VPC. |
| **OWASP Category** | A05:2021 - Security Misconfiguration |

**Vulnerable Terraform code:**
```hcl
ingress {
  from_port   = 22
  to_port     = 22
  protocol    = "tcp"
  cidr_blocks = ["0.0.0.0/0"]
}
```

**Fix - Restrict to specific admin IP or use SSM:**
```hcl
ingress {
  from_port   = 22
  to_port     = 22
  protocol    = "tcp"
  cidr_blocks = ["YOUR_ADMIN_IP/32"]
  description = "SSH access restricted to admin IP"
}
```

**Better fix - Use AWS Systems Manager Session Manager (no SSH port needed).**

---

### Finding INFRA-02: API Port Exposed Publicly Without Load Balancer

| Attribute | Detail |
|-----------|--------|
| **Severity** | High |
| **File** | `backend/terraform/ec2.tf` |
| **Description** | Spring Boot API port 10004 is exposed to `0.0.0.0/0` with no Application Load Balancer, WAF, or reverse proxy in front. Direct public access to the application server. |
| **Impact** | No DDoS protection, no SSL termination, no WAF rules filtering malicious requests. Direct exposure to application-layer attacks. |

**Fix - Deploy ALB with WAF:**
```hcl
resource "aws_lb" "api" {
  name               = "airquality-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb_sg.id]
  subnets            = [aws_subnet.public_a.id, aws_subnet.public_b.id]
}
```

---

### Finding INFRA-03: No HTTPS/TLS Encryption in Transit

| Attribute | Detail |
|-----------|--------|
| **Severity** | High |
| **File** | `backend/terraform/ec2.tf` |
| **Description** | The Spring Boot application serves HTTP on port 10004 without TLS. All data between client and server is transmitted in plaintext, including JWT tokens, user credentials during login, and sensor data. |
| **Impact** | Man-in-the-middle attacks can intercept JWT tokens and credentials. All API traffic is visible to network observers. |
| **OWASP Category** | A02:2021 - Cryptographic Failures |

**Fix - Use ALB with ACM for TLS termination:**
```hcl
resource "aws_acm_certificate" "api" {
  domain_name       = "api.airquality.example.com"
  validation_method = "DNS"
}
```

---

### Finding INFRA-04: RDS Storage Not Encrypted at Rest

| Attribute | Detail |
|-----------|--------|
| **Severity** | High |
| **File** | `backend/terraform/rds.tf` |
| **Description** | RDS instance does not have `storage_encrypted = true`. Database data including user credentials and sensor readings is stored unencrypted on disk. |
| **Impact** | Physical access to storage or EBS snapshots exposes all data in plaintext. |
| **OWASP Category** | A02:2021 - Cryptographic Failures |

**Fix:**
```hcl
resource "aws_db_instance" "airquality" {
  storage_encrypted = true
  kms_key_id        = aws_kms_key.rds.arn
}
```

---

### Finding INFRA-05: Database Credentials in Terraform State

| Attribute | Detail |
|-----------|--------|
| **Severity** | CRITICAL |
| **File** | `backend/terraform/ec2.tf`, `backend/terraform/rds.tf` |
| **Description** | Database credentials are passed via Terraform variables and embedded in EC2 user data script as plaintext environment variables. Credentials are stored in the Terraform state file in plaintext. |
| **Impact** | Anyone with access to Terraform state or EC2 instance metadata can retrieve database credentials. |

**Fix - Use AWS Secrets Manager:**
```hcl
resource "aws_secretsmanager_secret" "db_credentials" {
  name = "airquality/db-credentials"
}

# Application retrieves credentials at runtime via AWS SDK
```

---

### Finding INFRA-06: No IMDSv2 Enforcement on EC2

| Attribute | Detail |
|-----------|--------|
| **Severity** | Medium |
| **File** | `backend/terraform/ec2.tf` |
| **Description** | EC2 instance does not enforce IMDSv2 (Instance Metadata Service v2). IMDSv1 is vulnerable to SSRF attacks that can steal IAM role credentials from the metadata endpoint (`http://169.254.169.254`). |
| **Impact** | Server-Side Request Forgery (SSRF) attacks can steal EC2 IAM role credentials. |
| **OWASP Category** | A10:2021 - Server-Side Request Forgery |

**Fix:**
```hcl
metadata_options {
  http_endpoint = "enabled"
  http_tokens   = "required"  # Enforces IMDSv2
}
```

---

### Finding FE-01: JWT Token Stored in localStorage

| Attribute | Detail |
|-----------|--------|
| **Severity** | Medium |
| **File** | `frontend/src/services/api.js` |
| **Description** | JWT token is stored in `localStorage`, which is accessible to any JavaScript running on the page. If an XSS vulnerability exists, the attacker can read the token and impersonate the user. |
| **Impact** | XSS vulnerability would allow complete token theft. `localStorage` is not protected by the `HttpOnly` flag. |

**Fix - Store tokens in HttpOnly cookies:**
```java
// Backend: Set JWT as HttpOnly cookie
ResponseCookie cookie = ResponseCookie.from("jwt", token)
    .httpOnly(true)
    .secure(true)
    .path("/")
    .maxAge(86400)
    .sameSite("Strict")
    .build();
response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
```

---

### Finding FE-02: No Content Security Policy (CSP)

| Attribute | Detail |
|-----------|--------|
| **Severity** | Medium |
| **File** | `frontend/index.html` |
| **Description** | No Content-Security-Policy header or meta tag is configured. The application loads external resources (Leaflet map tiles from OpenStreetMap). Without CSP, XSS payloads can load arbitrary external scripts. |
| **Impact** | No browser-level protection against inline script injection or external script loading. |

**Fix - Add CSP meta tag:**
```html
<meta http-equiv="Content-Security-Policy"
  content="default-src 'self'; script-src 'self';
  style-src 'self' 'unsafe-inline';
  img-src 'self' data: https://*.tile.openstreetmap.org;
  connect-src 'self' http://localhost:10004">
```

---

## 6. Manual Penetration Testing Results (Playwright - 40 UI Tests)

| # | Test Case | API Endpoint | Result | Security Category |
|---|-----------|-------------|--------|-------------------|
| 1 | Demo User 1 auto-fill and login | POST `/api/auth/login` | PASS | Authentication |
| 2 | Create zone "Dublin City Centre" | POST `/api/zones` | PASS | CRUD - Zones |
| 3 | Create zone "Cork Harbour" | POST `/api/zones` | PASS | CRUD - Zones |
| 4 | Empty zone form validation | Client-side validation | PASS | Input Validation |
| 5 | Update zone name and radius | PUT `/api/zones/{id}` | PASS | CRUD - Zones |
| 6 | Delete zone "Cork Harbour" | DELETE `/api/zones/{id}` | PASS | CRUD - Zones |
| 7 | Create pollutant PM2.5 | POST `/api/pollutants` | PASS | CRUD - Pollutants |
| 8 | Create pollutant O3 | POST `/api/pollutants` | PASS | CRUD - Pollutants |
| 9 | Empty pollutant form validation | Client-side validation | PASS | Input Validation |
| 10 | Update pollutant threshold | PUT `/api/pollutants/{id}` | PASS | CRUD - Pollutants |
| 11 | Create sensor reading (AQI=76) | POST `/api/readings` | PASS | CRUD - Readings |
| 12 | Create second reading (AQI=92) | POST `/api/readings` | PASS | CRUD - Readings |
| 13 | Empty reading form validation | Client-side validation | PASS | Input Validation |
| 14 | Filter readings by zone | GET `/api/readings?zone_id=` | PASS | Functionality |
| 15 | Create alert rule (CRITICAL, ABOVE) | POST `/api/alerts` | PASS | CRUD - Alerts |
| 16 | Create alert rule (LOW, BELOW) | POST `/api/alerts` | PASS | CRUD - Alerts |
| 17 | Update alert threshold and severity | PUT `/api/alerts/{id}` | PASS | CRUD - Alerts |
| 18 | Delete alert rule | DELETE `/api/alerts/{id}` | PASS | CRUD - Alerts |
| 19 | Empty alert form validation | Client-side validation | PASS | Input Validation |
| 20 | Dashboard summary with live data | GET `/api/dashboard/summary` | PASS | Non-CRUD |
| 21 | Dashboard trend and comparison charts | GET `/api/dashboard/trends`, `/comparison` | PASS | Non-CRUD |
| 22 | Forecast without selection - error toast | Client-side validation | PASS | Input Validation |
| 23 | Forecast with valid zone and pollutant | GET `/api/forecast` | PASS | Non-CRUD |
| 24 | Air Quality Map with AQI colour legend | GET `/api/zones` + `/api/dashboard/summary` | PASS | Non-CRUD |
| 25 | Logout redirects to login page | Client-side auth clear | PASS | Authentication |
| 26 | Invalid credentials - 401 Unauthorised | POST `/api/auth/login` (401) | PASS | Auth Security |
| 27 | Register new user "Test Professor" | POST `/api/auth/register` | PASS | Authentication |
| 28 | Cannot update another user's zone - 403 | PUT `/api/zones/{id}` (403 Forbidden) | PASS | Authorisation |
| 29 | Cannot delete another user's zone - 403 | DELETE `/api/zones/{id}` (403 Forbidden) | PASS | Authorisation |
| 30 | Empty login form validation | Client-side validation | PASS | Input Validation |
| 31 | Empty registration form validation | Client-side validation | PASS | Input Validation |
| 32 | Duplicate email registration - 409 | POST `/api/auth/register` (409 Conflict) | PASS | Auth Security |
| 33 | Demo User 2 login | POST `/api/auth/login` | PASS | Authentication |
| 34 | XSS payload `<script>alert('XSS')</script>` | POST `/api/zones` (stored XSS test) | PASS | XSS Prevention |
| 35 | SQL injection `'; DROP TABLE users; --` | POST `/api/zones` (SQL injection test) | PASS | Injection Prevention |
| 36 | Special characters `& O'Brien's Tower` | POST `/api/zones` | PASS | Edge Case |
| 37 | Latitude boundary (91 > max 90) rejected | Client-side validation | PASS | Input Validation |
| 38 | Negative radius (-500) rejected | Client-side validation | PASS | Input Validation |
| 39 | Protected route /dashboard without login | ProtectedRoute redirect to /login | PASS | Access Control |
| 40 | Protected route /zones without login | ProtectedRoute redirect to /login | PASS | Access Control |

### Key Security Observations from Penetration Testing:

**SQL Injection Prevention (Test 35):** Spring Data JPA uses parameterised queries for all database operations. The SQL injection payload `'; DROP TABLE users; --` submitted via the zone creation form was treated as a literal string value, not executed as SQL. The database remained intact and the payload was stored as plain text. This confirms protection against OWASP A03:2021 - Injection.

**XSS Prevention (Test 34):** React's JSX auto-escaping mechanism prevents script execution in the browser. The XSS payload `<script>alert('XSS')</script>` submitted via the zone creation form was rendered as visible text in the UI, not executed as JavaScript. However, the raw payload was stored in the database (see Finding API-03 above).

**Authorisation Enforcement (Tests 28-29):** Backend ownership checks correctly return HTTP 403 Forbidden when a user attempts to update or delete resources owned by another user. This confirms the ownership-based authorisation model prevents horizontal privilege escalation.

**Input Validation (Tests 4, 9, 13, 19, 30-31, 37-38):** Both frontend (React form validation) and backend (Jakarta Bean Validation with `@Valid` and constraint annotations) reject invalid inputs including boundary coordinates exceeding valid ranges, negative radius values, and empty required fields.

**Access Control (Tests 39-40):** Frontend ProtectedRoute component correctly redirects unauthenticated users to the login page when attempting to access protected routes such as `/dashboard` and `/zones`.

---

## 7. Consolidated Findings and Fixes Summary Table

| # | Finding ID | Severity | Tool/Source | Finding Description | Fix/Remediation | Status |
|---|-----------|----------|-------------|-------------------|-----------------|--------|
| 1 | AUTH-01 | CRITICAL | Manual Review | Hardcoded JWT secret in application.yml | Move to environment variable `${JWT_SECRET}` | Documented |
| 2 | AUTH-02 | CRITICAL | Manual Review | Hardcoded database credentials (root/root) | Use `${SPRING_DATASOURCE_*}` env vars | Documented |
| 3 | AUTH-05 | CRITICAL | Manual Review | H2 console exposed without authentication | Restrict with `@Profile("dev")` | Documented |
| 4 | API-01 | CRITICAL | Manual Review | CORS wildcard methods/headers with credentials | Restrict to GET, POST, PUT, DELETE, OPTIONS | Documented |
| 5 | INFRA-01 | CRITICAL | Terraform Review | SSH (port 22) open to 0.0.0.0/0 | Restrict to admin IP CIDR | Documented |
| 6 | INFRA-05 | CRITICAL | Terraform Review | Database credentials in Terraform state | Use AWS Secrets Manager | Documented |
| 7 | AUTH-03 | HIGH | Manual Review | No JWT token revocation after logout | Implement token blacklist service | Documented |
| 8 | AUTH-04 | HIGH | Manual Review | No rate limiting on auth endpoints | Add Bucket4j rate limiter | Documented |
| 9 | SAST-02 | HIGH | SpotBugs CI Config | SpotBugs non-blocking (continue-on-error) | Remove continue-on-error from pipeline | Documented |
| 10 | DEP-01 | HIGH | OWASP Dep-Check | CVSS threshold too lenient (7) | Lower failBuildOnCVSS to 5 | Documented |
| 11 | DEP-02 | HIGH | OWASP CI Config | OWASP check non-blocking | Remove continue-on-error | Documented |
| 12 | INFRA-02 | HIGH | Terraform Review | API port 10004 exposed publicly | Deploy ALB with WAF | Documented |
| 13 | INFRA-03 | HIGH | Terraform Review | No HTTPS/TLS encryption in transit | Use ALB with ACM certificate | Documented |
| 14 | INFRA-04 | HIGH | Terraform Review | RDS storage not encrypted at rest | Enable storage_encrypted = true | Documented |
| 15 | SAST-01 | MEDIUM | SpotBugs | 50 EI_EXPOSE_REP bugs (mutable exposure) | Return defensive copies or accept for JPA entities | Accepted Risk |
| 16 | SAST-04 | MEDIUM | JaCoCo | Instruction coverage 42.3% | Added 9 additional tests (50 total, 78.6% line) | Fixed |
| 17 | DEP-03 | MEDIUM | npm audit | 2 moderate vulnerabilities (esbuild via vite) | Update vite to patched version | Accepted Risk |
| 18 | DEP-04 | MEDIUM | npm audit CI Config | npm audit non-blocking | Remove continue-on-error | Documented |
| 19 | API-02 | MEDIUM | Manual Review | Missing security response headers | Add CSP, HSTS, X-Frame-Options in SecurityConfig | Documented |
| 20 | API-03 | MEDIUM | Playwright Test 34 | Stored XSS - raw HTML stored in database | Add OWASP Java HTML Sanitizer | Documented |
| 21 | INFRA-06 | MEDIUM | Terraform Review | No IMDSv2 enforcement on EC2 | Set http_tokens = "required" | Documented |
| 22 | FE-01 | MEDIUM | Manual Review | JWT token stored in localStorage | Move to HttpOnly secure cookies | Documented |
| 23 | FE-02 | MEDIUM | Manual Review | No Content Security Policy configured | Add CSP meta tag in index.html | Documented |
| 24 | CQ-01 | LOW | Checkstyle CI Config | Checkstyle non-blocking | Remove continue-on-error | Documented |
| 25 | API-04 | LOW | Manual Review | Swagger UI publicly accessible | Restrict to dev/test profiles | Documented |
| 26 | FE-03 | LOW | Manual Review | No Subresource Integrity for external CDN | Add integrity attributes | Documented |

---

## 8. OWASP Top 10 (2021) Compliance Summary

| OWASP Category | Status | Related Findings |
|----------------|--------|-----------------|
| A01: Broken Access Control | Partially Mitigated | Ownership checks enforced (Tests 28-29); H2 console exposed (AUTH-05) |
| A02: Cryptographic Failures | Vulnerable | No HTTPS (INFRA-03), no RDS encryption (INFRA-04), hardcoded secrets (AUTH-01, AUTH-02) |
| A03: Injection | Mitigated | JPA parameterised queries prevent SQL injection (Test 35); stored XSS risk (API-03) |
| A04: Insecure Design | Partially Mitigated | No rate limiting (AUTH-04), no token revocation (AUTH-03) |
| A05: Security Misconfiguration | Vulnerable | CORS wildcards (API-01), SSH open (INFRA-01), non-blocking security checks |
| A06: Vulnerable Components | Partially Mitigated | OWASP check configured but non-blocking (DEP-01, DEP-02) |
| A07: Auth Failures | Vulnerable | Hardcoded secrets (AUTH-01, AUTH-02), no rate limiting, no token revocation |
| A08: Software Integrity | Partially Mitigated | CI pipeline exists but security checks are non-blocking |
| A09: Logging Failures | Vulnerable | No CloudWatch monitoring, no security audit logging |
| A10: SSRF | Vulnerable | No IMDSv2 enforcement on EC2 (INFRA-06) |

---

## 9. Security Strengths

The application implements several security best practices that provide a strong foundation:

| Security Control | Implementation |
|-----------------|----------------|
| Password Hashing | BCrypt with cost factor 10 via Spring Security PasswordEncoder |
| Authentication | JWT-based stateless authentication with HMAC-SHA256 signing |
| SQL Injection Prevention | Spring Data JPA parameterised queries (verified by Playwright Test 35) |
| XSS Prevention (Client) | React JSX auto-escaping prevents script execution (verified by Test 34) |
| Authorisation | Ownership-based access control on all CRUD operations (verified by Tests 28-29) |
| Route Guards | Frontend ProtectedRoute redirects unauthenticated users (Tests 39-40) |
| Input Validation | Jakarta Bean Validation (`@Valid`) on all API request DTOs |
| CI Pipeline | 4 backend security tools + 2 frontend tools integrated into GitHub Actions |
| Test Coverage | 50 backend tests (78.6% line coverage) + 40 UI penetration tests |
| Infrastructure as Code | Terraform manages all AWS resources with version-controlled configuration |

---

*Report generated: 17 March 2026*
*Tools: SpotBugs, OWASP Dependency-Check, Checkstyle, JaCoCo, npm audit, ESLint, Playwright*
