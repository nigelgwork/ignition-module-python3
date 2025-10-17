# Python 3 Integration for Ignition

A production-ready Ignition module that enables Python 3.11 scripting in Ignition 8.3+, bridging the gap between Ignition's Jython 2.7 and modern Python 3.

## Current Version: 1.14.0

**Latest Features:**
- 🔒 **Security Hardening** - Admin mode, input validation, audit logging (NEW in 1.14.0)
- 📊 **Performance Monitoring** - Real-time metrics and Gateway impact assessment (NEW in 1.14.0)
- ✨ **Saved Scripts** - Build a library of reusable Python scripts
- 🎨 **Designer IDE** - Visual code editor with saved script management
- 🔄 **REST API** - Remote execution and script management
- 📦 **Self-Contained** - Embedded Python 3.11, no system install needed
- ⚡ **Process Pool** - 3-20 concurrent Python processes
- 🔌 **pip Support** - Install any Python package

## Quick Start

### Installation

1. Download: [Python3Integration-1.14.0.modl](/modules/ignition-module-python3/python3-integration/build/Python3Integration-1.14.0.modl)
2. Install: Config → System → Modules → Install or Upgrade a Module
3. Upload the .modl file
4. Restart Gateway

### First Script

**In Ignition Script Console (Gateway):**
```python
# Execute Python 3 code
result = system.python3.exec("""
import requests
response = requests.get('https://api.github.com')
result = f"Status: {response.status_code}"
""")

print(result)  # Output: Status: 200
```

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

### 🔒 Security Hardening (New in 1.14.0)

**Admin Mode:**
- **RESTRICTED mode** (default): Only safe modules allowed (math, json, datetime, etc.)
- **ADMIN mode**: Full access to advanced modules (os, subprocess, requests, pandas, numpy, etc.)
- Automatic role detection (Ignition Administrator role required for ADMIN mode)
- Always-blocked modules for security (telnetlib, paramiko, threading, ctypes, etc.)

**Input Validation:**
- Code size limits (1MB max)
- Script name validation (alphanumeric only)
- Path traversal prevention
- SQL injection protection

**Audit Logging:**
- All code execution logged
- Code hash tracking (not full code for privacy)
- Sanitized logging (redacts passwords, tokens, secrets)

### 📊 Performance Monitoring (New in 1.14.0)

**Real-Time Metrics:**
- Total executions, success/failure counts, success rate
- Min/max/average execution time
- Pool utilization and health score (0-100)
- Error tracking by type

**Gateway Impact Assessment:**
- Executions per minute
- Pool contention events
- Impact level classification (LOW/MEDIUM/HIGH)
- Average CPU time consumed

**REST API Endpoints:**
- `GET /api/v1/metrics` - Get comprehensive performance metrics
- `GET /api/v1/gateway-impact` - Get Gateway impact assessment

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
| `/metrics` | GET | Get performance metrics (NEW in 1.14.0) |
| `/gateway-impact` | GET | Get Gateway impact assessment (NEW in 1.14.0) |
| `/scripts/list` | GET | List all saved scripts |
| `/scripts/load/:name` | GET | Load a saved script |
| `/scripts/save` | POST | Save a new script |
| `/scripts/delete/:name` | DELETE | Delete a script |

## Configuration

### Pool Size

Edit `<ignition-install>/data/ignition.conf`:

```properties
wrapper.java.additional.200=-Dignition.python3.poolsize=5
```

Default: 3 processes

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

Output: `build/Python3Integration-1.14.0.modl`

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
