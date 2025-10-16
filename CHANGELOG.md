# Changelog

All notable changes to the Python 3 Integration module are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
