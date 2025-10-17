# Python 3 Integration for Ignition

**Current Version: v2.0.22** | [Module README](python3-integration/README.md) | [Architecture Docs](python3-integration/docs/)

---

## 📦 What's in this Repository

This repository contains a production-ready **Python 3 Integration module** for Ignition 8.3+.

### **Python 3 Integration Module** (v2.0.22) ⭐

A production-ready Ignition module with Designer IDE for Python 3 development.

👉 **[Go to Module Documentation](python3-integration/README.md)**

**Quick Start:**
```bash
cd python3-integration
./gradlew clean build --no-daemon
# Install build/libs/python3-integration-signed.modl in Ignition Gateway
```

**Key Features:**
- 🎨 Modern Designer IDE with VS Code-inspired dark theme
- 🏗️ Modular Architecture (v2.0.0+) - Clean separation of concerns
- 📊 Enhanced Diagnostics with real-time metrics
- ✨ Script Management - Save, load, organize in folders
- 🔄 REST API for remote execution
- 🔒 Production Security - Script signing, CSRF protection
- 📦 Self-contained Python 3.11, no system install needed

---

## 🚀 Latest Release: v2.0.22 (Theme System Completion)

**New in v2.0.22:**
- ✅ **Split Pane Divider Theming** - All dividers now respect current theme (dark/light)
- ✅ **Fixed Light Theme** - Split pane dividers properly display in light mode
- ✅ **Consistent Theme Colors** - Dividers use ModernTheme constants
- ✅ **Dynamic Theme Switching** - Dividers update when switching themes

**Recent Improvements (v2.0.17-v2.0.21):**
- ✅ **Python Version Detection** - Real-time version display from Gateway
- ✅ **Diagnostics Panel** - Manual refresh, no log spam
- ✅ **Context Menu Fix** - Proper text visibility in dark theme
- ✅ **Execution Logging** - Comprehensive INFO-level logs
- ✅ **Security Mode Fix** - Changed default from RESTRICTED to ADMIN for usability

[View Full Changelog](python3-integration/README.md#changelog)

---

## 🔧 Quick Links

### For Module Users
- **Installation Guide**: [python3-integration/README.md](python3-integration/README.md#quick-start)
- **API Reference**: [python3-integration/README.md](python3-integration/README.md#api-reference)
- **Troubleshooting**: [python3-integration/README.md](python3-integration/README.md#troubleshooting)
- **Upgrade Guide**: [UPGRADE_GUIDE.md](UPGRADE_GUIDE.md)

### For Module Developers
- **Architecture Guide**: [python3-integration/docs/V2_ARCHITECTURE_GUIDE.md](python3-integration/docs/V2_ARCHITECTURE_GUIDE.md)
- **Testing Guide**: [python3-integration/docs/TESTING_GUIDE.md](python3-integration/docs/TESTING_GUIDE.md)
- **Development Guide**: [CLAUDE.md](CLAUDE.md)
- **Version Workflow**: [python3-integration/docs/VERSION_UPDATE_WORKFLOW.md](python3-integration/docs/VERSION_UPDATE_WORKFLOW.md)

### For Ignition SDK Learning
- **Official SDK Docs**: https://www.sdk-docs.inductiveautomation.com/
- **SDK Examples Repository**: https://github.com/inductiveautomation/ignition-sdk-examples
- **Module Development Forum**: https://forum.inductiveautomation.com/c/module-development/7

---

## 🏗️ Development

### Build the Module

```bash
cd python3-integration
./gradlew clean build --no-daemon
```

**Output:** `build/libs/python3-integration-signed.modl`

### Test the Module

```bash
# Start Docker test environment
docker-compose up -d

# View logs
docker logs -f ignition-python3-test

# Install module at http://localhost:9088
```

### Version Management

See [VERSION_UPDATE_WORKFLOW.md](python3-integration/docs/VERSION_UPDATE_WORKFLOW.md) for complete version release checklist.

---

## 📖 External Resources

- **Official SDK Docs**: https://www.sdk-docs.inductiveautomation.com/
- **SDK Examples**: https://github.com/inductiveautomation/ignition-sdk-examples
- **Forum**: https://forum.inductiveautomation.com/c/module-development/7
- **Gradle Plugin**: https://github.com/inductiveautomation/ignition-module-tools

---

## 📜 Credits

**Python 3 Integration Module** developed by Gaskony with assistance from Claude Code (Anthropic).

Built using the Ignition 8.3 SDK from Inductive Automation.

---

## 📄 License

See individual module source files for licensing information.
