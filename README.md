# Python 3 Integration for Ignition

[![CI - Build and Security](https://github.com/nigelgwork/ignition-module-python3/actions/workflows/ci.yml/badge.svg)](https://github.com/nigelgwork/ignition-module-python3/actions/workflows/ci.yml)
[![Release](https://github.com/nigelgwork/ignition-module-python3/actions/workflows/release.yml/badge.svg)](https://github.com/nigelgwork/ignition-module-python3/actions/workflows/release.yml)

A production-ready Ignition module for Python 3 integration with comprehensive CI/CD, security scanning, and automated releases.

**Developed by Gaskony**

## Module Information

- **Module ID**: `com.gaskony.python3integration`
- **Current Version**: 1.4.0
- **Vendor**: Gaskony
- **Required Ignition**: 8.3.0+
- **Required Framework**: 8+

## Project Status: ⚙️ Active Development

**Latest Release:** v1.4.0 - Gateway-only with RPC infrastructure
**Next Release:** v1.5.0 - Designer scope support (in progress)

This is a **fully independent** module project with enterprise-grade CI/CD, security scanning, and comprehensive documentation.

## Features

- **Python 3 Integration**: Enables Python 3 scripting in Ignition via subprocess process pool
- **Gateway Scope**: Server-side Python 3 execution with JSON-based communication (✅ Working in v1.4.0)
- **Process Pool**: 3 concurrent Python executors with health monitoring
- **RPC Infrastructure**: Ready for Designer/Client support (⏸️ Pending v1.5.0)
- **Free Module**: No licensing restrictions

## What Works in v1.4.0

✅ **Gateway-Side Functions:**
- `system.python3.exec(code, variables)` - Execute Python code blocks
- `system.python3.eval(expression, variables)` - Evaluate Python expressions
- `system.python3.callModule(module, function, args)` - Call Python module functions
- `system.python3.isAvailable()` - Check Python availability
- `system.python3.getVersion()` - Get Python version info
- `system.python3.getPoolStats()` - Get process pool statistics
- `system.python3.example()` - Run test example

✅ **Use Cases:**
- Gateway Timer Scripts
- Gateway Event Scripts
- Tag Event Scripts
- WebDev scripting endpoints

❌ **Not Yet Working:**
- Designer Script Console (v1.5.0)
- Vision Client scripts (future)
- Perspective session scripts (future)

**See:** [STATUS_v1.4.0.md](STATUS_v1.4.0.md) for complete status and next steps.

## Repository Contents

This repository is **completely standalone** and includes:
- ✅ Complete SDK documentation (`docs/knowledge-base/`)
- ✅ Official Ignition SDK examples (`examples/`)
- ✅ CLAUDE.md with AI development guidance
- ✅ .gitignore for Ignition module development
- ✅ GitHub Actions CI/CD workflows
- ✅ Security scanning (Gitleaks, OWASP)
- ✅ Code quality checks (Checkstyle)
- ✅ Automated releases on version tags

## Goals

Define your Python 3 integration goals here:
- [ ] Python 3 runtime in Ignition
- [ ] Custom Python 3 scripting functions
- [ ] Gateway-side Python scripts
- [ ] Designer integration tools
- [ ] Python package management

## Installation

### From Built Module (v1.4.0)

**Module File:** `python3-integration/build/Python3Integration-1.4.0.modl`

1. Navigate to your Ignition Gateway: http://localhost:9088
2. Log in with admin credentials
3. Go to: **Config → System → Modules**
4. Click **"Install or Upgrade a Module"**
5. Select file: `Python3Integration-1.4.0.modl`
6. Click **Install**
7. Wait for Gateway to restart (~20 seconds)

### Verification

Test in Gateway Timer Script:
```python
# Check availability
available = system.python3.isAvailable()
print("Python 3 Available:", available)

# Run example
result = system.python3.example()
print(result)

# Get pool stats
stats = system.python3.getPoolStats()
print("Pool Stats:", stats)
```

Expected output:
```
Python 3 Available: True
Python 3 is working! 2^100 = 1.26765060023e+30
Pool Stats: {totalSize: 3, healthy: 3, available: 3, inUse: 0}
```

### From Source

See the [Building from Source](#building-from-source) section below.

## Prerequisites

### For Installation
- Ignition 8.3.0+ Gateway
- Python 3.x installed on the Gateway server

### For Development
- Java JDK 17
- Ignition 8.3+ Gateway (http://localhost:9088 for testing)
- Gradle 8.5+ (included via wrapper)
- Python 3.x development knowledge

## Getting Started

### 1. Review Complete SDK Documentation

This repository includes the complete Ignition SDK knowledge base:

```
docs/knowledge-base/
├── 00-CLAUDE-CODE-INSTRUCTIONS.md    # Claude Code workflows
├── 01-SDK-Overview-Getting-Started.md # SDK fundamentals
├── 02-Module-Architecture-Structure.md # Module scopes & lifecycle
├── 03-Build-Systems-Gradle-Maven.md   # Build configuration
├── 04-Perspective-Component-Development.md
├── 05-Vision-Component-Development.md
├── 06-OPC-UA-Device-Driver-Development.md
├── 07-Scripting-Functions-RPC-Communication.md
└── 08-Quick-Reference-Cheat-Sheet.md
```

**Start here:**
```bash
cat docs/knowledge-base/01-SDK-Overview-Getting-Started.md
cat docs/knowledge-base/02-Module-Architecture-Structure.md
```

### 2. Reference Official SDK Examples

This repository includes complete SDK examples:

```bash
ls -la examples/
# Contains official Ignition SDK examples from Inductive Automation
```

### 3. Choose Module Type

Decide what your module will provide:
- **Scripting Functions**: Add Python 3 functions to Ignition scripts
- **Gateway Hooks**: Background Python processes
- **Designer Tools**: Python-based Designer utilities
- **Component Development**: Custom components with Python backend

### 4. Create Module Structure

```bash
# Example structure
python3-integration/
├── build.gradle.kts
├── settings.gradle.kts
├── common/
│   └── src/main/java/
├── gateway/
│   └── src/main/java/
└── designer/
    └── src/main/java/
```

## Development Plan

1. **Design Phase**
   - Define module purpose
   - Plan architecture
   - Review Python 3 integration options

2. **Setup Phase**
   - Create Gradle project structure
   - Configure build files
   - Set up module.xml generation

3. **Implementation Phase**
   - Implement hook classes
   - Add Python 3 runtime integration
   - Create scripting functions or components

4. **Testing Phase**
   - Build module
   - Install in test Gateway
   - Verify functionality

## Resources

### Included in This Repository
- ✅ **SDK Documentation**: `docs/knowledge-base/` (Complete guides 00-08)
- ✅ **SDK Examples**: `examples/` (Official Ignition SDK examples)
- ✅ **AI Guidance**: `CLAUDE.md` (Instructions for Claude Code)
- ✅ **Git Ready**: `.gitignore` configured for Ignition development

### Module Development
- Create module structure (Gradle recommended)
- Add project-specific documentation as needed
- Build and test scripts will be generated by Gradle

## Building from Source

### Quick Build

```bash
cd python3-integration
./gradlew clean build
```

The built module will be in `python3-integration/build/Python3Integration-X.X.X.unsigned.modl`

### Build Commands

```bash
# Clean build
./gradlew clean build

# Build with security checks
./gradlew clean build dependencyCheckAnalyze

# Run code quality checks
./gradlew checkstyleMain checkstyleTest

# Check build output
ls -lh build/*.modl
```

### Version Management

Version is controlled in `python3-integration/version.properties`:

```properties
version.major=1
version.minor=4
version.patch=0
```

**Version History:**
- v1.4.0 (Current) - Gateway-only with RPC infrastructure
- v1.3.0 - Simplified Gateway-only architecture
- v1.2.x - Multi-scope with RPC (had Designer lockup issues)
- v1.1.x - Initial releases
- v1.0.0 - First version

Update these values to change the module version. The build automatically includes the version in the filename.

## CI/CD Pipeline

This project uses GitHub Actions for continuous integration and automated releases.

### Automated Workflows

#### CI Workflow (`.github/workflows/ci.yml`)

Runs on every push and pull request to `master`, `main`, or `develop`:

1. **Build and Test**
   - Builds module with Gradle
   - Uploads build artifacts
   - Verifies module file creation

2. **Security Scanning**
   - **Gitleaks**: Scans for secrets in code and commit history
   - **OWASP Dependency Check**: Analyzes dependencies for known vulnerabilities
   - Uploads security reports as artifacts

3. **Code Quality**
   - **Checkstyle**: Enforces Java coding standards
   - Uploads quality reports

4. **Module Verification**
   - Extracts and validates module.xml
   - Verifies module ID, vendor, and version

#### Release Workflow (`.github/workflows/release.yml`)

Triggered by version tags (e.g., `v1.0.0`) or manual dispatch:

1. Extracts version from tag
2. Updates `version.properties`
3. Builds release module
4. Creates GitHub Release with:
   - Module `.modl` file as asset
   - Auto-generated release notes
   - Installation instructions

### Creating a Release

```bash
# Update version in version.properties
cd python3-integration
# Edit version.properties with new version

# Commit the version change
git add version.properties
git commit -m "Bump version to 1.1.0"

# Create and push version tag
git tag v1.1.0
git push origin master --tags

# GitHub Actions will automatically:
# - Build the module
# - Run all security checks
# - Create a GitHub Release
# - Upload the .modl file
```

## Security

### Security Scanning

This project includes comprehensive security scanning:

- **Secret Detection**: Gitleaks scans for exposed credentials, API keys, and tokens
- **Dependency Vulnerabilities**: OWASP Dependency Check analyzes all dependencies
- **Code Quality**: Checkstyle enforces secure coding practices

### Security Reports

Security scan reports are available in CI artifacts:
- OWASP Dependency Check Report: `build/reports/dependency-check-report.html`
- Checkstyle Report: `build/reports/checkstyle/`

### Configuring Security Scans

**OWASP Suppressions**: Edit `python3-integration/config/owasp-suppressions.xml` to suppress false positives

**Checkstyle Rules**: Edit `python3-integration/config/checkstyle/checkstyle.xml` to customize code quality rules

### Security Best Practices

- ✅ No hardcoded secrets or credentials
- ✅ Automated secret scanning on every commit
- ✅ Dependency vulnerability scanning
- ✅ Code quality enforcement
- ✅ Comprehensive `.gitignore` for sensitive files

## Development Workflow

### Local Development

```bash
# 1. Clone repository
git clone https://github.com/nigelgwork/ignition-module-python3.git
cd ignition-module-python3/python3-integration

# 2. Make changes to source code
# Edit files in gateway/src/ or common/src/

# 3. Run security checks locally
./gradlew dependencyCheckAnalyze
./gradlew checkstyleMain checkstyleTest

# 4. Build module
./gradlew clean build

# 5. Test in Ignition Gateway
# Upload build/Python3Integration-X.X.X.unsigned.modl to Gateway at:
# http://localhost:9088/main/config/system/modules
```

### Contributing

1. Create a feature branch
2. Make your changes
3. Ensure all checks pass locally
4. Create a pull request
5. CI will automatically run all checks
6. Merge after approval

## Quick Commands

```bash
# Build module
cd python3-integration && ./gradlew clean build

# Run all checks
./gradlew clean build dependencyCheckAnalyze checkstyleMain checkstyleTest

# Check build output
ls -lh build/*.modl

# View module metadata
unzip -p build/*.modl module.xml
```

## Notes

This is a **completely standalone repository** with everything needed for Ignition module development:
- No external dependencies on other projects
- All SDK documentation included
- All examples included
- Ready for independent git repository
- Can be developed entirely on its own

---

## Quick Reference for v1.4.0

### Important Files
- **Status Document:** [STATUS_v1.4.0.md](STATUS_v1.4.0.md) - Complete status and next steps
- **Testing Guide:** [MANUAL_TESTING_GUIDE.md](MANUAL_TESTING_GUIDE.md) - Step-by-step testing
- **Test Results:** [TEST_RESULTS_v1.3.0.md](TEST_RESULTS_v1.3.0.md) - v1.3.0 validation
- **Module File:** `python3-integration/build/Python3Integration-1.4.0.modl`

### Quick Commands
```bash
# Build module
cd python3-integration && ./gradlew clean build

# Check Docker Gateway
docker ps --filter "name=ignition"

# View Gateway logs
docker logs claude-ignition-test 2>&1 | grep -i python | tail -20

# Copy module out
docker cp claude-ignition-test:/usr/local/bin/ignition/user-lib/modules/Python3Integration-1.4.0.modl ./
```

### Next Steps for v1.5.0
1. ✅ v1.4.0 built and ready to install
2. ⏸️ Manual installation via Gateway web UI required
3. ⏸️ Test Gateway functionality
4. ⏸️ Research Designer RPC API
5. ⏸️ Enable Designer scope
6. ⏸️ Build and test v1.5.0

### Getting Help
- Read: [STATUS_v1.4.0.md](STATUS_v1.4.0.md) for complete documentation
- Check: Gateway logs for module initialization
- Test: Via Gateway Timer Scripts (not Designer yet)

---

**Ready to continue development!** 🚀

See [STATUS_v1.4.0.md](STATUS_v1.4.0.md) for complete status and next steps.
