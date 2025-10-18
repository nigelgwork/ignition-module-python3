# Python 3 Integration for Ignition

**Current Version: v2.2.0** | [Full Documentation →](python3-integration/README.md)

A production-ready Ignition module that enables Python 3 scripting alongside Jython 2.7, with a modern Designer IDE.

---

## 🚀 Quick Start

```bash
cd python3-integration
./gradlew clean build --no-daemon
# Install build/libs/python3-integration-signed.modl in Ignition Gateway
```

**Key Features:**
- 🎨 Modern Designer IDE with dark theme
- 🏗️ Modular Architecture (v2.0.0+)
- 📊 Enhanced Diagnostics with real-time metrics
- ✨ Script Management - Save, load, organize in folders
- ⌨️ Keyboard Shortcuts - Ctrl+Enter, Ctrl+S, Ctrl+N, Ctrl+F
- 🖱️ Context Menus - Right-click scripts (Load, Export, Rename, Delete, Move)
- 🎯 Power User Features - Font controls, move to folder, drag-and-drop
- 🔄 REST API for remote execution and script autocomplete
- 🔒 Production Security - Script signing, CSRF protection

👉 **[See Full Documentation](python3-integration/README.md)** for features, API reference, and examples.

---

## 📖 Documentation

- **Module Documentation**: [python3-integration/README.md](python3-integration/README.md) - Complete user guide
- **Architecture**: [python3-integration/docs/V2_ARCHITECTURE_GUIDE.md](python3-integration/docs/V2_ARCHITECTURE_GUIDE.md)
- **Development Guide**: [CLAUDE.md](CLAUDE.md) - For contributors

---

## 🔧 For Developers

### Build & Test
```bash
# Build module
cd python3-integration && ./gradlew clean build --no-daemon

# Test with Docker
docker-compose up -d
# Access at http://localhost:9088
```

### Key Resources
- **Version Workflow**: [python3-integration/docs/VERSION_UPDATE_WORKFLOW.md](python3-integration/docs/VERSION_UPDATE_WORKFLOW.md)
- **Testing Guide**: [python3-integration/docs/TESTING_GUIDE.md](python3-integration/docs/TESTING_GUIDE.md)
- **Roadmap**: [python3-integration/docs/V2_FEATURE_COMPARISON_AND_ROADMAP.md](python3-integration/docs/V2_FEATURE_COMPARISON_AND_ROADMAP.md)

---

## 📚 External Resources

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
