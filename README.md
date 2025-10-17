# Python 3 Integration for Ignition

**Current Version: v2.0.9** | [Module README](python3-integration/README.md) | [Architecture Docs](python3-integration/docs/)

---

## 📦 What's in this Repository

This repository contains three main resources for Ignition module development:

### 1. **Python 3 Integration Module** (v2.0.9) ⭐

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

### 2. **Ignition SDK Knowledge Base** 📚

Comprehensive guides for developing Ignition 8.3+ modules.

👉 **[Browse SDK Documentation](docs/README.md)**

**Topics Covered:**
- **00** - Claude Code Instructions (AI development workflows)
- **01** - SDK Overview & Getting Started
- **02** - Module Architecture & Structure
- **03** - Build Systems (Gradle/Maven)
- **04** - Perspective Component Development
- **05** - Vision Component Development
- **06** - OPC-UA Device Driver Development
- **07** - Scripting Functions & RPC Communication
- **08** - Quick Reference & Cheat Sheet

**Best for:** Learning module development concepts, understanding SDK architecture, troubleshooting module issues.

---

### 3. **Official SDK Examples** 🔨

17 example modules from Inductive Automation demonstrating various module types.

👉 **[Explore SDK Examples](examples/README.md)**

**Example Types:**
- Perspective Components (minimal & full)
- Vision Components
- Scripting Functions & RPC
- OPC-UA Device Drivers
- Gateway Network Functions
- Report Components & Datasources
- Event Stream Handlers
- And more...

**Best for:** Reference implementations, copying patterns, understanding SDK APIs in practice.

---

## 🚀 Latest Release: v2.0.9 (UX Fixes)

**New in v2.0.9:**
- ✅ **Fixed Scrollbars** - Only appear when needed (AS_NEEDED policy)
- ✅ **Fixed Theme Selector** - No more text cutoff (150px width)
- ✅ **Larger Description Panel** - 50% increase for better usability
- ✅ **Python Version Display** - Shows actual version instead of "Unknown"
- ✅ **Dark Theme Dividers** - All panel dividers match dark theme
- ✅ **Dark Theme Dialogs** - All popups follow consistent dark theme

[View Full Changelog](python3-integration/README.md#changelog)

---

## 📚 Documentation Structure

```
/
├── python3-integration/       # THE WORKING MODULE ⭐
│   ├── README.md             # Comprehensive module documentation
│   ├── docs/                 # Module-specific guides
│   │   ├── V2_ARCHITECTURE_GUIDE.md
│   │   ├── V2_STATUS_SUMMARY.md
│   │   ├── V2_FEATURE_COMPARISON_AND_ROADMAP.md
│   │   ├── TESTING_GUIDE.md
│   │   └── VERSION_UPDATE_WORKFLOW.md
│   ├── common/               # Common scope (shared code)
│   ├── gateway/              # Gateway scope (Python bridge, REST API)
│   ├── designer/             # Designer scope (Python 3 IDE)
│   └── build.gradle.kts      # Module build configuration
│
├── docs/                     # SDK KNOWLEDGE BASE 📚
│   ├── 00-CLAUDE-CODE-INSTRUCTIONS.md
│   ├── 01-SDK-Overview-Getting-Started.md
│   ├── 02-Module-Architecture-Structure.md
│   ├── 03-Build-Systems-Gradle-Maven.md
│   ├── 04-Perspective-Component-Development.md
│   ├── 05-Vision-Component-Development.md
│   ├── 06-OPC-UA-Device-Driver-Development.md
│   ├── 07-Scripting-Functions-RPC-Communication.md
│   └── 08-Quick-Reference-Cheat-Sheet.md
│
├── examples/                 # SDK EXAMPLES 🔨
│   ├── perspective-component/
│   ├── scripting-function/
│   ├── opc-ua-device/
│   └── ... (14 more examples)
│
├── scripts/                  # Testing utilities
├── archive/                  # Historical v1.x planning docs
├── CLAUDE.md                # Development guidance for AI
└── UPGRADE_GUIDE.md         # Upgrade instructions
```

### 📖 Understanding the Two Docs Folders

**Note:** This repository has TWO separate documentation directories with different purposes:

#### [`/python3-integration/docs/`](python3-integration/docs/) - **Module-Specific** ⭐
Documentation for THIS specific module (Python 3 Integration v2.0.9):
- V2 Architecture Guide
- Version Update Workflow
- Testing Guide
- Feature Comparison & Roadmap

**Use this for:** Working with the Python 3 Integration module specifically.

#### [`/docs/`](docs/) - **General SDK Learning** 📚
General educational guides for ANY Ignition 8.3+ module development:
- SDK Overview & Getting Started
- Module Architecture & Structure
- Build Systems (Gradle/Maven)
- Component Development

**Use this for:** Learning Ignition SDK development concepts in general.

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

### For SDK Learning
- **Getting Started**: [docs/01-SDK-Overview-Getting-Started.md](docs/01-SDK-Overview-Getting-Started.md)
- **Module Architecture**: [docs/02-Module-Architecture-Structure.md](docs/02-Module-Architecture-Structure.md)
- **Scripting Functions**: [docs/07-Scripting-Functions-RPC-Communication.md](docs/07-Scripting-Functions-RPC-Communication.md)

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

**SDK Documentation** compiled from official Ignition SDK resources and community knowledge.

**SDK Examples** provided by Inductive Automation.

---

## 📄 License

See individual module source files for licensing information.
