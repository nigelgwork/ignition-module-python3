# Python 3 Integration Module for Ignition

**Current Version: v2.4.0** | [Changelog](#changelog) | [GitHub](https://github.com/nigelgwork/ignition-module-python3)

This module enables Python 3 scripting functions in Ignition 8.3+, allowing you to use modern Python 3 features and libraries alongside Ignition's built-in Jython 2.7 environment.

## Features

- **Python 3 Execution**: Execute Python 3 code from Ignition scripts
- **Designer IDE** *(v1.7.0+, refactored v2.0.0)*: Interactive Python 3 IDE in the Designer with code editor, output panel, and diagnostics
- **Process Pool**: Efficient subprocess pooling for minimal overhead
- **Full Python 3 Library Support**: Use numpy, pandas, requests, and any Python 3 package
- **Simple API**: Easy-to-use scripting functions under `system.python3.*`
- **REST API** *(v1.6.0+)*: HTTP endpoints for external integration and Designer communication
- **Automatic Health Checking**: Self-healing process pool with automatic restart
- **Cross-Platform**: Works on Windows, Linux, and macOS

## Architecture

This module uses a **subprocess + process pool** approach:

1. **Process Pool**: Maintains 3-5 warm Python 3 processes (configurable)
2. **JSON Communication**: Bidirectional communication via stdin/stdout
3. **Health Monitoring**: Automatic health checks and process restart
4. **Thread-Safe**: Concurrent script execution support

### Benefits over Alternatives

- **No complex dependencies** (no Py4J, no JEP)
- **Stable and reliable** (isolated processes)
- **Easy to debug** (standard Python logging)
- **Minimal overhead** (process reuse)

## Prerequisites

### Required

1. **Ignition 8.3.0+** with unsigned modules enabled
2. **Python 3.8+** installed on the Gateway server
3. **Java 17** (required by Ignition 8.3)

### Installation Steps

#### 1. Install Python 3

**Windows:**
```bash
# Download from python.org or use Chocolatey
choco install python3

# Verify installation
python --version
```

**Linux:**
```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install python3 python3-pip

# Verify installation
python3 --version
```

**macOS:**
```bash
# Using Homebrew
brew install python3

# Verify installation
python3 --version
```

#### 2. Enable Unsigned Modules in Ignition

Edit `<ignition-install>/data/ignition.conf` and add:
```properties
wrapper.java.additional.X=-Dignition.allowunsignedmodules=true
```

Restart Ignition after making this change.

#### 3. Configure Python Path (Optional)

The module will try to auto-detect Python 3. To specify a custom path:

**Option A: System Property**
```properties
# In ignition.conf, add:
wrapper.java.additional.Y=-Dignition.python3.path=/path/to/python3
```

**Option B: Environment Variable**
```bash
export IGNITION_PYTHON3_PATH=/path/to/python3
```

**Option C: Let Auto-Detection Work**

The module checks these locations automatically:
- Windows: `python3`, `python`, `C:\Python3*\python.exe`
- Linux: `python3`, `/usr/bin/python3`, `/usr/local/bin/python3`
- macOS: `python3`, `/usr/local/bin/python3`, `/opt/homebrew/bin/python3`

#### 4. Configure Pool Size (Optional)

```properties
# In ignition.conf, add:
wrapper.java.additional.Z=-Dignition.python3.poolsize=5
```

Default is 3 processes.

## Building the Module

```bash
cd python3-integration

# Build with Gradle
./gradlew build

# Module will be in: build/libs/python3-integration-signed.modl
```

## Installation

1. Open Ignition Gateway web interface (http://localhost:8088)
2. Navigate to **Config → System → Modules**
3. Scroll to bottom and click **Install or Upgrade a Module**
4. Select the `.modl` file
5. Click **Install**
6. Module status should show **Running**

## Usage

### Designer IDE (v2.0.30 - Current)

The Designer IDE provides an interactive development environment for testing Python 3 code directly in the Ignition Designer.

**Architecture:** Refactored in v2.0.0 with modular design (Managers + UI Panels)

**To open the IDE:**
1. Open the Ignition Designer
2. Navigate to **Tools → Python 3 IDE**
3. A new window will open with the code editor

**Features:**
- **Code Editor**: Write and edit Python 3 code with syntax highlighting
- **Real-time Syntax Checking** *(v1.11.0+)*: Red squiggles for errors, yellow for warnings
- **Modern UI** *(v1.12.0+)*: VS Code-inspired dark theme with modern buttons and styling
- **Refactored Architecture** *(v2.0.0+)*: Modular design with 95-490 line files (down from 2,676-line monolith)
- **Execute Button**: Run code on the Gateway (Ctrl+Enter shortcut)
- **Clear Output Button** *(v2.0.24)*: Quickly clear output and error panels
- **Keyboard Shortcuts** *(v2.0.25)*: Ctrl+Enter, Ctrl+S, Ctrl+Shift+S, Ctrl+N, Ctrl+F, Ctrl+H
- **Context Menus** *(v2.0.26)*: Right-click scripts (Load, Export, Rename, Delete, Move) and folders
- **Save As** *(v2.0.27)*: Full metadata save dialog with dirty state indicator
- **Current Script Label** *(v2.0.27)*: Shows active script with unsaved changes indicator (*)
- **Font Size Controls** *(v2.0.28)*: A+/A- buttons, Ctrl++/Ctrl+-/Ctrl+0 shortcuts
- **Move to Folder** *(v2.0.29)*: Context menu to move scripts between folders
- **Drag-and-Drop** *(v2.0.30)*: Drag scripts and folders to reorganize
- **Output Panel**: View execution results
- **Error Panel**: View detailed error messages and tracebacks
- **Diagnostics**: Real-time pool statistics (healthy processes, available, in use)
- **Execution Timing**: See how long each execution takes
- **Async Execution**: Non-blocking UI during code execution

**Example Workflow:**
```python
# Write code in the editor
import math
result = math.sqrt(144)
print(f"Square root: {result}")

# Click Execute (or press Ctrl+Enter)
# Results appear in Output panel
```

The Designer IDE communicates with the Gateway via REST API, so all code executes on the Gateway using the process pool (just like `system.python3.*` functions).

### Basic Examples

#### Execute Python 3 Code

```python
# In Ignition Script Console or any script

# Simple calculation (beyond Jython's capability)
result = system.python3.eval("2 ** 1000")
print(result)  # Huge number!

# Execute code block
code = """
result = sum([1, 2, 3, 4, 5])
"""
total = system.python3.exec(code)
print(total)  # 15
```

#### Use Python 3 Libraries

```python
# Use numpy
result = system.python3.callModule("math", "sqrt", [144])
print(result)  # 12.0

# Use pandas (if installed)
code = """
import pandas as pd
df = pd.DataFrame({'a': [1, 2, 3], 'b': [4, 5, 6]})
result = df.to_dict()
"""
data = system.python3.exec(code)
print(data)
```

#### Pass Variables

```python
# Execute with variables
code = """
result = x + y
"""
result = system.python3.exec(code, {'x': 10, 'y': 20})
print(result)  # 30

# Evaluate expression with variables
result = system.python3.eval("x * 2 + y", {'x': 5, 'y': 3})
print(result)  # 13
```

#### Advanced: Use Modern Python Features

```python
# List comprehensions, f-strings, type hints, etc.
code = """
data = [1, 2, 3, 4, 5]
result = [x**2 for x in data if x % 2 == 0]
"""
squares = system.python3.exec(code)
print(squares)  # [4, 16]

# Async/await (for CPU-bound tasks)
code = """
import asyncio

async def process():
    return sum(range(1000000))

result = asyncio.run(process())
"""
total = system.python3.exec(code)
print(total)
```

### API Reference

#### `system.python3.exec(code, variables=None)`
Execute Python 3 code. Set a `result` variable to return a value.

**Parameters:**
- `code` (str): Python code to execute
- `variables` (dict): Optional variables to pass to Python scope

**Returns:** Result of execution

**Example:**
```python
code = """
import json
data = {'name': 'John', 'age': 30}
result = json.dumps(data)
"""
json_str = system.python3.exec(code)
```

#### `system.python3.eval(expression, variables=None)`
Evaluate a Python 3 expression and return the result.

**Parameters:**
- `expression` (str): Python expression
- `variables` (dict): Optional variables

**Returns:** Result of expression

**Example:**
```python
result = system.python3.eval("len([1,2,3,4,5])")  # 5
```

#### `system.python3.callModule(moduleName, functionName, args, kwargs=None)`
Call a function from a Python 3 module.

**Parameters:**
- `moduleName` (str): Module name (e.g., "math", "numpy")
- `functionName` (str): Function name
- `args` (list): Positional arguments
- `kwargs` (dict): Optional keyword arguments

**Returns:** Function result

**Example:**
```python
# math.pow(2, 10)
result = system.python3.callModule("math", "pow", [2, 10])  # 1024.0
```

#### `system.python3.isAvailable()`
Check if Python 3 is available.

**Returns:** Boolean

#### `system.python3.getVersion()`
Get Python 3 version information.

**Returns:** Dictionary with version info

**Example:**
```python
info = system.python3.getVersion()
print(info['version'])
```

#### `system.python3.getPoolStats()`
Get process pool statistics.

**Returns:** Dictionary with pool stats

**Example:**
```python
stats = system.python3.getPoolStats()
print("Available: %d, In Use: %d" % (stats['available'], stats['inUse']))
```

#### `system.python3.example()`
Run a simple test example.

**Returns:** Test result string

#### `system.python3.getAvailableScripts()` *(v2.0.24+)*
Get list of all available saved scripts with metadata. Useful for building script selection UIs and autocomplete helpers.

**Returns:** List of script metadata dictionaries

**Example:**
```python
scripts = system.python3.getAvailableScripts()
# Returns: [
#   {"name": "CalculateTax", "description": "Tax calculator",
#    "path": "Finance/CalculateTax", "author": "John",
#    "version": "1.0", "lastModified": "2025-10-18 10:30:00"},
#   ...
# ]
```

## Installing Python Packages

To use third-party packages in your Python 3 scripts:

```bash
# On the Gateway server
pip3 install numpy pandas requests

# Or use a virtual environment (recommended)
python3 -m venv /opt/ignition-python
source /opt/ignition-python/bin/activate
pip install numpy pandas requests

# Then configure module to use venv Python:
# -Dignition.python3.path=/opt/ignition-python/bin/python
```

## Troubleshooting

### Module Won't Load

**Check Gateway logs:**
```bash
tail -f <ignition-install>/logs/wrapper.log
```

Look for errors like:
- "Python process is not alive" → Python not found or wrong path
- "Failed to initialize Python 3 process pool" → Python installation issue

**Verify Python installation:**
```bash
python3 --version  # Should show Python 3.8+
```

### Functions Return Errors

**Test module manually:**
```python
# In Script Console
result = system.python3.example()
print(result)

# Check availability
available = system.python3.isAvailable()
print("Python 3 available:", available)

# Get version
version = system.python3.getVersion()
print(version)
```

### Performance Issues

**Check pool stats:**
```python
stats = system.python3.getPoolStats()
print(stats)
```

If `available` is often 0, increase pool size:
```properties
wrapper.java.additional.X=-Dignition.python3.poolsize=10
```

### Process Pool Hangs

The module includes automatic health checking. If processes hang:

1. Check Gateway logs for health check failures
2. Processes will be automatically restarted
3. Consider increasing timeout or pool size

## Limitations

### Data Type Mapping

Python ↔ JSON ↔ Java conversions:

| Python 3 | JSON | Jython/Java |
|----------|------|-------------|
| int, float | number | Number |
| str | string | String |
| bool | boolean | Boolean |
| list, tuple | array | List |
| dict | object | Map |
| None | null | null |

**Complex objects** (custom classes, numpy arrays, etc.) are converted to strings.

### Performance

- **First call**: ~100-200ms (process warmup)
- **Subsequent calls**: ~10-50ms (process pool)
- **Large data**: Serialize/deserialize overhead

For high-performance scenarios, consider:
- Batch operations in a single `exec()` call
- Increase pool size
- Use Jython for simple tasks

### Concurrency

- Process pool supports concurrent execution
- Each process handles one request at a time
- Pool size = max concurrent Python executions

## Development

### Project Structure

```
python3-integration/
├── common/                  # Shared code (Common scope)
├── gateway/                 # Gateway scope
│   └── src/main/
│       ├── java/           # Java source
│       └── resources/      # Python bridge script
├── designer/                # Designer scope (v1.7.0+, refactored v2.0.0)
│   └── src/main/java/      # Designer IDE components
│       ├── Python3IDE_v2.java           # Main IDE panel (refactored v2.0.0)
│       ├── ui/                          # UI panels (v2.0.0+)
│       │   ├── EditorPanel.java
│       │   └── ScriptTreePanel.java
│       ├── managers/                    # Business logic (v2.0.0+)
│       │   ├── GatewayConnectionManager.java
│       │   ├── ScriptManager.java
│       │   └── ThemeManager.java
│       ├── Python3RestClient.java       # REST API client
│       ├── Python3ExecutionWorker.java  # Async worker
│       ├── DesignerHook.java            # Designer module hook
│       ├── ModernTheme.java             # UI theme constants (v1.12.0+)
│       └── ModernStatusBar.java         # Status bar component (v1.12.0+)
├── docs/                    # Documentation
│   ├── V2_ARCHITECTURE_GUIDE.md         # v2.0 architecture (v2.0.3)
│   ├── V2_MIGRATION_GUIDE.md            # Migration guide (v2.0.3)
│   ├── V2_FEATURE_COMPARISON_AND_ROADMAP.md  # Roadmap
│   └── V2_STATUS_SUMMARY.md             # Status summary
├── build.gradle.kts        # Root build file
├── settings.gradle.kts     # Gradle settings
├── version.properties      # Module version (v2.0.30)
└── README.md               # This file
```

### Building from Source

```bash
# Clone and build
git clone <repo>
cd python3-integration
./gradlew clean build

# Module output
ls -lh build/libs/*.modl
```

### Running Tests

```bash
./gradlew test
```

## Contributing

Contributions welcome! Please:

1. Follow existing code style
2. Add tests for new features
3. Update documentation
4. Test on multiple platforms

## License

Apache 2.0

## Support

- **Issues**: GitHub Issues
- **Forum**: Inductive Automation Forum
- **Docs**: See `/docs` directory

## Roadmap

**Current Release: v2.0.30** - Phase 1 & 2 Complete! (Essential + Power User Features)

**Completed:**
- [x] Designer integration (Python 3 script editor) - **v1.7.0**
- [x] Real-time syntax checking - **v1.11.0**
- [x] Modern UI design (VS Code theme) - **v1.12.0**
- [x] Modular architecture refactor - **v2.0.0**
- [x] Enhanced diagnostics - **v2.0.8**
- [x] UX fixes (scrollbars, dividers, dialogs) - **v2.0.9**
- [x] Python version detection - **v2.0.17**
- [x] Theme-aware split pane dividers - **v2.0.22**
- [x] **Clear Output button** - **v2.0.24**
- [x] **Script autocomplete API (getAvailableScripts)** - **v2.0.24**
- [x] **Keyboard shortcuts (Ctrl+Enter, Ctrl+S, etc.)** - **v2.0.25**
- [x] **Context menu (right-click on scripts/folders)** - **v2.0.26**
- [x] **Save As button with full metadata** - **v2.0.27**
- [x] **Dirty state indicator (unsaved changes)** - **v2.0.27**
- [x] **Current script label** - **v2.0.27**
- [x] **Font size controls (A+/A- buttons, Ctrl++/-)** - **v2.0.28**
- [x] **Move script between folders** - **v2.0.29**
- [x] **Drag-and-drop organization** - **v2.0.30**

**Optional Future Enhancements (Phase 3):**
- [ ] Advanced Find/Replace dialog (v2.1.0)
- [ ] Real-time syntax checking (v2.2.0)
- [ ] Intelligent auto-completion with Jedi (v2.2.0)
- [ ] Virtual environment support
- [ ] Package manager UI in Gateway
- [ ] Async/callback support
- [ ] Binary data handling (bytes, numpy arrays)
- [ ] Streaming results for large datasets
- [ ] Python process resource limits (CPU, memory)
- [ ] Multiple Python versions support

See [ROADMAP.md](ROADMAP.md) and [docs/V2_FEATURE_COMPARISON_AND_ROADMAP.md](docs/V2_FEATURE_COMPARISON_AND_ROADMAP.md) for detailed roadmap.

## Future Enhancements

Comprehensive planning documents for enterprise-scale production deployments:

### 📋 [Comprehensive Test Suite](docs/roadmap/COMPREHENSIVE_TEST_SUITE.md)
**Priority: HIGH** | Unit tests, integration tests, performance benchmarks, security validation, CI/CD pipeline
- Achieve 80%+ code coverage
- JUnit/Mockito framework
- GitHub Actions automation
- Performance baselines

### 🔍 [Process Monitoring and Recovery](docs/roadmap/PROCESS_MONITORING_AND_RECOVERY.md)
**Priority: MEDIUM** | Production-grade monitoring, health checks, circuit breakers, automatic recovery
- Multi-level health monitoring
- Circuit breaker pattern
- Adaptive pool sizing
- Metrics export (Prometheus, CloudWatch)
- Alert management (Email, Slack)

### 🔒 [Python Sandboxing and Security](docs/roadmap/PYTHON_SANDBOXING_AND_SECURITY.md)
**Priority: MEDIUM-HIGH** | Resource protection, audit trails, user context tracking, compliance
- Resource limit enforcement
- User authentication and authorization
- Audit logging for compliance
- Script access control

**[See Complete Roadmap →](docs/roadmap/README.md)**

## Examples

See the usage examples above for common Python 3 integration patterns. Additional examples can be found in the module documentation and test scripts.

## Credits

Built using the Ignition SDK:
- https://github.com/inductiveautomation/ignition-sdk-examples
- https://www.sdk-docs.inductiveautomation.com/

## Changelog

### 2.4.0 (Modern UX Polish - Sleek Scrollbars & Autocomplete Diagnostics)
- **NEW**: Modern sleek scrollbars with minimal design
  - Thin, semi-transparent scrollbar thumbs (8px base, 10px on hover)
  - Rounded corners for modern aesthetic
  - Invisible track for clean look
  - Smooth hover effects
  - Matches VS Code / IntelliJ style
- **NEW**: Autocomplete status indicator in status bar
  - Shows "AC: Ready" when Jedi is available
  - Shows "AC: No Jedi" with installation instructions when unavailable
  - Helpful tooltip: "Ctrl+Space to trigger"
  - Real-time status updates
- **FIXED**: White borders on code editor and output areas now blend with dark theme
  - All scroll panes now use `BORDER_DEFAULT` color
  - Consistent with description area border style
  - Clean, seamless appearance
- **IMPROVED**: Better autocomplete error handling
  - Distinguishes between Jedi not installed vs temporary failures
  - 1-minute cooldown after errors to prevent spam
  - Clear user feedback about autocomplete availability
- **UX**: Smoother scrolling with increased unit increment (16px)
- **UX**: All visual elements now harmonized for sleek, modern IDE feel

### 2.3.2 (Comprehensive Dark Theme Fix)
- **FIXED**: JSplitPane dividers now properly themed (no more light gray)
- **FIXED**: JScrollPane backgrounds now match dark theme
- **FIXED**: All borders now use theme colors consistently
- **FIXED**: Viewport backgrounds properly themed
- **IMPROVED**: Comprehensive UIManager settings for ALL Swing components
  - ScrollBar (background, track, thumb colors)
  - SplitPane (divider, shadows, borders)
  - Borders (TitledBorder, general borders)
  - ComboBox, Tree, List, Table components
  - Menu and PopupMenu components
  - All text components (TextField, TextArea)
- **IMPROVED**: Light theme also updated with comprehensive settings
- **UX**: Complete visual consistency across all UI elements
- **UX**: No more light gray components showing through dark theme

### 2.3.1 (UX Improvements + Auto-install Jedi)
- **NEW**: Jedi package automatically installed on module startup for autocomplete
- **IMPROVED**: Theme dropdown reduced by 15% (180→153px) to prevent "Theme:" label cutoff
- **IMPROVED**: Description panel made 25% taller (150→188px) with scrollbar removed
- **NEW**: "Edit Metadata..." context menu option for scripts
  - Edit name, description, author, and version all at once
  - Clean dialog interface with dark theme support
- **UX**: Better use of available space in Script Information panel
- **UX**: All metadata now editable from right-click menu

### 2.3.0 (Bundled Python Package Management - Foundation)
- **NEW**: Python3PackageManager class for offline package installation
- **NEW**: Package catalog system with metadata (packages.json)
- **NEW**: Platform-specific wheel bundling (Windows x64, Linux x64)
- **NEW**: REST API endpoints for package management:
  - GET /api/v1/packages/catalog - Get available packages
  - GET /api/v1/packages/status - Get installation status
  - POST /api/v1/packages/install/:name - Install package bundle
  - POST /api/v1/packages/uninstall/:name - Uninstall package bundle
  - POST /api/v1/packages/verify - Verify installed packages
- **NEW**: Helper script download_wheels.py for downloading wheels from PyPI
- **NEW**: Package bundles: jedi (~3MB), web (~5MB), datascience (~85MB)
- **IMPROVED**: Air-gapped deployment support
- **DOCS**: Package bundling documentation and usage guide
- **NOTE**: This is the foundation release - wheels not yet bundled (will be added in future release)

### 2.2.0 (Real-time Syntax Checking + Intelligent Auto-Completion)
- **COMPLETE**: Phase 3 fully implemented! All advanced IDE features now available
- **SYNTAX CHECKING**: Real-time Python syntax validation with debouncing (500ms)
- **SYNTAX CHECKING**: Red squiggly underlines for syntax errors
- **SYNTAX CHECKING**: Yellow squiggly underlines for warnings
- **SYNTAX CHECKING**: Error tooltips on hover via RSyntaxTextArea parser system
- **AUTO-COMPLETION**: Jedi-powered intelligent completions
- **AUTO-COMPLETION**: Context-aware suggestions based on code analysis
- **AUTO-COMPLETION**: Function signatures in completion popup
- **AUTO-COMPLETION**: Docstrings displayed for each completion
- **AUTO-COMPLETION**: Type badges (function, class, module, keyword, variable)
- **AUTO-COMPLETION**: Auto-activation after typing (500ms delay)
- **AUTO-COMPLETION**: Manual trigger with Ctrl+Space
- **IMPROVED**: Rich HTML formatting in completion descriptions
- **DOCS**: Phase 3 complete - Full IDE feature parity achieved!

### 2.1.0 (Advanced Find/Replace Dialog)
- **NEW**: Advanced Find/Replace dialog with comprehensive features
- **NEW**: Regex pattern support for powerful search capabilities
- **NEW**: Whole word matching option
- **NEW**: Search direction (forward/backward)
- **NEW**: Find/Replace history with combo boxes (stores last 20 searches)
- **NEW**: Count matches feature to see total occurrences
- **NEW**: Keyboard shortcut Ctrl+Shift+F to open advanced dialog
- **NEW**: Additional shortcuts: F3 (Find Next), Shift+F3 (Find Previous), Escape (Close)
- **IMPROVED**: Modern dark theme styling for dialog
- **IMPROVED**: Status feedback with color-coded messages
- **DOCS**: Phase 3 implementation begins (Advanced IDE Features)

### 2.0.30 (Drag-and-Drop Organization - Validated)
- **VALIDATED**: Drag-and-drop already fully implemented via ScriptTreeTransferHandler
- **VALIDATED**: Scripts and folders can be dragged to reorganize
- **VALIDATED**: Visual feedback during drag operations
- **BUILD**: Verified build and documentation updates
- **DOCS**: Updated all documentation to reflect Phase 2 completion

### 2.0.29 (Move Script Between Folders)
- **NEW**: "Move to Folder..." context menu item for scripts
- **NEW**: Folder selection dialog with JComboBox picker
- **NEW**: Async move operation (load → save with new folder path)
- **NEW**: Status feedback and tree refresh after move
- **IMPROVED**: Better script organization capabilities

### 2.0.28 (Font Size Controls)
- **NEW**: A+ and A- buttons in toolbar for font size adjustment
- **NEW**: Tooltips showing keyboard shortcut hints (Ctrl++/Ctrl+-)
- **VALIDATED**: Font size persistence already existed via Preferences
- **VALIDATED**: Keyboard shortcuts (Ctrl++, Ctrl+-, Ctrl+0) already existed
- **IMPROVED**: Better accessibility for users with vision needs

### 2.0.27 (Save Improvements - Validated)
- **VALIDATED**: Save As button already existed with full metadata dialog
- **VALIDATED**: Current Script Label already existed showing folder/script path
- **VALIDATED**: Dirty State Indicator (*) already existed for unsaved changes
- **BUILD**: Verified build and documentation updates
- **DOCS**: Updated all documentation to reflect Phase 1 completion

### 2.0.26 (Context Menus - Validated)
- **VALIDATED**: Context menus already existed for scripts and folders
- **VALIDATED**: Right-click scripts shows: Load, Export, Rename, Delete
- **VALIDATED**: Right-click folders shows: New Script, New Subfolder, Rename
- **BUILD**: Verified build and documentation updates

### 2.0.25 (Keyboard Shortcuts - Validated)
- **VALIDATED**: All keyboard shortcuts already existed
- **VALIDATED**: Ctrl+Enter (Execute), Ctrl+S (Save), Ctrl+Shift+S (Save As)
- **VALIDATED**: Ctrl+N (New), Ctrl+F (Find), Ctrl+H (Replace)
- **BUILD**: Verified build and documentation updates

### 2.0.24 (Script Autocomplete + Clear Output)
- **NEW**: getAvailableScripts() method for script discovery
- **NEW**: REST API endpoint /api/v1/scripts/available
- **NEW**: RPC interface update for Designer/Client access
- **NEW**: Python3ScriptModule.properties documentation for autocomplete
- **NEW**: Clear Output button added to EditorPanel toolbar
- **IMPROVED**: Script discoverability for programmatic access
- **IMPROVED**: Foundation for future dynamic script registration

### 2.0.23 (Repository Consolidation)
- **CLEANUP**: Removed 11 redundant documentation files (5,300+ lines)
- **CLEANUP**: Deleted outdated UPGRADE_GUIDE.md (referenced v1.7.x versions)
- **IMPROVED**: Kept only essential v2.0 documentation (7 files, 2,900 lines)
- **IMPROVED**: Root README simplified by 38% (124 → 77 lines)
- **IMPROVED**: Documentation now focused and navigation-oriented
- **ROADMAP**: Added 2 HIGH priority UI fixes (blue border, panel dividers)
- **DOCS**: Updated both README files to v2.0.23

### 2.0.22 (Theme System Completion)
- **FIXED**: JSplitPane dividers now respect current theme on creation (dark/light)
- **FIXED**: mainSplit divider (sidebar | editor) uses theme-aware colors
- **FIXED**: sidebarSplit divider (tree | metadata) uses theme-aware colors
- **FIXED**: bottomSplit divider (execution results | diagnostics) uses theme-aware colors
- **IMPROVED**: All dividers use ModernTheme.BACKGROUND_DARKER (dark) or Color(200, 200, 200) (light)
- **IMPROVED**: Split panes made instance variables for proper theme updates
- **IMPROVED**: Consistent theme experience across all UI components

### 2.0.21 (Security Mode Fix - CRITICAL)
- **FIXED**: Changed default security mode from RESTRICTED to ADMIN for execution
- **FIXED**: RESTRICTED mode was blocking exec(), eval(), compile(), import sys
- **IMPROVED**: Designer IDE now works without admin API key (development tool)

### 2.0.20 (Execution Logging)
- **NEW**: Comprehensive INFO-level logging throughout execution flow
- **NEW**: Python3ExecutionWorker logs execution start, code, completion
- **NEW**: Python3RestClient logs HTTP requests/responses
- **IMPROVED**: Execution debugging now visible in INFO logs

### 2.0.19 (Context Menu Text Fix)
- **FIXED**: Right-click context menu text now visible in dark theme
- **FIXED**: stylePopupMenu() now called AFTER menu items are added
- **IMPROVED**: Proper theme styling for Load, Export, Rename, Delete menu items

### 2.0.18 (Diagnostics Spam Fix)
- **FIXED**: Removed auto-refresh timer from DiagnosticsPanel (was polling every 5 seconds)
- **IMPROVED**: Diagnostics now refresh manually (on connection, after execution)
- **IMPROVED**: No more log spam from Python version polling

### 2.0.17 (Python Version Detection)
- **NEW**: Real-time Python version detection from Gateway
- **NEW**: Version display in diagnostics panel
- **FIXED**: Python version REST endpoint security mode (ADMIN for development)

### 2.0.9 (UX Fixes)
- **FIXED**: Scrollbars now only appear when needed (AS_NEEDED policy applied to all scroll panes)
- **FIXED**: Theme selector dropdown width increased to 150px to prevent "VS Code Dark+" text cutoff
- **FIXED**: Description panel expanded from 120px to 150px for better usability (50% increase from v2.0.1)
- **FIXED**: Python version detection now displays actual version (e.g., "Python: 3.11.5") instead of "Unknown"
- **FIXED**: Panel dividers now styled with dark theme (removed white dividers)
- **FIXED**: Popup dialogs (Save, Delete, Rename) now follow dark theme with proper styling
- **IMPROVED**: ScrollPane policies explicitly set to AS_NEEDED in ScriptTreePanel and EditorPanel
- **IMPROVED**: ModernStatusBar now fetches and displays Python version on connection
- **IMPROVED**: JSplitPane divider in EditorPanel styled to match dark theme
- **IMPROVED**: Enhanced UIManager properties for consistent dialog theming (TextField borders, selection colors, button hover states)

### 2.0.8 (Enhanced Diagnostics)
- **NEW**: Python version display in diagnostics panel
- **NEW**: Total executions counter with real-time updates
- **NEW**: Success rate display with color coding (95%+ green, 85%+ yellow, <85% red)
- **NEW**: Average execution time metric in milliseconds
- **NEW**: ExecutionMetrics data class for structured diagnostics parsing
- **IMPROVED**: Enhanced diagnostics panel with 10 metrics vs 6 in v2.0.0
- **IMPROVED**: Auto-refresh every 5 seconds for real-time monitoring
- **IMPROVED**: Color-coded health indicators for success rate and pool usage

### 2.0.7 (Export/Import)
- **NEW**: Export script to .py file functionality with file chooser dialog
- **NEW**: Import script from .py file functionality
- **NEW**: Automatic .py extension addition on export
- **IMPROVED**: JFileChooser dialogs with Python file filter (.py)
- **IMPROVED**: File I/O with proper error handling and status messages

### 2.0.6 (Find/Replace)
- **NEW**: Find/Replace toolbar integrated into EditorPanel
- **NEW**: Find Next functionality with forward search
- **NEW**: Replace and Replace All functionality
- **NEW**: Match case checkbox option for case-sensitive search
- **IMPROVED**: Integrated with RSyntaxTextArea SearchEngine API
- **IMPROVED**: Toolbar layout with labeled input fields

### 2.0.5 (Folder Management)
- **NEW**: New Folder button with folder path input dialog
- **NEW**: Rename button for scripts and folders
- **NEW**: ScriptManager.renameScript() method for script renaming
- **IMPROVED**: Complete folder organization capabilities
- **IMPROVED**: Folder and script rename with validation

### 2.0.4 (Delete Script)
- **NEW**: Delete script functionality with confirmation dialog
- **NEW**: Script deletion clears editor and metadata after successful delete
- **NEW**: ScriptTreePanel.getSelectedScriptName() helper method
- **IMPROVED**: Warning-style confirmation dialog (Yes/No) for safe deletion
- **IMPROVED**: Status bar feedback for delete operations

### 2.0.3 (Comprehensive Documentation)
- **NEW**: V2_ARCHITECTURE_GUIDE.md (530 lines) - Complete architectural overview
- **NEW**: V2_MIGRATION_GUIDE.md (470 lines) - Migration strategies and code examples
- **NEW**: DEVELOPER_EXTENSION_GUIDE.md (530 lines) - Extension patterns and best practices
- **IMPROVED**: Comprehensive v2.0 documentation for developers and contributors
- **IMPROVED**: Detailed component interaction diagrams and threading model
- **IMPROVED**: Code examples for extending the v2.0 architecture

### 2.0.2 (Code Cleanup)
- **FIXED**: Star imports replaced with specific imports in all v2 files
- **FIXED**: Removed unused imports across codebase (EditorPanel, ScriptTreePanel, ThemeManager, etc.)
- **IMPROVED**: Checkstyle warnings reduced from 91 to 50
- **IMPROVED**: Code quality and maintainability improvements

### 2.0.1 (UX Fix)
- **IMPROVED**: Metadata description panel height increased from 70px to 120px (70% increase)
- **IMPROVED**: Better scroll pane sizing for multi-line script descriptions
- **IMPROVED**: Enhanced metadata panel visibility and usability

### 2.0.0 (Architecture Refactoring)
- **ARCHITECTURE**: Refactored from 2,676-line monolith (Python3IDE_v1_17_2.java) to modular architecture
- **NEW**: GatewayConnectionManager for centralized Gateway communication
- **NEW**: ScriptManager for script CRUD operations (load, save, delete, list)
- **NEW**: ThemeManager for centralized theme management (Dark, Monokai, VS Code Dark+)
- **NEW**: EditorPanel UI component (95 lines) with RSyntaxTextArea integration
- **NEW**: ScriptTreePanel UI component (440 lines) with hierarchical folder structure
- **NEW**: MetadataPanel UI component (145 lines) with read-only script metadata
- **NEW**: DiagnosticsPanel UI component (241 lines) with real-time pool statistics
- **NEW**: ModernStatusBar UI component with connection status and pool stats
- **NEW**: Python3IDE_v2.java main class (490 lines) orchestrating all components
- **ARCHITECTURE**: Token reduction from 25K tokens to 2.5K tokens per file (10x improvement)
- **ARCHITECTURE**: Separation of concerns - Managers (business logic), UI Panels (presentation), Main Class (orchestration)
- **IMPROVED**: Easier testing with isolated, mockable components
- **IMPROVED**: Easier maintenance with focused, single-responsibility classes
- **IMPROVED**: Easier extension with clear extension points and dependency injection
- **IMPROVED**: Each file 95-490 lines (vs 2,676 in v1.17.2 monolith)

### 1.12.1 (Bug Fixes)
- **FIXED**: URL encoding for script names with spaces in REST API calls
- **IMPROVED**: Script tree font size increased from 14pt to 16pt for better readability
- **IMPROVED**: Script tree row height increased to 32px for proper spacing
- **IMPROVED**: Script tree icon-text gap increased to 12px
- **IMPROVED**: ModernTheme colors applied to script tree for consistent UI

### 1.12.0 (Modern UI Design)
- **NEW**: Modern UI with VS Code-inspired dark theme
- **NEW**: ModernStatusBar with connection status, pool stats, and cursor position
- **NEW**: ModernButton components with hover effects and rounded corners
- **NEW**: ModernTheme color palette for consistent styling
- **NEW**: Rounded borders (RoundedBorder) for modern UI appearance
- **IMPROVED**: All buttons replaced with ModernButton (primary, success, default)
- **IMPROVED**: Dark theme applied to all panels, toolbars, and text areas
- **IMPROVED**: Enhanced visual feedback with modern colors
- **IMPROVED**: Better readability with updated fonts and spacing
- **IMPROVED**: Professional IDE appearance matching modern code editors

### 1.11.0 (Real-time Syntax Checking)
- **NEW**: Real-time Python syntax checking in Designer IDE
- **NEW**: Red squiggly underlines for syntax errors
- **NEW**: Yellow squiggly underlines for code quality warnings
- **NEW**: REST API `/check-syntax` endpoint for syntax validation
- **NEW**: Pyflakes integration for enhanced code quality checks
- **NEW**: Debounced syntax checking (500ms delay after typing stops)
- **IMPROVED**: Designer IDE with integrated syntax parser
- **IMPROVED**: Better developer experience with immediate feedback

### 1.7.0 (Designer IDE Release)
- **NEW**: Designer scope with interactive Python 3 IDE
- **NEW**: Python3IDE panel accessible from Tools menu
- **NEW**: REST API client for Designer-Gateway communication
- **NEW**: Async execution worker (SwingWorker) for non-blocking UI
- **NEW**: Real-time diagnostics in Designer IDE
- **ARCHITECTURE**: Moved from RPC to REST API for Designer communication
- **IMPROVED**: Better Designer experience with code editor, output, and error panels

### 1.6.1 (License Fix)
- **FIXED**: License display showing "Free" instead of "Trial"
- **FIXED**: Module upgrade compatibility (restored original module ID)

### 1.6.0 (REST API Release)
- **NEW**: REST API endpoints for Python execution
- **NEW**: OpenAPI 3.0 specification
- **NEW**: Health check endpoint
- **NEW**: Diagnostics endpoint
- **IMPROVED**: Better external integration support

### 1.0.0 (Initial Release)
- Python 3 subprocess execution
- Process pool management
- Basic scripting functions
- Auto-detection of Python path
- Health checking and auto-restart
