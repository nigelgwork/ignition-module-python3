# Python 3 Integration Module - Documentation Index

**Current Version**: v2.0.9
**Last Updated**: 2025-10-17

Quick reference to all module documentation.

---

## 📋 Essential Documents

### V2 Architecture (Current)
- **[V2_ARCHITECTURE_GUIDE.md](V2_ARCHITECTURE_GUIDE.md)** - v2.0.0+ modular architecture
- **[V2_STATUS_SUMMARY.md](V2_STATUS_SUMMARY.md)** - Current status and completion percentages
- **[V2_FEATURE_COMPARISON_AND_ROADMAP.md](V2_FEATURE_COMPARISON_AND_ROADMAP.md)** - v1 vs v2 comparison, roadmap
- **[V2_MIGRATION_GUIDE.md](V2_MIGRATION_GUIDE.md)** - Migrating from v1.x to v2.0+

### Development Workflow
- **[VERSION_UPDATE_WORKFLOW.md](VERSION_UPDATE_WORKFLOW.md)** - **MANDATORY** version release checklist
- **[TESTING_GUIDE.md](TESTING_GUIDE.md)** - How to test the module
- **[DEVELOPER_EXTENSION_GUIDE.md](DEVELOPER_EXTENSION_GUIDE.md)** - Extending the module

---

## 🏗️ Architecture & Design

### Core Architecture
- **[ARCHITECTURE.md](ARCHITECTURE.md)** - Gateway/Python bridge architecture
- **[EMBEDDING-PYTHON.md](EMBEDDING-PYTHON.md)** - Self-contained Python distribution
- **[EXAMPLES.md](EXAMPLES.md)** - Code examples and usage patterns

---

## 🔧 Build & Deployment

### Building
- **[IGNITION_MODULE_BUILD_GUIDE.md](IGNITION_MODULE_BUILD_GUIDE.md)** - Complete build instructions
- **[MODULE_BUILD_TROUBLESHOOTING.md](MODULE_BUILD_TROUBLESHOOTING.md)** - Common issues and fixes
- **[QUICK_START_TEMPLATE.md](QUICK_START_TEMPLATE.md)** - Module development template

### Deployment & Signing
- **[SIGNING.md](SIGNING.md)** - Module signing with certificates
- **[CERTIFICATE_TRUST_INSTRUCTIONS.md](CERTIFICATE_TRUST_INSTRUCTIONS.md)** - Trust self-signed certificates
- **[SELF-CONTAINED-SETUP.md](SELF-CONTAINED-SETUP.md)** - Self-contained Python setup
- **[CI_Setup_Prompts.md](CI_Setup_Prompts.md)** - GitHub Actions CI/CD setup

---

## 📊 Current Module Status (v2.0.9)

### ✅ Implemented Features
- Modern Designer IDE with VS Code-inspired dark theme
- Script management (save, load, delete, rename, organize in folders)
- Find/Replace toolbar
- Import/Export scripts to .py files
- Enhanced diagnostics panel with real-time metrics
- Theme support (Dark, Light, VS Code Dark+)
- REST API for remote execution
- Modular architecture (v2.0.0 refactor)

### 🎯 Next Priorities (Sprint Plan)
- Essential buttons (New, Delete, Rename - shortcut buttons)
- Save improvements (Ctrl+S, auto-save indicator)
- Keyboard shortcuts (Delete, Rename, F2)
- Font size controls
- Context menu (right-click operations)
- Move scripts between folders (drag-and-drop)
- Real-time syntax checking
- Basic auto-completion

---

## 📚 Key Technical Details

### REST API Endpoints (v2.0.9)
```
POST /data/python3integration/api/v1/exec              - Execute code
POST /data/python3integration/api/v1/eval              - Evaluate expression
POST /data/python3integration/api/v1/call-module       - Call Python function
GET  /data/python3integration/api/v1/version           - Python version
GET  /data/python3integration/api/v1/pool-stats        - Pool statistics
GET  /data/python3integration/api/v1/health            - Health check
GET  /data/python3integration/api/v1/diagnostics       - Performance metrics
GET  /data/python3integration/api/v1/scripts           - List scripts
POST /data/python3integration/api/v1/scripts/save      - Save script
GET  /data/python3integration/api/v1/scripts/{name}    - Load script
DELETE /data/python3integration/api/v1/scripts/{name}  - Delete script
```

### Module Structure (v2.0.0+)
```
designer/src/main/java/.../designer/
├── Python3IDE_v2.java          # Main IDE class (490 lines, was 2,676)
├── managers/                   # Business logic layer
│   ├── GatewayConnectionManager.java
│   ├── ScriptManager.java
│   └── ThemeManager.java
└── ui/                         # Presentation layer
    ├── EditorPanel.java
    ├── ScriptTreePanel.java
    ├── MetadataPanel.java
    └── DiagnosticsPanel.java
```

---

## 🔗 External Resources

- **Ignition SDK Documentation**: https://www.sdk-docs.inductiveautomation.com/
- **RSyntaxTextArea API**: https://bobbylight.github.io/RSyntaxTextArea/
- **Ignition Forum**: https://forum.inductiveautomation.com/

---

## 📞 Quick Reference

**For Users**:
- Open IDE: Tools → Python 3 IDE
- Execute code: Ctrl+Enter
- Save script: Ctrl+S
- Find text: Ctrl+F

**For Developers**:
- Build: `./gradlew clean build --no-daemon`
- Version: `python3-integration/version.properties`
- Main IDE: `designer/src/main/java/.../Python3IDE_v2.java`
- Gateway API: `gateway/src/main/java/.../Python3RestEndpoints.java`
- Version workflow: `docs/VERSION_UPDATE_WORKFLOW.md`

---

**Last Updated**: 2025-10-17 | **Version**: v2.0.9
