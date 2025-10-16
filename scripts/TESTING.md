# Python 3 Integration Module - Testing Guide

This document provides comprehensive testing instructions for the Python 3 Integration module for Ignition.

## Table of Contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Automated Testing with Docker](#automated-testing-with-docker)
- [Manual Testing](#manual-testing)
- [Testing Checklist](#testing-checklist)
- [Troubleshooting](#troubleshooting)

## Overview

The Python 3 Integration module provides Python 3 scripting capabilities within Ignition. This guide covers:

- Automated testing using Docker Compose
- Manual testing procedures
- Verification of core functionality
- Common issues and solutions

## Prerequisites

### For Automated Testing
- Docker and Docker Compose installed
- At least 2GB free RAM
- Ports 9088 and 8043 available

### For Manual Testing
- Ignition 8.3+ Gateway running
- Designer access
- Admin credentials
- Unsigned modules enabled: `-Dignition.allowunsignedmodules=true`

### For Building
- Java JDK 17
- Gradle 8.5+ (wrapper included)
- Git (for version control)

## Automated Testing with Docker

The fastest way to test the module is using the provided Docker Compose environment.

### Quick Start

```bash
# Navigate to project root
cd /modules/ignition-module-python3

# Run automated build and test
./scripts/build-and-test.sh
```

This script will:
1. Clean Zone.Identifier files
2. Build the module
3. Start Ignition Gateway in Docker
4. Wait for Gateway to be ready

### Manual Docker Testing

```bash
# 1. Build the module
cd python3-integration
./gradlew clean build

# 2. Start Docker environment
docker-compose up -d

# 3. Wait for Gateway startup (about 60 seconds)
docker-compose logs -f ignition

# 4. Access Gateway
# Gateway Web UI: http://localhost:9088
# Username: admin
# Password: password

# 5. Install module
# - Navigate to Config → System → Modules
# - Click "Install or Upgrade a Module"
# - Select: python3-integration/build/Python3Integration-X.X.X.modl

# 6. Connect Designer
# - Launch Designer
# - Connect to: localhost:9088
# - Username: admin, Password: password

# 7. Run tests (see Testing Checklist below)

# 8. Cleanup when done
docker-compose down
docker volume rm ignition-data  # Optional: Remove persistent data
```

### Docker Environment Details

- **Gateway URL**: http://localhost:9088
- **Gateway SSL**: https://localhost:8043
- **Admin User**: admin
- **Admin Password**: password
- **Ignition Version**: 8.3.0
- **Edition**: Standard
- **Data Volume**: `ignition-data` (persists across restarts)
- **Module Directory**: `./python3-integration/build` (mounted to `/modules`)

## Manual Testing

### 1. Module Installation

**Steps:**
1. Navigate to Gateway webpage: `http://your-gateway:8088`
2. Go to: Config → System → Modules
3. Click "Install or Upgrade a Module"
4. Browse to: `python3-integration/build/Python3Integration-X.X.X.modl`
5. Click "Install"
6. Wait for module to install and Gateway to restart

**Expected Results:**
- Module appears in module list
- Status shows "Running"
- License shows "Free" (not "Trial")
- No error messages in Gateway logs

### 2. Designer Script Console Testing

**Steps:**
1. Launch Designer
2. Open Script Console (Tools → Script Console or Ctrl+Shift+K)
3. Verify project is configured as "Gateway Scripting Project"
4. Run the following tests:

**Test 1: Check Availability**
```python
# Should return True if module is loaded
result = system.python3.isAvailable()
print("Python 3 Available:", result)
```

**Expected Output:**
```
Python 3 Available: True
```

**Test 2: Simple Expression Evaluation**
```python
# Test basic Python 3 math
result = system.python3.eval("2 ** 100")
print("2^100 =", result)
```

**Expected Output:**
```
2^100 = 1267650600228229401496703205376
```

**Test 3: Code Execution**
```python
# Execute Python code
code = """
import sys
result = f"Python {sys.version}"
"""
result = system.python3.exec(code)
print("Python Version:", result)
```

**Expected Output:**
```
Python Version: Python 3.11.6 (main, ...)
```

**Test 4: Variables Passing**
```python
# Pass variables to Python
variables = {"a": 10, "b": 20}
result = system.python3.eval("a + b", variables)
print("10 + 20 =", result)
```

**Expected Output:**
```
10 + 20 = 30.0
```

**Test 5: Module Function Call**
```python
# Call Python standard library function
import java.util.ArrayList
args = java.util.ArrayList()
args.add(16)

result = system.python3.callModule("math", "sqrt", args)
print("sqrt(16) =", result)
```

**Expected Output:**
```
sqrt(16) = 4.0
```

**Test 6: Get Version Info**
```python
# Get detailed version information
version_info = system.python3.getVersion()
print("Version Info:", version_info)
```

**Expected Output:**
```
Version Info: {available=True, version=3.11.6 (main, Oct  2 2023, 13:45:54) [GCC 11.4.0]}
```

**Test 7: Pool Statistics**
```python
# Check process pool health
stats = system.python3.getPoolStats()
print("Pool Stats:", stats)
```

**Expected Output:**
```
Pool Stats: {totalSize=3, available=3, inUse=0, healthy=3}
```

**Test 8: Distribution Info**
```python
# Check Python distribution details
info = system.python3.getDistributionInfo()
print("Distribution Info:", info)
```

**Expected Output:**
```
Distribution Info: {os=linux, embeddedInstalled=False, pythonPath=/usr/bin/python3, available=True, ...}
```

### 3. Gateway Logs Verification

**Steps:**
1. Go to: Config → System → Console/Logs
2. Look for Python 3 Integration messages

**Expected Log Messages:**
```
INFO  | Python 3 Integration module setup
INFO  | Using Python: /usr/bin/python3
INFO  | Initializing Python 3 process pool (size: 3)
INFO  | Python 3 Integration module started successfully
INFO  | Registering Python 3 scripting functions
INFO  | Python 3 scripting functions registered successfully
```

**No Error Messages Should Appear:**
- No "Failed to initialize Python 3 process pool"
- No "Python 3 not found"
- No AttributeError or import errors

### 4. Performance Testing

**Long-Running Script Test:**
```python
import time
start = time.time()

# Execute 100 Python expressions
for i in range(100):
    result = system.python3.eval("2 ** %d" % i)

elapsed = time.time() - start
print("Executed 100 expressions in %.2f seconds" % elapsed)
```

**Expected Results:**
- Should complete in less than 10 seconds
- No timeouts or process crashes
- Pool statistics should show healthy processes

## Testing Checklist

Use this checklist to verify full functionality:

### Installation
- [ ] Module installs without errors
- [ ] Module shows as "Running" status
- [ ] License shows as "Free" (not "Trial")
- [ ] No errors in Gateway logs during startup

### Basic Functionality
- [ ] `system.python3.isAvailable()` returns `True`
- [ ] `system.python3.eval()` executes simple expressions
- [ ] `system.python3.exec()` executes code blocks
- [ ] `system.python3.callModule()` calls stdlib functions
- [ ] Variables can be passed between Ignition and Python
- [ ] Python results return to Ignition correctly

### Process Pool
- [ ] `system.python3.getPoolStats()` shows healthy processes
- [ ] Multiple concurrent requests work correctly
- [ ] Processes recover from errors automatically
- [ ] Pool remains healthy after long running tasks

### Error Handling
- [ ] Invalid Python syntax raises clear error message
- [ ] Python exceptions are caught and reported
- [ ] Traceback information is available
- [ ] Failed processes are replaced automatically

### Version Information
- [ ] `system.python3.getVersion()` returns version info
- [ ] `system.python3.getDistributionInfo()` shows Python path
- [ ] Python 3.8+ is being used

### Gateway Scripts
- [ ] Functions work from Gateway-scoped scripts
- [ ] Functions work from scheduled Gateway events
- [ ] Functions work from Transaction Groups

### Cleanup
- [ ] Module uninstalls cleanly
- [ ] No orphaned Python processes after shutdown
- [ ] No errors in logs during module shutdown

## Troubleshooting

### Issue: Module shows as "Trial" instead of "Free"

**Symptom:** License status shows "Trial Mode"

**Solution:** This was fixed in v1.1.1. Upgrade to latest version.

### Issue: AttributeError - 'object has no attribute python3'

**Symptom:**
```
AttributeError: 'com.inductiveautomation.ignition.designer.gui.tool' object has no attribute 'python3'
```

**Current Status:** Known issue under investigation. The module registers functions in Gateway scope but they may not be visible in Designer Script Console.

**Workarounds:**
1. Verify project is set as "Gateway Scripting Project"
2. Try running from Gateway-scoped tag change script
3. Check Gateway logs to verify module loaded successfully
4. Restart Gateway and Designer

### Issue: Python process fails to start

**Symptom:**
```
ERROR | Failed to initialize Python 3 process pool
ERROR | Python 3 not found
```

**Solutions:**
1. Install Python 3.8+ on the Gateway server
2. Enable auto-download: `-Dignition.python3.autodownload=true`
3. Specify Python path: `-Dignition.python3.path=/path/to/python3`

**Verify Python:**
```bash
python3 --version  # Should be 3.8 or higher
which python3      # Should show path
```

### Issue: Unsigned module error

**Symptom:**
```
Module signature verification failed
```

**Solution:** Add to `ignition.conf`:
```
wrapper.java.additional.N=-Dignition.allowunsignedmodules=true
```

### Issue: Docker container won't start

**Symptom:** `docker-compose up` fails or times out

**Solutions:**
1. Check Docker is running: `docker ps`
2. Check port availability: `netstat -an | grep 9088`
3. Increase Docker memory allocation (minimum 2GB)
4. Check Docker logs: `docker-compose logs ignition`

### Issue: Script timeout errors

**Symptom:**
```
Python3Exception: No response from Python process (timeout: 30000ms)
```

**Solutions:**
1. Check pool statistics: `system.python3.getPoolStats()`
2. Increase pool size: `-Dignition.python3.poolsize=5`
3. Optimize Python script (reduce computation time)
4. Check for infinite loops in Python code

### Issue: Import errors in Python

**Symptom:**
```
ModuleNotFoundError: No module named 'numpy'
```

**Solution:** The embedded Python has limited packages. Options:
1. Use system Python with packages pre-installed
2. Install packages to embedded Python directory
3. Use only Python standard library

## Module Configuration

### System Properties

Add these to `ignition.conf` under `wrapper.java.additional.N`:

```properties
# Pool size (default: 3)
-Dignition.python3.poolsize=3

# Auto-download embedded Python (default: true)
-Dignition.python3.autodownload=true

# Custom Python path (optional)
-Dignition.python3.path=/usr/bin/python3

# Allow unsigned modules
-Dignition.allowunsignedmodules=true
```

### Environment Variables

These can also be set as environment variables:
- `IGNITION_PYTHON3_POOLSIZE`
- `IGNITION_PYTHON3_AUTODOWNLOAD`
- `IGNITION_PYTHON3_PATH`

## Version History

### v1.1.1 (Current)
- Fixed: Module showing as "Trial" instead of "Free"
- Fixed: All checkstyle violations (15 → 0)
- Improved: Code quality and documentation
- Added: Comprehensive testing documentation
- Added: Docker-based testing environment

### v1.1.0
- Added: Docker Compose test environment
- Added: Automated build and test scripts
- Known Issue: AttributeError in Designer Script Console

### v1.0.0
- Initial release
- Python 3 integration via subprocess process pool
- Support for eval, exec, and module function calls
- Automatic Python distribution management

## Support

### Resources
- **Documentation**: [README.md](./README.md)
- **Architecture**: [docs/knowledge-base/](./docs/knowledge-base/)
- **Issues**: GitHub Issues (if repository is public)

### Reporting Issues

When reporting issues, include:
1. Module version (check Config → Modules)
2. Ignition version and edition
3. Operating system (Windows/Linux/macOS)
4. Python version (`python3 --version`)
5. Error messages from Gateway logs
6. Steps to reproduce
7. Expected vs actual behavior

### Getting Help

1. Check this testing guide
2. Review Gateway logs (Config → Console/Logs)
3. Verify Python installation
4. Check Ignition forums
5. Review Ignition SDK documentation

## Development Testing

### For Module Developers

**Build and Test Workflow:**
```bash
# 1. Clean Zone.Identifier files
find . -name "*Zone.Identifier*" -type f -delete

# 2. Build module
cd python3-integration
./gradlew clean build

# 3. Check for errors
ls -lh build/*.modl  # Should show .modl file

# 4. Test in Docker
cd ..
docker-compose up -d

# 5. Install and test module
# (Follow testing checklist above)

# 6. Review logs
docker-compose logs -f ignition

# 7. Cleanup
docker-compose down
```

**Version Management:**
```bash
# Edit version
nano python3-integration/version.properties

# Build new version
cd python3-integration
./gradlew clean build

# Commit
git add .
git commit -m "Release vX.X.X"
git push
```

## Continuous Integration

The module can be tested automatically using:
- Docker Compose for containerized testing
- Gradle for builds and checkstyle
- Shell scripts for automation

**CI Pipeline Example:**
1. Checkout code
2. Run `find . -name "*Zone.Identifier*" -type f -delete`
3. Run `./gradlew clean build`
4. Run `docker-compose up -d`
5. Wait for Gateway ready
6. Install module via API
7. Run test scripts
8. Collect results
9. Cleanup: `docker-compose down`

## Best Practices

### For Testing
1. Always test module installation from scratch
2. Verify module uninstall/reinstall works
3. Test with fresh Gateway (no cached data)
4. Check Gateway logs after every operation
5. Test in both Designer and Gateway contexts
6. Verify process pool health regularly
7. Test error conditions (bad code, timeouts, etc.)

### For Development
1. Increment version before every build
2. Delete Zone.Identifier files before builds
3. Run checkstyle before committing
4. Test locally before pushing
5. Document changes in commit messages
6. Keep CLAUDE.md updated with patterns

### For Production
1. Test in development environment first
2. Backup Gateway before module updates
3. Schedule updates during maintenance windows
4. Monitor Gateway logs after deployment
5. Have rollback plan ready
6. Test critical scripts after updates

---

**Document Version:** 1.0
**Last Updated:** 2025-10-14
**Module Version:** 1.1.1
