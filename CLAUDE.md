# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## ⚠️ CRITICAL: File Cleanup Rules

**ALWAYS DELETE Zone.Identifier files immediately:**
- **NEVER** ignore or skip Zone.Identifier files
- **ALWAYS** delete them using: `find . -name "*Zone.Identifier*" -type f -delete`
- Run this check at the start of ANY code cleanup, documentation update, or file organization task
- These are Windows WSL metadata files that should never be committed

**Remember:** DELETE, not ignore!

## ⚠️ CRITICAL: Version Management and Build Process

**ALWAYS follow this complete workflow for EVERY build:**

### 1. Pre-Build Cleanup
- Delete Zone.Identifier files: `find . -name "*Zone.Identifier*" -type f -delete`
- Review and tidy up code (remove commented code, fix formatting)
- Update documentation if needed (README, TESTING_GUIDE, etc.)

### 2. Version Increment
Version file: `python3-integration/version.properties`

**Current Version: v2.0.22** (December 2024)

**Versioning Rules:**
- **MAJOR** (x.0.0): Breaking changes, major new features, architectural changes
- **MINOR** (1.x.0): New features, significant fixes, scope changes, API additions
- **PATCH** (1.0.x): Bug fixes, documentation updates, minor tweaks

**Examples:**
- Added new feature (folders, find/replace) → **MINOR** (2.0.9 → 2.1.0)
- Fixed UI bugs (scrollbars, themes) → **PATCH** (2.0.8 → 2.0.9)
- Rewrote entire architecture (v1 → v2 refactor) → **MAJOR** (1.17.2 → 2.0.0)

**Version Locations to Update:**
When incrementing version, update ALL of these files:
- [ ] `python3-integration/version.properties` - Primary version source (REQUIRED)
- [ ] `python3-integration/designer/src/main/java/.../DesignerHook.java` - Fallback version (line 183)
- [ ] `README.md` (repository root) - Main README version references (lines 3, 11, 76, 135)
- [ ] `python3-integration/README.md` - Module README version references (lines 3, 136, 471, 514) + Changelog entry
- [ ] `CLAUDE.md` - Current version references (lines 27, 112, 121, 551, 566)
- [ ] Update Changelog sections in both README files

**Release Checklist:**
- [ ] All tests passing (`./gradlew clean build`)
- [ ] Zone.Identifier files deleted
- [ ] Code cleanup complete (no commented code, proper formatting)
- [ ] Documentation updated (README, version references)
- [ ] Version bumped in all locations above
- [ ] Git commit with proper format
- [ ] Git push to GitHub
- [ ] Build artifacts verified (*.modl file in build/libs/)

**Version History:**
- v2.0.15 (Dec 2024) - Complete theme system fixes, Python version detection rebuild
- v2.0.14 (Dec 2024) - Theme refinements, file chooser consistency, enhanced logging
- v2.0.13 (Dec 2024) - Code consolidation (removed v2, renamed v1_9 to canonical)
- v2.0.12 (Dec 2024) - Theme-aware dialogs (DarkDialog)
- v2.0.0-2.0.11 - Various improvements and refactoring
- v1.17.2 (2024) - Last v1.x release before v2.0.0 refactor

See git commit history for complete changelog.

### 3. Build Module
```bash
cd /modules/ignition-module-python3/python3-integration
./gradlew clean build --no-daemon
```

### 4. Git Commit and Push
**ALWAYS commit and push after successful build:**
```bash
git add -A
git commit -m "Release vX.Y.Z - [description]

Version: X.Y.Z-1 → X.Y.Z (MAJOR/MINOR/PATCH)

Changes:
- [List key changes]
- [...]

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>"
git push
```

### Complete Build Workflow Summary
1. **Clean**: Delete Zone.Identifier files
2. **Tidy**: Code cleanup and documentation updates
3. **Version**: Increment version.properties
4. **Update READMEs**: Update BOTH README.md files with new version and changelog
   - `/README.md` (repository root) - Update version references and latest release section
   - `/python3-integration/README.md` (module) - Update version references and add changelog entry
5. **Build**: Run ./gradlew clean build
6. **Commit**: Add all changes (code + READMEs + version.properties)
7. **Push**: Push to GitHub

**Remember:** NEVER build without incrementing version, updating BOTH READMEs, and pushing to git!

## ⚠️ CRITICAL: Pre-Push Requirements (User Request)

**BEFORE EVERY PUSH TO GITHUB, YOU MUST:**

1. **Update ALL Documentation** (ESPECIALLY README FILES):
   - `/README.md` (repository root) - Update version (lines 3, 11, 76, 135) and latest release section
   - `/python3-integration/README.md` (module) - Update version (lines 3, 136, 471, 514) and add changelog entry
   - ARCHITECTURE.md - update if architecture changed
   - CHANGELOG.md - add release notes if exists
   - Any other .md files that reference version numbers or features
   - Version references in comments and docstrings

2. **Clean Working Folder**:
   - Delete Zone.Identifier files: `find . -name "*Zone.Identifier*" -type f -delete`
   - Remove commented-out code
   - Remove temporary files
   - Fix formatting issues
   - Remove debug statements

3. **Verify Git Status**:
   - Run `git status` to check for untracked or modified files
   - Ensure no unexpected files are being committed
   - Review changes with `git diff`

**This is a MANDATORY workflow requirement requested by the user. Do NOT skip these steps.**

## Repository Purpose

This is a **Python 3 Integration module** for Ignition 8.3 SDK. The repository contains both:
1. **Active module implementation** (`python3-integration/` directory) - a working Python 3 integration module
2. **SDK documentation and examples** for reference

## Repository Structure

**Current Version: v2.0.22** (December 2024)

```
ignition-module-python3/
├── README.md                        # Repository landing page (update with each release)
├── CLAUDE.md                        # This file - AI guidance
├── UPGRADE_GUIDE.md                 # Upgrade instructions
├── .gitignore                       # Git ignore rules
│
├── python3-integration/             # ⭐ WORKING MODULE (v2.0.22)
│   ├── build.gradle.kts            # Root build configuration
│   ├── settings.gradle.kts         # Gradle settings
│   ├── version.properties          # Current version: 2.0.22
│   ├── README.md                   # Module documentation (comprehensive)
│   │
│   ├── common/                     # Common scope (shared code)
│   ├── gateway/                    # Gateway scope (Python bridge, REST API)
│   │   ├── build.gradle.kts
│   │   └── src/main/java/.../gateway/
│   │       ├── GatewayHook.java
│   │       ├── Python3ProcessPool.java
│   │       ├── Python3Executor.java
│   │       ├── Python3ScriptModule.java
│   │       ├── Python3RestEndpoints.java
│   │       └── resources/python_bridge.py
│   │
│   ├── designer/                   # Designer scope (Python 3 IDE - v2.0.0+)
│   │   ├── build.gradle.kts
│   │   └── src/main/java/.../designer/
│   │       ├── DesignerHook.java
│   │       ├── Python3IDE_v2.java         # Main IDE class (refactored v2.0.0)
│   │       ├── managers/                  # Business logic layer
│   │       │   ├── GatewayConnectionManager.java
│   │       │   ├── ScriptManager.java
│   │       │   └── ThemeManager.java
│   │       └── ui/                        # Presentation layer
│   │           ├── EditorPanel.java
│   │           ├── ScriptTreePanel.java
│   │           ├── MetadataPanel.java
│   │           └── DiagnosticsPanel.java
│   │
│   └── docs/                        # Module-specific documentation
│       ├── V2_ARCHITECTURE_GUIDE.md
│       ├── V2_STATUS_SUMMARY.md
│       ├── V2_FEATURE_COMPARISON_AND_ROADMAP.md
│       ├── V2_MIGRATION_GUIDE.md
│       ├── TESTING_GUIDE.md
│       └── VERSION_UPDATE_WORKFLOW.md
│
├── docs/                            # 📚 SDK KNOWLEDGE BASE
│   ├── README.md                    # SDK docs index
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
├── examples/                        # 🔨 SDK EXAMPLES (17 modules)
│   ├── README.md                    # Examples index
│   ├── scripting-function/          # Most relevant for this module
│   ├── perspective-component/
│   ├── opc-ua-device/
│   └── ... (14 more examples)
│
├── scripts/                         # Testing utilities
│   ├── TESTING.md
│   └── MANUAL_TESTING_GUIDE.md
│
├── archive/                         # Historical v1.x planning docs
│   └── v1-planning-docs/
│       ├── CHANGELOG.md
│       ├── ROADMAP_STATUS.md
│       ├── REFACTORING_PLAN.md
│       └── ... (stale v1.x documentation)
│
├── Dockerfile.test                  # Docker test environment
├── docker-compose.yml               # Test infrastructure
└── config/                          # Test configuration
```

**Note:** Repository reorganized Oct 2024 to separate:
1. Working module (python3-integration/ - v2.0.22)
2. SDK learning resources (docs/, examples/)
3. Historical planning docs (archive/)

## Working with the Active Module

The `python3-integration/` directory contains a complete, working module implementation. Key aspects:

### Architecture Overview

The module uses a **subprocess process pool** approach to bridge Ignition's Jython 2.7 with Python 3:

**Gateway Scope:**
1. **GatewayHook** - Module lifecycle, initializes process pool during startup()
2. **Python3ProcessPool** - Manages 3-20 warm Python processes, thread-safe borrowing/returning
3. **Python3Executor** - Wraps single Python subprocess, handles JSON communication via stdin/stdout
4. **Python3ScriptModule** - Exposes scripting functions like `system.python3.exec()`, `system.python3.eval()`
5. **Python3RestEndpoints** - REST API for remote execution (v1.6.0+, enhanced v2.0.0+)
6. **python_bridge.py** - Python-side request handler running in each subprocess

**Designer Scope (v2.0.0+):**
1. **Python3IDE_v2.java** - Main IDE orchestration class (refactored v2.0.0)
2. **Managers/** - Business logic (GatewayConnectionManager, ScriptManager, ThemeManager)
3. **UI/** - Presentation layer (EditorPanel, ScriptTreePanel, MetadataPanel, DiagnosticsPanel)

See `python3-integration/docs/V2_ARCHITECTURE_GUIDE.md` for detailed component interactions and data flow.

### Build Commands

```bash
# Build the module (from python3-integration/ directory)
cd python3-integration
./gradlew clean build

# Output location
ls -lh build/libs/*.modl

# Build from repository root
cd /modules/ignition-module-python3
./gradlew -p python3-integration clean build
```

### Testing the Module

```bash
# Install in local Ignition Gateway
# 1. Navigate to http://localhost:8088
# 2. Config → System → Modules → Install or Upgrade a Module
# 3. Upload: python3-integration/build/libs/python3-integration-signed.modl

# Test in Script Console (once installed)
# system.python3.example()
# system.python3.getVersion()
# system.python3.getPoolStats()

# Test Designer IDE (v2.0.0+)
# Open Designer → Tools → Python 3 IDE
# Connect to Gateway, write Python 3 code, click Execute
```

### Key Implementation Files

When modifying module functionality, focus on these files:

- **GatewayHook.java** - Module lifecycle (setup, startup, shutdown), REST API mounting
- **Python3ProcessPool.java** - Pool management, health checking, borrow/return logic
- **Python3Executor.java** - Single process communication, timeout handling
- **Python3ScriptModule.java** - Scripting function definitions with @ScriptFunction annotations
- **Python3RestEndpoints.java** - REST API endpoints (Ignition 8.3 OpenAPI compliant)
- **python_bridge.py** - Python-side command processing (execute, evaluate, call_module)

### SDK Documentation Reference

The complete Ignition SDK documentation is in `docs/knowledge-base/`:

1. **00-CLAUDE-CODE-INSTRUCTIONS.md** - Specific workflows for Claude Code
2. **01-SDK-Overview-Getting-Started.md** - SDK fundamentals, prerequisites
3. **02-Module-Architecture-Structure.md** - Module scopes, hooks, lifecycle
4. **07-Scripting-Functions-RPC-Communication.md** - Most relevant for this module

### Official Examples Reference

The `examples/` directory contains reference implementations:

```bash
# Most relevant examples for this module
ls -la examples/scripting-function/        # Scripting function implementation pattern
ls -la examples/perspective-component-minimal/  # Gradle build configuration reference
```

## Module Development Patterns

### Module Lifecycle Critical Phases

Every GatewayHook goes through three phases (see `GatewayHook.java` for implementation):

1. **setup(GatewayContext)** - Early initialization
   - Load configuration (system properties, environment variables)
   - Register extension points
   - **DO NOT** start threads or access database
   - Current module: Loads Python path configuration, initializes logger

2. **startup(LicenseState)** - Main initialization
   - Platform services now available
   - Start background threads, initialize process pools
   - Register script managers
   - Current module: Creates Python3ProcessPool, registers Python3ScriptModule

3. **shutdown()** - Clean shutdown
   - Stop all threads, close connections
   - Release resources to prevent memory leaks
   - Current module: Shuts down process pool, terminates all Python subprocesses

### Scripting Function Registration

To expose functions to Ignition scripts (pattern from `Python3ScriptModule.java`):

```java
@ScriptFunction(docBundlePrefix = "Python3ScriptModule")
public Object exec(String code, @KeywordArgs Map<String, Object> variables) {
    // Implementation
}
```

Then register in GatewayHook.startup():
```java
context.getScriptManager().addScriptModule(
    "system.python3",
    new Python3ScriptModule(processPool),
    new ScriptModuleDocProvider()
);
```

Documentation properties file: `src/main/resources/Python3ScriptModule.properties`

### Build Configuration

This module uses Gradle with the Ignition SDK plugin (`io.ia.sdk.modl`):

- **Root build.gradle.kts**: Defines module metadata, scopes, hooks
- **Scope build.gradle.kts**: Dependencies for each scope (common, gateway, designer)
- **settings.gradle.kts**: Declares subprojects

Key configuration in root `build.gradle.kts`:
```kotlin
ignitionModule {
    projectScopes.putAll(mapOf(
        ":gateway" to "G",      // Gateway scope
        ":common" to "GC"       // Common scope (Gateway + Client)
    ))

    hooks.putAll(mapOf(
        "com.inductiveautomation.ignition.examples.python3.gateway.GatewayHook" to "G"
    ))
}
```

## Thread Safety and Concurrency

The module handles concurrent script execution through process pooling:

### Process Pool Pattern

```java
// From Python3ProcessPool.java
private final BlockingQueue<Python3Executor> availableExecutors;

// Borrow (blocks if pool exhausted)
Python3Executor executor = pool.borrowExecutor(30, TimeUnit.SECONDS);

// Execute with borrowed executor
try {
    result = executor.execute(code, variables);
} finally {
    pool.returnExecutor(executor);  // CRITICAL: Always return
}
```

### Concurrency Constraints

- Pool size (default: 3) = max concurrent Python executions
- Each executor handles one request at a time (synchronized)
- Requests beyond pool size wait up to 30 seconds
- Health checker runs every 30 seconds (separate thread)

### Thread-Safe Patterns Used

1. **BlockingQueue** for executor availability
2. **synchronized** blocks in Python3Executor for command execution
3. **AtomicInteger** for pool statistics
4. **ExecutorService** for health checking

## Configuration System

Configuration is loaded in GatewayHook.setup() via system properties and environment variables:

### Python Path Detection (Priority Order)

1. System property: `-Dignition.python3.path=/path/to/python3`
2. Environment variable: `IGNITION_PYTHON3_PATH`
3. Auto-detection (OS-specific paths in GatewayHook.java)
4. Fallback: `python3`

### Pool Size Configuration

System property: `-Dignition.python3.poolsize=5` (default: 3)

To add to Ignition, edit `ignition.conf`:
```properties
wrapper.java.additional.100=-Dignition.allowunsignedmodules=true
wrapper.java.additional.101=-Dignition.python3.path=/usr/bin/python3.11
wrapper.java.additional.102=-Dignition.python3.poolsize=5
```

## Subprocess Communication Protocol

The module uses **line-based JSON** protocol between Java and Python:

### Request Format (Java → Python via stdin)

```json
{"command": "execute", "code": "result = 2 + 2", "variables": {}}
{"command": "evaluate", "expression": "x + y", "variables": {"x": 10, "y": 20}}
{"command": "call_module", "module": "math", "function": "sqrt", "args": [16]}
```

### Response Format (Python → Java via stdout)

```json
{"success": true, "result": 4}
{"success": false, "error": "NameError: name 'x' is not defined", "traceback": "..."}
```

### Critical Implementation Details

1. **Line-based protocol**: Each request/response is a single line (no pretty-printing)
2. **Unbuffered I/O**: Python started with `-u` flag
3. **Timeout handling**: Java reads with 30s timeout
4. **stderr ignored**: Only stdout used for responses (stderr logged separately)

See `Python3Executor.java` and `python_bridge.py` for full protocol implementation.

## Common Development Tasks

### Adding a New Scripting Function

1. Add method to `Python3ScriptModule.java` with `@ScriptFunction` annotation
2. Add documentation to `Python3ScriptModule.properties`
3. Rebuild: `./gradlew -p python3-integration clean build`
4. Reinstall module in Gateway
5. Test in Script Console

### Extending Python Bridge

1. Add new command handler to `python_bridge.py`:
   ```python
   def process_request(self, request):
       if request['command'] == 'my_command':
           return self.handle_my_command(request)
   ```
2. Add Java method to `Python3Executor.java` to send the new command
3. Update `Python3ScriptModule.java` to expose to scripts

### Debugging

**Gateway logs location**: `<ignition-install>/logs/wrapper.log`

**Check module status**:
```bash
tail -f wrapper.log | grep Python3
```

**Common issues**:
- "Python process is not alive" → Check Python path, verify `python3 --version` works
- "Timeout waiting for executor" → Pool exhausted, increase pool size
- "Failed to parse response" → Check python_bridge.py for print statements (breaks JSON protocol)

## Module Package Structure

Current module uses:
- **Module ID**: `com.inductiveautomation.ignition.examples.python3`
- **Package**: `com.inductiveautomation.ignition.examples.python3.gateway`
- **Hook**: `com.inductiveautomation.ignition.examples.python3.gateway.GatewayHook`

Follows reverse domain notation (Inductive Automation convention for examples).

## Resource Files

Resources in `src/main/resources/` are bundled into the .modl file:

- **python_bridge.py**: Extracted to temp file at runtime, executed by subprocess
- **Python3ScriptModule.properties**: Scripting function documentation

Access at runtime:
```java
InputStream is = getClass().getResourceAsStream("/python_bridge.py");
```

## Critical Best Practices

### Subprocess Management

- **Always** terminate processes in shutdown() to prevent orphaned processes
- **Never** use process.waitFor() without timeout (can hang forever)
- **Always** return executors to pool in finally blocks
- **Monitor** process health continuously (already implemented)

### JSON Communication

- **Never** use print() in python_bridge.py (breaks protocol)
- **Always** use single-line JSON (no newlines in JSON strings)
- **Handle** serialization failures gracefully (complex objects → str)

### Ignition Module Development

- **Use** SLF4J logger, never System.out.println
- **Test** module install/uninstall cycles (check for memory leaks)
- **Version** carefully: SNAPSHOT vs release versions
- **Document** configuration properties

### Certificate Management

**Decision: Certificates are KEPT in git** (not regenerated each time)

**Rationale:**
1. **CI/CD Compatibility** - GitHub Actions workflows work out-of-box without extra setup
2. **Consistency** - Same certificate signature across all environments (dev, test, prod)
3. **Reproducible Builds** - Same inputs produce same outputs, verifiable
4. **Simplicity** - Clone and build works immediately, no extra steps required
5. **Minimal Size** - Only 4.9KB total (certificate.der + keystore.jks + gaskony-cert.pem)

**Files in Repository:**
- `python3-integration/certificate.der` (883 bytes) - Public certificate
- `python3-integration/keystore.jks` (2.7KB) - Private keystore
- `python3-integration/gaskony-cert.pem` (1.3KB) - PEM format certificate
- `python3-integration/sign.props` - Signing configuration with development passwords

**Security Notes:**
- These are **development-only self-signed certificates**
- Passwords (`gaskony2024`) are public in repository
- Intended for testing and development environments only
- For production distribution, generate new certificates with private keys

**Alternative (Not Recommended):**
- Regenerating certificates each build creates inconsistency
- Different environments get different signatures
- CI/CD breaks without extra configuration
- Script (`generate-signing-certs.sh`) creates `gradle.properties` but build uses `sign.props` (mismatch)
- No practical security benefit for open-source development module

**Current Approach is Correct** for this open-source development project.

## Repository Resources

- **Active module code**: `python3-integration/` (v2.0.22)
- **V2 Architecture Guide**: `python3-integration/docs/V2_ARCHITECTURE_GUIDE.md` ⭐
- **V2 Status Summary**: `python3-integration/docs/V2_STATUS_SUMMARY.md`
- **V2 Feature Comparison**: `python3-integration/docs/V2_FEATURE_COMPARISON_AND_ROADMAP.md`
- **Testing Guide**: `python3-integration/docs/TESTING_GUIDE.md`
- **Version Workflow**: `python3-integration/docs/VERSION_UPDATE_WORKFLOW.md`
- **SDK Documentation**: `docs/` (00-08 guides for Ignition SDK)
- **SDK Examples**: `examples/scripting-function/` (most similar pattern)

## Python 3 IDE (v2.0.0+ - IMPLEMENTED)

**STATUS**: ✅ Fully implemented and refactored in v2.0.0 (Oct 2024)

The Python 3 IDE is a **Designer-scoped feature** that provides an IDE-type interface for Python 3 development:

### Implemented Features (v2.0.22)

**Core IDE:**
- ✅ Code editor with Python syntax highlighting (RSyntaxTextArea)
- ✅ Gateway execution via REST API (non-blocking, async)
- ✅ Separate output/error tabs with color coding
- ✅ Execution timing and performance metrics
- ✅ Connection management (multi-Gateway support)

**Script Management:**
- ✅ Save scripts with names and descriptions
- ✅ Load scripts from tree browser
- ✅ Delete scripts with confirmation
- ✅ Rename scripts (v2.0.5)
- ✅ Folder organization (create, rename folders - v2.0.5)
- ✅ Import/Export scripts to .py files (v2.0.7)

**Advanced Features:**
- ✅ Find/Replace toolbar (v2.0.6)
- ✅ Enhanced diagnostics panel (v2.0.8)
- ✅ Theme support (Dark, Light, VS Code Dark+ - v1.11.0+)
- ✅ Real-time Python version detection (v2.0.9)
- ✅ Modular architecture (v2.0.0 refactor)

**v2.0.0 Refactoring:**
- Reduced main class from 2,676 lines → 490 lines (82% reduction)
- Separated concerns: Managers (business logic) + UI Panels (presentation)
- Improved maintainability: 95-490 lines per file (vs 25K tokens before)

### Architecture (v2.0.0+)

**Main Class:**
- `Python3IDE_v2.java` - Orchestration, menu registration, panel assembly

**Managers (Business Logic):**
- `GatewayConnectionManager.java` - Gateway URL management, REST client lifecycle
- `ScriptManager.java` - Script CRUD operations, file I/O
- `ThemeManager.java` - Theme application, RSyntaxTextArea styling

**UI Panels (Presentation):**
- `EditorPanel.java` - Code editor, Execute button, output/error display
- `ScriptTreePanel.java` - Script browser tree, right-click menu
- `MetadataPanel.java` - Script name, description, save button
- `DiagnosticsPanel.java` - Execution time, pool stats, health indicators

### Documentation

- **Full Architecture**: `python3-integration/docs/V2_ARCHITECTURE_GUIDE.md`
- **Feature Comparison**: `python3-integration/docs/V2_FEATURE_COMPARISON_AND_ROADMAP.md`
- **Status Summary**: `python3-integration/docs/V2_STATUS_SUMMARY.md`

### Historical Context

The original IDE plan (`python3-integration/docs/PYTHON_IDE_PLAN.md`) outlined v1.7.0-v1.8.0 implementation phases. This has been fully implemented and later refactored to v2.0.0 architecture for better maintainability.

## REST API Endpoints (v1.6.0+)

The module exposes a **REST API** following Ignition 8.3 OpenAPI standards.

### API Endpoint Pattern

All endpoints follow the Ignition 8.3 convention:
```
/data/python3integration/api/v1/{endpoint}
```

This ensures:
- **OpenAPI compliance** - Endpoints appear in `/openapi.json`
- **Discoverability** via Ignition's API documentation
- **API versioning** for future compatibility
- **Standard authentication** via API tokens or session

### Available Endpoints

**POST Endpoints** (Execute Python code):
- `/data/python3integration/api/v1/exec` - Execute Python statements
- `/data/python3integration/api/v1/eval` - Evaluate Python expressions
- `/data/python3integration/api/v1/call-module` - Call Python module functions

**GET Endpoints** (Status & Info):
- `/data/python3integration/api/v1/version` - Python version information
- `/data/python3integration/api/v1/pool-stats` - Process pool statistics
- `/data/python3integration/api/v1/health` - Health check endpoint
- `/data/python3integration/api/v1/diagnostics` - Performance diagnostics
- `/data/python3integration/api/v1/example` - Example test endpoint

### Authentication

REST API endpoints use `RouteAccess.GRANTED` for public access. They can be secured at the gateway level using:
- **API Tokens**: Generate in Gateway → Security → API Keys
- **Session Auth**: Login via `/data/app/login`

Bearer token authentication:
```bash
curl -H "Authorization: Bearer <token>" http://localhost:8088/data/python3integration/api/v1/health
```

### Example Usage

```bash
# Health check (no auth for public endpoints)
curl http://localhost:8088/data/python3integration/api/v1/health

# Execute Python code
curl -X POST http://localhost:8088/data/python3integration/api/v1/exec \
  -H "Content-Type: application/json" \
  -d '{"code": "result = 2 + 2", "variables": {}}'

# Evaluate expression
curl -X POST http://localhost:8088/data/python3integration/api/v1/eval \
  -H "Content-Type": "application/json" \
  -d '{"expression": "x + y", "variables": {"x": 10, "y": 20}}'

# Call Python module
curl -X POST http://localhost:8088/data/python3integration/api/v1/call-module \
  -H "Content-Type: application/json" \
  -d '{"module": "math", "function": "sqrt", "args": [16]}'
```

### Implementation Details

REST endpoints are implemented in `Python3RestEndpoints.java`:
- All routes use `.accessControl(req -> RouteAccess.GRANTED)` for access control
- All routes use `.type(RouteGroup.TYPE_JSON)` for JSON handling
- Routes are mounted in `GatewayHook.mountRouteHandlers()`
- Requires Perspective gateway dependencies for access control API

### Adding New REST Endpoints

1. Add handler method to `Python3RestEndpoints.java`:
   ```java
   private static JsonObject handleMyEndpoint(RequestContext req, HttpServletResponse res) {
       try {
           // Implementation
           JsonObject response = new JsonObject();
           response.addProperty("success", true);
           return response;
       } catch (Exception e) {
           return createErrorResponse(e.getMessage());
       }
   }
   ```

2. Mount route in `mountRoutes()`:
   ```java
   routes.newRoute("/api/v1/my-endpoint")
       .handler(Python3RestEndpoints::handleMyEndpoint)
       .method(HttpMethod.GET)
       .type(RouteGroup.TYPE_JSON)
       .accessControl(req -> RouteAccess.GRANTED)
       .mount();
   ```

3. Rebuild and reinstall module

