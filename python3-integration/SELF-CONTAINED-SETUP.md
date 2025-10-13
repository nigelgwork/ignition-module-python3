# Self-Contained Python 3 Integration

## Overview

This module is **self-contained** and automatically downloads Python 3 on first use. No manual Python installation required!

## How It Works

### Automatic Python Installation

When you install the module:

1. **First Use**: Module detects no Python installation
2. **Auto-Download**: Downloads Python 3.11.6 standalone distribution (~60MB)
3. **Auto-Extract**: Extracts to `<ignition-data>/python3-integration/python/`
4. **Ready**: Python 3 is now available for all scripts

**Download happens ONCE**. Subsequent uses are instant.

### Fallback to System Python

If Python 3.8+ is already installed on your server, the module will use it automatically (no download needed).

Priority:
1. Check for embedded Python (already downloaded)
2. Check for system Python 3.8+
3. Download embedded Python (if enabled)

## Installation

### Quick Start (Default - Auto-Download Enabled)

1. **Install Module**
   - Upload `.modl` file to Ignition Gateway
   - Module size: ~5MB (without Python)

2. **First Use**
   - Module automatically downloads Python 3 (~60MB)
   - Progress logged in Gateway logs
   - Takes 1-3 minutes depending on internet speed

3. **Done!**
   - All subsequent uses are instant
   - Python 3 is permanently installed

### Check Status

```python
# In Script Console
info = system.python3.getDistributionInfo()
print(info)

# Output:
# {
#   'os': 'linux',
#   'embeddedInstalled': True,
#   'pythonDir': '/path/to/ignition/data/python3-integration/python',
#   'autoDownload': True,
#   'pythonPath': '/path/to/python3',
#   'available': True
# }
```

## Configuration Options

### Option 1: Auto-Download (Default)

```properties
# No configuration needed!
# Module will auto-download on first use
```

### Option 2: Disable Auto-Download

```properties
# In ignition.conf, add:
wrapper.java.additional.X=-Dignition.python3.autodownload=false
```

Then manually install Python 3.8+ on your server.

### Option 3: Specify Python Path

```properties
# In ignition.conf, add:
wrapper.java.additional.X=-Dignition.python3.path=/usr/bin/python3.11
```

Module will use this Python instead of downloading.

### Option 4: Configure Pool Size

```properties
# In ignition.conf, add:
wrapper.java.additional.X=-Dignition.python3.poolsize=5
```

Default is 3 processes.

## Usage Examples

### Check Python Availability

```python
# Check if Python is ready
available = system.python3.isAvailable()
print("Python 3 available:", available)

# Get version
version = system.python3.getVersion()
print("Version:", version['version'])

# Get distribution info
info = system.python3.getDistributionInfo()
print("OS:", info['os'])
print("Embedded installed:", info['embeddedInstalled'])
print("Python path:", info['pythonPath'])
```

### Use Python 3

```python
# Simple calculation
result = system.python3.eval("2 ** 1000")
print(result)  # Huge number!

# Use built-in modules
result = system.python3.callModule("math", "sqrt", [144])
print(result)  # 12.0

# Execute code
code = """
import json
data = {'name': 'Test', 'value': 42}
result = json.dumps(data)
"""
json_str = system.python3.exec(code)
print(json_str)
```

## Installing Python Packages

The embedded Python includes pip. To install packages:

### Method 1: Via Terminal (Recommended)

```bash
# Find Python path
cd <ignition-data>/python3-integration/python

# Install packages (Linux/Mac)
./bin/pip3 install numpy pandas requests

# Install packages (Windows)
python.exe -m pip install numpy pandas requests
```

### Method 2: Via Ignition Script

```python
# Install package programmatically
code = """
import subprocess
import sys

subprocess.check_call([sys.executable, '-m', 'pip', 'install', 'requests'])
result = "Package installed successfully"
"""

result = system.python3.exec(code)
print(result)
```

### Verify Installation

```python
# Check installed packages
code = """
import pkg_resources
installed = [pkg.key for pkg in pkg_resources.working_set]
result = sorted(installed)
"""

packages = system.python3.exec(code)
print("Installed packages:", packages)
```

## Air-Gapped / Offline Environments

For servers without internet access, you have two options:

### Option A: Pre-Install System Python

1. Install Python 3.8+ manually on the server
2. Module will auto-detect and use it
3. No download needed

### Option B: Manual Embedded Python Installation

1. Download Python standalone build:
   - **Windows**: https://github.com/indygreg/python-build-standalone/releases/download/20231002/cpython-3.11.6+20231002-x86_64-pc-windows-msvc-shared-install_only.tar.gz
   - **Linux**: https://github.com/indygreg/python-build-standalone/releases/download/20231002/cpython-3.11.6+20231002-x86_64-unknown-linux-gnu-install_only.tar.gz
   - **macOS x64**: https://github.com/indygreg/python-build-standalone/releases/download/20231002/cpython-3.11.6+20231002-x86_64-apple-darwin-install_only.tar.gz
   - **macOS ARM**: https://github.com/indygreg/python-build-standalone/releases/download/20231002/cpython-3.11.6+20231002-aarch64-apple-darwin-install_only.tar.gz

2. Extract to: `<ignition-data>/python3-integration/python/`

3. Restart module or Gateway

## Troubleshooting

### Module Says Python Not Available

Check distribution status:
```python
info = system.python3.getDistributionInfo()
print(info)
```

If `available: False`, check the error message.

### Download Failed

**Causes:**
- No internet connection
- Firewall blocking GitHub
- Disk space issues

**Solutions:**
1. Check Gateway logs for detailed error
2. Try manual installation (see Air-Gapped section)
3. Install system Python 3.8+

### Python Path Not Found

The module looks for Python in these locations:

**Windows:**
- `python3`, `python`
- `C:\Python311\python.exe`
- `C:\Python310\python.exe`
- `C:\Python39\python.exe`

**Linux:**
- `python3`
- `/usr/bin/python3`
- `/usr/local/bin/python3`

**macOS:**
- `python3`
- `/usr/local/bin/python3`
- `/opt/homebrew/bin/python3`

### Disk Space Requirements

- **Embedded Python**: ~150MB
- **With packages**: ~200-300MB (depending on packages)

Check available space:
```bash
df -h <ignition-data>/python3-integration
```

### Permission Issues (Linux/macOS)

Ensure Ignition user has write permissions:
```bash
sudo chown -R ignition:ignition <ignition-data>/python3-integration
```

### Gateway Logs

Monitor installation progress:
```bash
tail -f <ignition-install>/logs/wrapper.log | grep Python3
```

## Advanced: Reinstall Python

If Python installation is corrupted, you can force reinstall via script:

```python
# WARNING: This will delete and re-download Python

# This functionality would need to be added to Python3ScriptModule
# For now, manually delete the directory:
# rm -rf <ignition-data>/python3-integration/python
# Then restart Ignition Gateway
```

## File Locations

- **Module data**: `<ignition-data>/python3-integration/`
- **Python installation**: `<ignition-data>/python3-integration/python/`
- **Python executable**:
  - Linux/Mac: `python/bin/python3`
  - Windows: `python/python.exe`

## Platform Support

| Platform | Arch | Supported | Python Version |
|----------|------|-----------|----------------|
| Windows | x86_64 | ✅ Yes | 3.11.6 |
| Linux | x86_64 | ✅ Yes | 3.11.6 |
| macOS | x86_64 | ✅ Yes | 3.11.6 |
| macOS | ARM64 | ✅ Yes | 3.11.6 |

## Benefits of Self-Contained Approach

### For Users
- ✅ No manual Python installation
- ✅ Consistent Python version across deployments
- ✅ No PATH or environment conflicts
- ✅ Works on servers without Python installed
- ✅ Easy to troubleshoot (one location)

### For Administrators
- ✅ Reduces deployment complexity
- ✅ No system-wide Python installation needed
- ✅ Isolated from system Python changes
- ✅ Easy to audit (single directory)
- ✅ Simple backup (one directory)

## Security Considerations

### Downloaded Files

Python distributions are downloaded from:
- **Official Source**: GitHub Releases
- **Project**: python-build-standalone
- **Maintainer**: Gregory Szorc (indygreg)
- **Verification**: SHA256 checksums available

### Network Requirements

Auto-download requires outbound HTTPS to:
- `github.com` (redirect)
- `objects.githubusercontent.com` (actual download)

Ports: 443 (HTTPS)

### Permissions

Embedded Python runs with same permissions as Ignition Gateway process. Follow standard security practices:
- Run Ignition under restricted user
- Use firewall rules to limit network access
- Regular security updates

## Comparison: Self-Contained vs System Python

| Aspect | Self-Contained | System Python |
|--------|----------------|---------------|
| Setup | Automatic | Manual install |
| Size | ~150MB | Varies |
| Updates | Manual (reinstall) | System updates |
| Isolation | Fully isolated | Shared |
| Packages | Independent | May conflict |
| Portability | High | Low |
| Air-gapped | Requires prep | Need Python .deb/.rpm |

## Frequently Asked Questions

### Q: Can I use my existing Python installation?
**A:** Yes! The module auto-detects system Python 3.8+. Disable auto-download if you prefer.

### Q: Does this replace Jython?
**A:** No, it complements it. Jython 2.7 still runs your existing scripts. Python 3 is available via `system.python3.*` functions.

### Q: Can I use virtualenv?
**A:** Yes! The embedded Python supports virtualenv. Create and activate as normal.

### Q: What about Python 2?
**A:** Not supported. Use Ignition's built-in Jython 2.7 for Python 2 scripts.

### Q: Can I update Python version?
**A:** Currently ships with Python 3.11.6. Future versions may support multiple Python versions.

### Q: Does it work in Docker?
**A:** Yes! Works in containerized Ignition. Auto-download will occur in container.

### Q: Can I disable auto-download for production?
**A:** Yes! Set `-Dignition.python3.autodownload=false` and pre-install Python.

##Summary

This self-contained approach makes Python 3 integration **plug-and-play**:

1. **Install module** (~5MB)
2. **First use**: Auto-downloads Python 3 (~60MB, one-time)
3. **Done**: Python 3 ready to use!

No configuration, no manual installation, no PATH issues. Just works.
