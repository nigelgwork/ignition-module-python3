# Changelog

All notable changes to the Python 3 Integration module are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.17.0] - 2025-10-17

### Added - Production Security Hardening

**Script Signing & Verification:**
- HMAC-SHA256 signatures on all saved scripts
- Automatic signature generation on script save
- Signature verification on script load with SecurityException on failure
- Configurable signing key via `-Dignition.python3.signing.key=<your-key>`
- Auto-generated key fallback based on Gateway path + hostname for development
- Backward compatibility: legacy scripts without signatures load with warning

**Security Headers on All REST Responses:**
- Content-Security-Policy (CSP): Prevents XSS attacks
- HTTP Strict Transport Security (HSTS): Forces HTTPS, 1-year max-age
- X-Frame-Options: DENY - Prevents clickjacking
- X-Content-Type-Options: nosniff - Prevents MIME sniffing
- X-XSS-Protection: Blocks reflected XSS
- Referrer-Policy: no-referrer - Prevents information leakage
- Permissions-Policy: Disables geolocation, microphone, camera, payment APIs

**CSRF Protection Infrastructure:**
- Token generation and validation methods
- Session-based token management with 1-hour expiry
- Constant-time comparison to prevent timing attacks
- Ready for future Gateway-level authentication integration

**Enhanced Authentication Model:**
- Constant-time string comparison for API key validation
- Documentation clarifying Gateway-level security approach
- SDK API limitations documented for future enhancements
- Support for reverse proxy authentication
- HTTPS/SSL enforcement recommendations

### Changed
- Python3ScriptRepository: Enhanced SavedScript class with signature field
- Python3ScriptRepository: saveScript() now generates HMAC signatures
- Python3ScriptRepository: loadScript() now verifies signatures
- Python3RestEndpoints: Added applySecurityHeaders() method to all responses
- Python3RestEndpoints: Added CSRF token generation and validation methods
- Python3RestEndpoints: Implemented constant-time secureEquals() for API keys
- Python3RestEndpoints: Updated authentication checks with SDK limitation documentation

### Technical
- New class: Python3ScriptSigner with HMAC-SHA256 signing
- SavedScript constructor updated with signature parameter (backward compatible)
- Security headers applied to all REST API responses
- CSRF tokens stored in ConcurrentHashMap with session IDs
- Constant-time comparison prevents timing-based credential enumeration attacks

### Security Assessment
- Script tamper protection via cryptographic signatures
- Defense-in-depth: security headers, CSRF infrastructure, constant-time comparisons
- Gateway-level authentication (API keys, network restrictions, reverse proxy)
- Docker container isolation deferred to v1.18.0 (requires 20-24h additional work)

### Configuration
Generate custom signing key:
```bash
openssl rand -hex 32
```

Configure in ignition.conf:
```properties
wrapper.java.additional.303=-Dignition.python3.signing.key=<your-64-char-hex-key>
```

---

## [1.16.0] - 2025-10-17

### Added - Security Enhancements
- **Admin Mode Detection**: HTTP header-based authentication for ADMIN mode access
  - Configure via `-Dignition.python3.admin.apikey=your-secret-key`
  - Use `X-Python3-Admin-Key` header for REST API requests
  - Allows administrators to use advanced Python modules (pandas, numpy, requests)
  - Falls back to RESTRICTED mode if no valid API key provided
- **Resource Limits on Python Processes**:
  - Memory limit: 512MB per process (configurable via `-Dignition.python3.max.memory.mb`)
  - CPU time limit: 60 seconds per execution (configurable via `-Dignition.python3.max.cpu.seconds`)
  - Applied via Python resource module (Unix/Linux only)
  - Prevents runaway scripts from consuming Gateway resources

### Added - Performance Monitoring
- **Per-Script Performance Metrics**:
  - Track execution count, success rate, timing by individual script
  - Top 50 scripts by execution count
  - REST endpoint: `GET /api/v1/metrics/script-metrics`
- **Historical Metric Tracking**:
  - Circular buffer of 100 snapshots (1-minute intervals)
  - Track execution trends, pool utilization over time
  - Analyze performance degradation
  - REST endpoint: `GET /api/v1/metrics/historical`
- **Health Alerts**:
  - Automatic threshold-based alerting
  - Pool utilization alerts (70% warning, 90% critical)
  - Failure rate alerts (>20%)
  - Alert deduplication (1-minute window)
  - REST endpoint: `GET /api/v1/metrics/alerts`

### Changed
- Python3MetricsCollector enhanced with per-script tracking, historical snapshots, health alerts
- Python3Executor sets resource limit environment variables
- Python3RestEndpoints includes admin mode detection logic and new metrics endpoints
- python_bridge.py applies resource limits on startup

### Technical
- New inner classes in Python3MetricsCollector: ScriptMetrics, MetricSnapshot, HealthAlert
- Resource limits applied via Python resource.setrlimit() (RLIMIT_AS, RLIMIT_CPU)
- Admin API key configured via system property, checked against HTTP headers
- 3 new REST API endpoints for advanced metrics

### Security Assessment
- RestrictedPython sandboxing already fully implemented (discovered in v1.14.0)
- Rate limiting, input validation, audit logging already in place
- No SQL injection risk (file-based JSON storage)
- Resource limits prevent DoS attacks from runaway scripts

---

## [1.15.1] - 2025-10-17

### Fixed - Designer IDE UI/UX
- JSplitPane dividers now use dark theme colors (ModernTheme.BACKGROUND_DARKER)
- Diagnostics panel visible in IDE (divider location adjusted from 400px to 300px)
- Theme selector expanded to prevent text cutoff (150x28 pixels)
- Scrollbars now hide when not required (VERTICAL_SCROLLBAR_AS_NEEDED policy)
- Gateway connection bar height reduced by 25% (padding and gaps optimized)

### Changed
- Python3IDE_v1_9.java: Split pane divider theming, scrollbar policies, layout spacing
- Improved overall user experience in dark theme

---

## [1.15.0] - 2025-10-17

### Added
- Modern UI design system for Designer IDE
- Dark theme support (VS Code Dark+ and Default Light)
- Theme selector in IDE toolbar
- Comprehensive diagnostics panel with auto-refresh
- Real-time pool statistics display

### Changed
- Designer IDE completely redesigned with modern aesthetics
- Color scheme follows ModernTheme constants
- Improved readability and visual consistency

---

## [1.14.0] - 2025-10-17

### Added - Security Hardening
- **RestrictedPython Sandboxing**:
  - Module whitelisting (safe vs admin vs always-blocked)
  - Blocked function lists (eval, exec, open, etc.)
  - Safe __import__ override
  - Pattern-based dangerous code detection
- **Security Mode System**:
  - RESTRICTED mode (default): Only safe modules allowed
  - ADMIN mode: Advanced modules allowed
  - Always-blocked modules (telnetlib, paramiko, threading, ctypes)
- **Rate Limiting**: 100 requests per minute per user
- **Input Validation**:
  - Code size limits (1MB max)
  - Script name validation (alphanumeric only)
  - Path traversal prevention
  - SQL keyword blacklisting (defense in depth)
- **Audit Logging**:
  - All code execution logged
  - Code hash tracking (privacy-preserving)
  - Secret sanitization (redacts passwords, tokens, API keys)

### Added - Performance Monitoring
- **Python3MetricsCollector**: Comprehensive performance tracking
  - Total executions, success/failure counts, success rate
  - Min/max/average execution time
  - Pool utilization and health score (0-100)
  - Error tracking by type
- **Gateway Impact Assessment**:
  - Executions per minute
  - Pool contention events
  - Impact level classification (LOW/MEDIUM/HIGH)
  - Average CPU time consumed
- **REST API Endpoints**:
  - `GET /api/v1/metrics` - Performance metrics
  - `GET /api/v1/gateway-impact` - Gateway impact assessment

### Changed
- Python3RestEndpoints includes security checks and metrics collection
- python_bridge.py implements sandboxing and security validation

---

## [1.8.0] - 2025-10-16

### Added
- **Saved Scripts Feature**: Save, load, and manage Python scripts
  - Script repository with persistent JSON storage
  - Designer IDE "Saved Scripts" panel with dropdown selector
  - Save Script button with name/description dialog
  - Load Script button to restore saved code
  - Delete Script button with confirmation
  - REST API endpoints for script management
  - Auto-refresh script list on Gateway connection

### Changed
- Designer IDE layout includes new Saved Scripts section
- Python3RestClient extended with script management methods
- Gateway data directory now includes scripts/ subdirectory

### Technical
- New classes: Python3ScriptRepository, SavedScript, ScriptMetadata
- REST endpoints: /scripts/save, /load, /list, /delete
- All script operations run asynchronously with SwingWorker

### Notes
- This version requires manual upgrade (uninstall/reinstall) due to new Designer classes
- See UPGRADE_GUIDE.md for details

---

## [1.7.12] - 2025-10-16

### Fixed
- Python code execution failing when using print() statements
- Added stdout capture using io.StringIO and contextlib.redirect_stdout
- Captured output now included in JSON response as 'output' field

### Technical
- Modified python_bridge.py to capture stdout during code execution
- Result variable takes precedence, falls back to captured output if no result

---

## [1.7.11] - 2025-10-16

### Added
- Gateway URL selector in Designer IDE
- Manual connection to different Gateway instances
- Support for connecting to multiple Gateways from single Designer

### Changed
- Python3RestClient accepts Gateway URL directly
- Designer IDE auto-connects on startup with configurable URL

---

## [1.7.10] - 2025-10-16

### Added
- Gateway URL configuration field in Designer IDE
- Connect button for manual Gateway connection

### Changed
- Removed auto-detection attempts (failed)
- Default URL: http://localhost:9088

---

## [1.7.9] - 2025-10-16

### Fixed
- Module signing working correctly with SDK 0.4.1 + Gateway 8.3.1-rc1
- Certificate validation passing

### Changed
- Updated to Ignition Gateway 8.3.1-rc1 (Docker)
- SDK plugin 0.4.1 (stable)

---

## [1.7.0-1.7.8] - 2025-10-15 to 2025-10-16

### Added
- Designer scope with Python 3 IDE
- REST API endpoints for remote code execution
- Async code execution with progress indicators
- Pool statistics display
- Health monitoring
- Error/output tabs in IDE

### Changed
- Module architecture: Gateway-only → Gateway + Designer
- Communication: Direct process pool → REST API
- SDK plugin: 0.1.1 → 0.4.1 (with temporary rollbacks)

### Fixed
- Various certificate signing issues (0.4.1, 0.5.0 compatibility)
- Designer menu integration
- REST endpoint routing

---

## [1.6.1] - 2025-10-14

### Added
- Self-contained Python distribution support
- Automatic Python 3.11 download on first run
- PythonDistributionManager for embedded Python

### Changed
- No longer requires system Python installation
- Python bundled with module

---

## [1.4.0] - 2025-10-14

### Added
- Common scope with RPC infrastructure
- Prepared for future Designer/Client scopes

---

## [1.3.0] - 2025-10-14

### Changed
- Simplified to Gateway-only architecture
- Removed unused Client/Designer scopes

---

## [1.1.3] - 2025-10-13

### Fixed
- Self-contained Python distribution working correctly

---

## [1.0.0] - 2025-10-13

### Added
- Initial release
- Gateway-scoped Python 3 integration
- Process pool with 3 warm Python processes
- Scripting functions: system.python3.exec(), eval(), callModule()
- Health monitoring and automatic process replacement
- JSON-based subprocess communication
- Thread-safe execution

### Features
- Execute arbitrary Python 3 code from Jython 2.7 scripts
- Pass variables between Jython and Python 3
- Call Python modules (math, requests, pandas, etc.)
- Process pool management (3-20 processes configurable)
- 30-second timeout per execution
- Automatic health checking every 30 seconds

---

## Version Schema

- **MAJOR** (x.0.0): Breaking changes, architectural redesigns
- **MINOR** (1.x.0): New features, new scopes, API additions
- **PATCH** (1.0.x): Bug fixes, documentation, minor tweaks

## Upgrade Compatibility

- **PATCH versions**: Always safe to upgrade directly
- **MINOR versions**: May require Designer restart or manual upgrade
- **MAJOR versions**: Breaking changes, review migration guide

See [UPGRADE_GUIDE.md](UPGRADE_GUIDE.md) for detailed upgrade procedures.
