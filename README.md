# Python 3 Integration for Ignition

A production-ready Ignition module that enables Python 3.11 scripting in Ignition 8.3+, bridging the gap between Ignition's Jython 2.7 and modern Python 3.

## Current Version: 1.17.0

**Latest Features:**
- 🔒 **Production Security** - Script signing, security headers, CSRF protection (NEW in 1.17.0)
- 🔒 **Security Hardening** - Admin mode detection, resource limits, sandboxing (1.16.0)
- 📊 **Performance Monitoring** - Per-script metrics, historical tracking, health alerts (1.16.0)
- ✨ **Saved Scripts** - Build a library of reusable Python scripts
- 🎨 **Designer IDE** - Visual code editor with saved script management
- 🔄 **REST API** - Remote execution and script management
- 📦 **Self-Contained** - Embedded Python 3.11, no system install needed
- ⚡ **Process Pool** - 3-20 concurrent Python processes
- 🔌 **pip Support** - Install any Python package

## Quick Start

### Installation

1. Download: [Python3Integration-1.17.0.modl](/modules/ignition-module-python3/python3-integration/build/Python3Integration-1.17.0.modl)
2. Install: Config → System → Modules → Install or Upgrade a Module
3. Upload the .modl file
4. Restart Gateway

### First Script

**In Designer IDE:**
1. Tools → Python 3 IDE
2. Connect to Gateway
3. Write code in editor
4. Click "Execute" (or Ctrl+Enter)
5. Save your script for later!

## Key Features

### 🎯 Core Functionality

| Feature | Description |
|---------|-------------|
| **Execute Code** | Run arbitrary Python 3 code from Jython scripts |
| **Evaluate Expressions** | Evaluate Python expressions and return values |
| **Call Modules** | Direct function calls to Python modules |
| **Variable Passing** | Share data between Jython and Python 3 |
| **Package Support** | Install any package via pip |

### 🔒 Production Security (NEW in 1.17.0)

**Script Signing & Verification:**
- HMAC-SHA256 signatures on all saved scripts
- Automatic tamper detection on script load
- Throws SecurityException if signature verification fails
- Configurable signing key via `-Dignition.python3.signing.key=<your-key>`
- Auto-generated key fallback for development

**Security Headers:**
- Content-Security-Policy (CSP) - prevents XSS attacks
- HTTP Strict Transport Security (HSTS) - forces HTTPS
- X-Frame-Options: DENY - prevents clickjacking
- X-Content-Type-Options: nosniff - prevents MIME sniffing
- Referrer-Policy: no-referrer - prevents information leakage
- Permissions-Policy - disables unnecessary browser features

**CSRF Protection Infrastructure:**
- Token generation and validation methods
- Session-based token management
- Constant-time comparison to prevent timing attacks
- Ready for Gateway-level authentication integration

**Authentication Model:**
- Gateway-level security (API keys, network restrictions)
- SDK limitations documented for future enhancements
- Reverse proxy authentication support
- HTTPS/SSL enforcement recommended

### 🔒 Security Hardening (Enhanced in 1.16.0)

**Admin Mode Detection (1.16.0):**
- **RESTRICTED mode** (default): Only safe modules allowed (math, json, datetime, etc.)
- **ADMIN mode**: Full access to advanced modules (os, subprocess, requests, pandas, numpy, etc.)
- HTTP header-based authentication via `X-Python3-Admin-Key`
- Configure admin API key: `-Dignition.python3.admin.apikey=your-secret-key`
- Always-blocked modules for security (telnetlib, paramiko, threading, ctypes, etc.)

**Resource Limits (NEW in 1.16.0):**
- **Memory limit**: 512MB per process (configurable)
- **CPU time limit**: 60 seconds per execution (configurable)
- Prevents runaway scripts from consuming Gateway resources
- Applied via Python resource module (Unix/Linux)

**Input Validation:**
- Code size limits (1MB max)
- Script name validation (alphanumeric only)
- Path traversal prevention
- No SQL injection risk (file-based storage)

**Audit Logging:**
- All code execution logged
- Code hash tracking (not full code for privacy)
- Sanitized logging (redacts passwords, tokens, secrets)

**RestrictedPython Sandboxing:**
- Module whitelisting (safe vs admin modules)
- Blocked function lists (eval, exec, open, etc.)
- Safe __import__ override
- Pattern-based dangerous code detection

### 📊 Performance Monitoring (Enhanced in 1.16.0)

**Real-Time Metrics:**
- Total executions, success/failure counts, success rate
- Min/max/average execution time
- Pool utilization and health score (0-100)
- Error tracking by type

**Per-Script Metrics (NEW in 1.16.0):**
- Track performance by individual script
- Execution count, success rate per script
- Average/min/max timing per script
- Top 50 scripts by execution count

**Historical Tracking (NEW in 1.16.0):**
- Circular buffer of 100 snapshots (1-minute intervals)
- Track execution trends over time
- Pool utilization history
- Analyze performance degradation

**Health Alerts (NEW in 1.16.0):**
- Automatic threshold-based alerts
- Pool utilization alerts (70% warning, 90% critical)
- Failure rate alerts (>20%)
- Alert deduplication (1-minute window)

**Gateway Impact Assessment:**
- Executions per minute
- Pool contention events
- Impact level classification (LOW/MEDIUM/HIGH)
- Average CPU time consumed

**REST API Endpoints:**
- `GET /api/v1/metrics` - Get comprehensive performance metrics
- `GET /api/v1/gateway-impact` - Get Gateway impact assessment
- `GET /api/v1/metrics/script-metrics` - Per-script performance (NEW in 1.16.0)
- `GET /api/v1/metrics/historical` - Historical snapshots (NEW in 1.16.0)
- `GET /api/v1/metrics/alerts` - Active health alerts (NEW in 1.16.0)

### 💾 Saved Scripts (New in 1.8.0)

- Save scripts with names and descriptions
- Load scripts from dropdown selector
- Delete unwanted scripts
- Scripts persist across Gateway restarts
- Accessible from any Designer instance

### 🖥️ Designer IDE (Added in 1.7.0)

- Visual code editor with monospace font
- Async execution (non-blocking UI)
- Separate output and error tabs
- Real-time pool statistics
- Execution timing
- Gateway URL selector for multi-Gateway environments

### 🔧 Process Pool

- 3-20 concurrent Python processes (configurable)
- Automatic health monitoring (30s intervals)
- Failed process auto-replacement
- Thread-safe borrowing/returning
- 30-second timeout per execution

## Architecture

```
┌─────────────────────────────────────────┐
│   Ignition Gateway (Jython 2.7)         │
│                                         │
│  ┌────────────────────────────────┐    │
│  │  Python3ProcessPool            │    │
│  │                                │    │
│  │  ┌──────────────────────────┐ │    │
│  │  │ Python 3.11 Process #1   │ │    │
│  │  │ - Full stdlib            │ │    │
│  │  │ - pip packages           │ │    │
│  │  │ - JSON communication     │ │    │
│  │  └──────────────────────────┘ │    │
│  │  ┌──────────────────────────┐ │    │
│  │  │ Python 3.11 Process #2   │ │    │
│  │  └──────────────────────────┘ │    │
│  │  ┌──────────────────────────┐ │    │
│  │  │ Python 3.11 Process #3   │ │    │
│  │  └──────────────────────────┘ │    │
│  └────────────────────────────────┘    │
│                                         │
│  ┌────────────────────────────────┐    │
│  │  REST API Endpoints            │    │
│  │  - /exec, /eval                │    │
│  │  - /scripts/* (NEW)            │    │
│  │  - /pool-stats, /health        │    │
│  └────────────────────────────────┘    │
└─────────────────────────────────────────┘
         ↕ REST API
┌─────────────────────────────────────────┐
│   Ignition Designer                      │
│                                         │
│  ┌────────────────────────────────┐    │
│  │  Python 3 IDE                  │    │
│  │  - Code editor                 │    │
│  │  - Saved scripts dropdown      │    │
│  │  - Save/Load/Delete            │    │
│  │  - Output/Error display        │    │
│  └────────────────────────────────┘    │
└─────────────────────────────────────────┘
```

## API Reference

### Scripting Functions

```python
# Execute code
system.python3.exec(code, variables={})

# Evaluate expression
system.python3.eval(expression, variables={})

# Call module function
system.python3.callModule(module, function, args=[], kwargs={})

# Get Python version
system.python3.getVersion()

# Get pool statistics
system.python3.getPoolStats()

# Run example test
system.python3.example()
```

### REST API Endpoints

**Base URL:** `http://gateway:port/data/python3integration/api/v1`

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/exec` | POST | Execute Python code |
| `/eval` | POST | Evaluate Python expression |
| `/pool-stats` | GET | Get process pool statistics |
| `/health` | GET | Health check |
| `/metrics` | GET | Get performance metrics |
| `/gateway-impact` | GET | Get Gateway impact assessment |
| `/metrics/script-metrics` | GET | Per-script performance data (NEW in 1.16.0) |
| `/metrics/historical` | GET | Historical metric snapshots (NEW in 1.16.0) |
| `/metrics/alerts` | GET | Active health alerts (NEW in 1.16.0) |
| `/scripts/list` | GET | List all saved scripts |
| `/scripts/load/:name` | GET | Load a saved script |
| `/scripts/save` | POST | Save a new script |
| `/scripts/delete/:name` | DELETE | Delete a script |

## Configuration

Edit `<ignition-install>/data/ignition.conf`:

### Pool Size

```properties
wrapper.java.additional.200=-Dignition.python3.poolsize=5
```

Default: 3 processes

### Admin API Key (NEW in 1.16.0)

Enable ADMIN mode for administrators (allows pandas, numpy, requests, etc.):

```properties
wrapper.java.additional.300=-Dignition.python3.admin.apikey=your-32-character-secret-key
```

**Usage:**
```bash
curl -X POST http://gateway:9088/data/python3integration/api/v1/exec \
  -H "X-Python3-Admin-Key: your-32-character-secret-key" \
  -H "Content-Type: application/json" \
  -d '{"code": "import pandas as pd\nresult = str(pd.DataFrame())"}'
```

### Resource Limits (NEW in 1.16.0)

Prevent runaway scripts from consuming Gateway resources:

```properties
wrapper.java.additional.301=-Dignition.python3.max.memory.mb=512
wrapper.java.additional.302=-Dignition.python3.max.cpu.seconds=60
```

Defaults: 512MB memory, 60 seconds CPU time

### Script Signing Key (NEW in 1.17.0)

Configure custom signing key for production deployments:

```properties
wrapper.java.additional.303=-Dignition.python3.signing.key=<your-64-character-hex-key>
```

**Generate key:**
```bash
openssl rand -hex 32
```

Default: Auto-generated from Gateway installation path and hostname

### Python Path (Optional)

To use system Python instead of embedded:

```properties
wrapper.java.additional.201=-Dignition.python3.path=/usr/bin/python3.11
```

### Installing Packages

**Via Docker (development):**
```bash
docker exec ignition-python3-test \
  /usr/local/bin/ignition/data/python3-integration/python/python/bin/python3.11 \
  -m pip install pandas numpy scikit-learn
```

**Via System (production):**
```bash
cd <ignition-install>/data/python3-integration/python/python/bin
./python3.11 -m pip install requests pandas numpy
```

Installed packages are available to all Python processes immediately.

## Upgrading

### From 1.16.x to 1.17.0

**Important:** This upgrade adds production-ready security enhancements.

**Recommended Process:**
1. Install v1.17.0 over existing version
2. Restart Gateway
3. Restart Designer clients (if using Designer IDE)
4. (Optional) Configure custom signing key for production

**Key Changes:**
- **Security:** HMAC-SHA256 script signing & verification
- **Security:** Comprehensive security headers (CSP, HSTS, X-Frame-Options, etc.)
- **Security:** CSRF protection infrastructure
- **Security:** Constant-time string comparison for API keys
- **Documentation:** Authentication model clarified (Gateway-level security)

**Configuration (Optional):**
```properties
# Custom signing key for production
wrapper.java.additional.303=-Dignition.python3.signing.key=<your-64-char-hex-key>
```

**Note:** Existing saved scripts will continue to work. On first save after upgrade, they will be signed automatically. Legacy scripts without signatures will load with a warning in Gateway logs.

### From 1.15.x to 1.16.0

**Important:** This upgrade adds significant security and performance enhancements.

**Recommended Process:**
1. Install v1.16.0 over existing version
2. Restart Gateway
3. Restart Designer clients (if using Designer IDE)
4. (Optional) Configure admin API key for ADMIN mode access

**Key Changes:**
- **Security:** Admin mode detection via HTTP headers
- **Security:** Resource limits on Python processes (memory, CPU time)
- **Performance:** Per-script performance metrics
- **Performance:** Historical metric tracking (100 snapshots)
- **Performance:** Automatic health alerts
- **API:** 3 new REST endpoints for advanced metrics

**Configuration (Optional):**
```properties
# Enable ADMIN mode for administrators
wrapper.java.additional.300=-Dignition.python3.admin.apikey=your-secret-key

# Resource limits (prevent runaway scripts)
wrapper.java.additional.301=-Dignition.python3.max.memory.mb=512
wrapper.java.additional.302=-Dignition.python3.max.cpu.seconds=60
```

### From 1.14.x to 1.15.1

**Important:** This upgrade includes UI/UX bug fixes and improvements.

**Recommended Process:**
1. Install v1.15.1 over existing version
2. Restart Gateway
3. Restart Designer clients (if using Designer IDE)

**Key Changes:**
- Fixed JSplitPane divider colors in dark theme
- Made diagnostics panel visible in IDE
- Expanded theme selector to prevent text cutoff
- Scrollbars now hide when not required
- Reduced gateway connection bar height

### From 1.8.x to 1.14.0

**Important:** This upgrade adds security hardening and performance monitoring.

**Recommended Process:**
1. Install v1.14.0 over existing version
2. Restart Gateway
3. Restart Designer clients (if using Designer IDE)

**Key Changes:**
- Default security mode is RESTRICTED (only safe Python modules allowed)
- Administrators automatically get ADMIN mode (can use advanced modules)
- New REST endpoints for metrics and Gateway impact assessment

### From 1.7.x to 1.8.0

**Important:** This upgrade adds new Designer classes and requires a manual process.

**Method 1 (Recommended):**
1. Uninstall v1.7.x
2. Restart Gateway
3. Install v1.8.0+
4. Restart Designer clients

**Method 2:**
1. Install v1.8.0+ over v1.7.x
2. Close all Designer clients
3. Reopen Designer clients

See [UPGRADE_GUIDE.md](UPGRADE_GUIDE.md) for detailed procedures.

## Examples

### Execute Code with Output

```python
code = """
import json
import datetime

data = {
    'timestamp': str(datetime.datetime.now()),
    'value': 42
}
result = json.dumps(data, indent=2)
"""

output = system.python3.exec(code)
print(output)
```

### Call Module Function

```python
# Calculate square root
result = system.python3.callModule("math", "sqrt", [16])
print(result)  # 4.0
```

### Use Installed Packages

```python
code = """
import requests
response = requests.get('https://api.github.com/repos/python/cpython')
result = f"Python repo stars: {response.json()['stargazers_count']}"
"""

stars = system.python3.exec(code)
print(stars)
```

## Troubleshooting

### Module Won't Load

**Check Gateway logs:** `<ignition-install>/logs/wrapper.log`

**Common issues:**
- Ensure Gateway is Ignition 8.3.1-rc1 or later
- Verify module is signed (check for certificate errors)

### Designer IDE Not Showing

**Solution:** Close and reopen Designer. If issue persists, clear Designer cache:
- Windows: `%USERPROFILE%\.ignition\cache\`
- Mac/Linux: `~/.ignition/cache/`

### Python Execution Fails

**Check:**
1. Pool stats show healthy processes
2. Gateway URL is correct in IDE
3. No firewall blocking port 9088
4. Gateway logs for Python errors

### Saved Scripts Not Loading

**Solution:** Click "Refresh" button in Saved Scripts panel

## Development

### Building from Source

```bash
cd python3-integration
./gradlew clean build --no-daemon
```

Output: `build/Python3Integration-1.17.0.modl`

### Testing

```bash
# Start Docker test environment
docker-compose up -d

# Wait for Gateway startup
docker logs -f ignition-python3-test

# Install module via web UI
# http://localhost:9088
```

### Project Structure

```
python3-integration/
├── common/          # Shared code (Gateway + Client)
├── gateway/         # Gateway-scoped code
│   ├── GatewayHook.java
│   ├── Python3ProcessPool.java
│   ├── Python3ScriptRepository.java (NEW)
│   └── python_bridge.py
├── designer/        # Designer-scoped code
│   ├── DesignerHook.java
│   ├── Python3IDE.java
│   └── Python3RestClient.java
└── build.gradle.kts
```

## Documentation

- [CHANGELOG.md](CHANGELOG.md) - Version history and changes
- [UPGRADE_GUIDE.md](UPGRADE_GUIDE.md) - Detailed upgrade procedures
- [CLAUDE.md](CLAUDE.md) - Development guidance for AI assistants
- [docs/knowledge-base/](docs/knowledge-base/) - Complete SDK documentation

## Technical Details

- **Module ID:** `com.inductiveautomation.ignition.examples.python3`
- **SDK Version:** 0.4.1
- **Python Version:** 3.11.5 (embedded)
- **Ignition Version:** 8.3.0+ (tested on 8.3.1-rc1)
- **License:** Free module, no license required
- **Scopes:** Gateway (G), Designer (D), Common (G+D)

## Credits

Developed by Gaskony with assistance from Claude Code (Anthropic).

## License

See individual source files for licensing information.
