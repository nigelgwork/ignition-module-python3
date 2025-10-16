# Python 3 IDE Enhancement Roadmap

**Project**: Ignition Python 3 Integration Module
**Current Version**: 1.8.0
**Target**: Transform basic code editor into professional Python IDE

---

## 📋 Version History & Status

### ✅ v1.0.0 - v1.8.0 (Completed)
- Python 3.11 execution via process pool (3-20 concurrent processes)
- REST API for remote code execution
- Designer IDE with basic code editor
- Saved scripts with JSON persistence
- Async execution (non-blocking UI)
- Pool statistics and health monitoring
- Self-contained Python distribution (embedded 3.11.5)
- pip package support
- Gateway URL configuration
- Basic save/load/delete script operations

---

## 🚀 v1.9.0 - Professional UI & Syntax Highlighting

**Status**: In Progress
**Release Target**: Next release
**Effort Estimate**: 3-5 days

### Features

#### 1. Enhanced Script Management UI
- **Left Sidebar with Tree Browser**
  - Folder hierarchy for script organization
  - Custom tree icons (folder, Python file)
  - Drag-and-drop to move scripts between folders
  - Expand/collapse folders

- **Metadata Display Panel**
  - Single-click on script → show metadata (author, created date, version)
  - Information panel below tree view
  - Last modified timestamp

- **Smart Script Loading**
  - Double-click to load script into editor
  - Unsaved changes detection
  - Prompt: "Save current script?" dialog before loading
  - Three options: Save, Discard, Cancel

- **Right-Click Context Menu**
  - New Folder
  - New Script
  - Rename
  - Delete (with confirmation)
  - Move to Folder...
  - Export Script (.py file)
  - Duplicate Script

- **Export/Import Functionality**
  - Export single script as .py file
  - Export folder as .zip archive
  - Import .py files into selected folder
  - Bulk import from .zip

#### 2. RSyntaxTextArea Integration
**Library**: [RSyntaxTextArea 3.3.4](https://github.com/bobbylight/RSyntaxTextArea)

**Features Enabled**:
- ✅ Python syntax highlighting
- ✅ Line numbers with gutter
- ✅ Code folding (collapse functions/classes)
- ✅ Bracket/parenthesis matching
- ✅ Auto-indentation
- ✅ Current line highlighting
- ✅ Mark occurrences (highlight all instances of selected text)
- ✅ Tab line painting (indentation guides)
- ✅ Whitespace visibility toggle

**Implementation**:
```java
// Replace JTextArea with RSyntaxTextArea
RSyntaxTextArea codeEditor = new RSyntaxTextArea(15, 80);
codeEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
codeEditor.setCodeFoldingEnabled(true);
codeEditor.setAutoIndentEnabled(true);
codeEditor.setMarkOccurrences(true);
codeEditor.setPaintTabLines(true);

RTextScrollPane scrollPane = new RTextScrollPane(codeEditor);
scrollPane.setLineNumbersEnabled(true);
```

#### 3. Theme System
**Built-in Themes**:
- **Light Themes**:
  - Default (RSyntaxTextArea default light)
  - IntelliJ Light
  - Eclipse Light

- **Dark Themes**:
  - Dark (RSyntaxTextArea default dark)
  - VS Code Dark+ ⭐ (default)
  - Monokai
  - Dracula

**Theme Selector**:
- Menu: `View → Themes → [theme name]`
- Saves preference to user settings (Java preferences API)
- Applies to entire IDE:
  - Code editor
  - Output area
  - Error area
  - Sidebar tree

**Implementation**:
```java
// Load theme
Theme theme = Theme.load(getClass().getResourceAsStream(
    "/org/fife/ui/rsyntaxtextarea/themes/dark.xml"
));
theme.apply(codeEditor);
```

#### 4. Enhanced Keyboard Shortcuts
| Shortcut | Action |
|----------|--------|
| **Ctrl+Enter** | Execute code (existing) |
| **Ctrl+S** | Save current script |
| **Ctrl+Shift+S** | Save As... |
| **Ctrl+N** | New script |
| **Ctrl+O** | Open script browser |
| **Ctrl+W** | Close current script |
| **Ctrl+F** | Find |
| **Ctrl+H** | Find & Replace |
| **Ctrl+/** | Toggle line comment |
| **Ctrl+D** | Duplicate line |
| **Ctrl+Shift+K** | Delete line |
| **Tab** | Indent selection |
| **Shift+Tab** | Un-indent selection |
| **Ctrl+]** | Increase indent |
| **Ctrl+[** | Decrease indent |
| **Ctrl++** | Increase font size |
| **Ctrl+-** | Decrease font size |
| **Ctrl+0** | Reset font size |

### Technical Implementation

#### New Classes
1. **ScriptTreeModel** (extends TreeModel)
   - Manages folder/script hierarchy
   - Supports add/remove/move operations
   - Fires tree events on changes

2. **ScriptTreeNode** (extends DefaultMutableTreeNode)
   - Wrapper for folders and scripts
   - Stores metadata (ScriptMetadata or FolderMetadata)
   - Custom toString() for display names

3. **ScriptTreeCellRenderer** (extends DefaultTreeCellRenderer)
   - Custom icons for folders (📁) and Python files (🐍)
   - Different colors for folders vs scripts
   - Bold text for unsaved scripts

4. **ScriptMetadataPanel** (extends JPanel)
   - Displays selected script metadata
   - Fields: Name, Author, Created, Modified, Version, Description
   - Read-only labels with styled formatting

5. **UnsavedChangesTracker**
   - Monitors code editor for changes
   - Tracks original content vs current content
   - isDirty() method
   - Fires events on dirty state change

#### Modified Classes
1. **Python3IDE.java**
   - Replace JTextArea with RSyntaxTextArea
   - Add JSplitPane for sidebar/editor split
   - Add JTree for script browser
   - Add metadata panel
   - Add theme menu
   - Add keyboard shortcut handlers
   - Add unsaved changes dialog

2. **Python3RestClient.java**
   - Already updated with metadata support ✅

3. **Python3ScriptRepository.java**
   - Already updated with metadata support ✅

#### Dependencies
Add to `designer/build.gradle.kts`:
```kotlin
dependencies {
    implementation("com.fifesoft:rsyntaxtextarea:3.3.4")
    implementation("com.fifesoft:autocomplete:3.3.1")  // For future code completion
    implementation("com.fifesoft:rstaui:3.3.1")        // For find/replace dialog
}
```

### UI Layout (Final)
```
┌───────────────────────────────────────────────────────┐
│ Gateway Connection: [http://localhost:9088] [Connect] │
├────────────┬──────────────────────────────────────────┤
│ Scripts    │ Python 3 Code Editor                     │
│            │ ┌──────────────────────────────────────┐ │
│ 📁 Folders │ 1│ # Python 3.11                       │ │
│  └─📁 Utils│ 2│ import requests                     │ │
│    └─🐍 API│ 3│                                     │ │
│ 📁 Tests   │ 4│ def main():                         │ │
│  └─🐍 Unit │ 5│     response = requests.get(...)   │ │
│            │ 6│     return response.json()          │ │
│ ───────────│  │                                     │ │
│ Metadata   │  └──────────────────────────────────────┘ │
│ Name: API  │ [Execute] [Clear] [Save]  Pool: 3/3 ✓   │
│ Author:... ├──────────────────────────────────────────┤
│ Created:...│ Output                         Errors   │
│ Version:1.0│ ┌──────────────────────────────────────┐ │
└────────────┴─┤ >>> Result: {'status': 'ok'}         │ │
               └──────────────────────────────────────┘ │
```

### Testing Plan
1. **Folder Operations**
   - Create folder
   - Rename folder
   - Delete folder (with confirmation)
   - Move script to folder

2. **Script Operations**
   - Create new script
   - Save script with metadata
   - Load script (verify metadata display)
   - Rename script
   - Delete script
   - Export script as .py file

3. **Unsaved Changes**
   - Modify code
   - Attempt to load different script
   - Verify "Save changes?" dialog
   - Test Save/Discard/Cancel options

4. **Syntax Highlighting**
   - Verify Python keywords highlighted
   - Verify strings, comments, numbers colored
   - Test code folding (collapse function)
   - Test bracket matching

5. **Themes**
   - Switch between light/dark themes
   - Verify theme persists after restart
   - Check all UI components themed correctly

6. **Keyboard Shortcuts**
   - Test all shortcuts in table above
   - Verify no conflicts with Ignition Designer shortcuts

---

## 🔌 v1.10.0 - Reusable Script Library Integration

**Status**: Planned
**Release Target**: After v1.9.0
**Effort Estimate**: 2-3 days

### Overview

Enable saved Python 3 scripts to be called from anywhere in Ignition's scripting environment, making the script library a reusable function library accessible from Script Transforms, Expression Structures, Gateway Event Scripts, and any other Jython context.

### Problem Statement

Currently, saved scripts can only be executed manually from the Designer IDE. However, users often want to:
- Call a saved data processing script from a Perspective Script Transform
- Use a saved utility function from multiple Gateway Event Scripts
- Reuse complex Python 3 logic across different Ignition scripting contexts
- Maintain a centralized library of Python 3 functions without copy-pasting code

### Solution

Add a new scripting function: `system.python3.callScript(scriptPath, args=[], kwargs={})`

This allows any Ignition script (Jython 2.7) to execute a saved Python 3 script by name/path and receive the result.

### Features

#### 1. New Scripting Function: `callScript()`

**Function Signature**:
```python
system.python3.callScript(scriptPath, args=[], kwargs={})
```

**Parameters**:
- `scriptPath` (str): Path to saved script (e.g., "My Scripts/data_processor" or "Utils/API/fetch_data")
- `args` (list, optional): Positional arguments to pass to the script
- `kwargs` (dict, optional): Keyword arguments to pass to the script

**Returns**: The value of the `result` variable set by the Python script

**Example Usage**:

```python
# In Perspective Script Transform (Jython 2.7)
# Call a saved Python 3 script that processes sensor data
processed = system.python3.callScript(
    "Data Processing/normalize_sensor_data",
    args=[value.sensorReading, value.sensorType],
    kwargs={"method": "zscore", "threshold": 3.0}
)

return processed
```

#### 2. Script Execution Model

**Saved Script Contract**:
Scripts must set a `result` variable to return a value:

```python
# Saved script: "Data Processing/normalize_sensor_data"
import numpy as np

def normalize(data, sensor_type, method='minmax', threshold=None):
    """Normalize sensor data using specified method"""
    if method == 'zscore':
        mean = np.mean(data)
        std = np.std(data)
        normalized = (data - mean) / std
        if threshold:
            normalized = np.clip(normalized, -threshold, threshold)
        return normalized
    elif method == 'minmax':
        return (data - min(data)) / (max(data) - min(data))
    return data

# Get arguments passed from Ignition
data = args[0]          # First positional argument
sensor_type = args[1]   # Second positional argument
method = kwargs.get('method', 'minmax')
threshold = kwargs.get('threshold', None)

# Execute and set result
result = normalize(data, sensor_type, method, threshold)
```

**Variable Injection**:
- `args` - List of positional arguments
- `kwargs` - Dictionary of keyword arguments
- All other variables from `system.python3.callScript()` kwargs

#### 3. REST API Extension

**New Endpoint**: `/data/python3integration/api/v1/call-script`

**Method**: POST

**Request Body**:
```json
{
  "scriptPath": "Data Processing/normalize_sensor_data",
  "args": [42.5, "temperature"],
  "kwargs": {
    "method": "zscore",
    "threshold": 3.0
  }
}
```

**Response**:
```json
{
  "success": true,
  "result": 1.23,
  "executionTimeMs": 45,
  "timestamp": 1697234567890
}
```

**Error Response**:
```json
{
  "success": false,
  "error": "Script not found: Data Processing/normalize_sensor_data",
  "timestamp": 1697234567890
}
```

#### 4. Script Path Resolution

**Supported Formats**:
- `"My Script"` - Root-level script
- `"Folder/My Script"` - Script in folder
- `"Folder/Subfolder/My Script"` - Nested folders
- `"/Folder/My Script"` - Leading slash optional

**Resolution Logic**:
1. Normalize path (remove leading/trailing slashes, handle multiple slashes)
2. Query script repository for exact match
3. If not found, try case-insensitive match
4. If still not found, return error

#### 5. Error Handling

**Script Not Found**:
```python
# Returns error if script doesn't exist
result = system.python3.callScript("NonExistent/Script")
# Throws: ScriptNotFoundException: Script not found: NonExistent/Script
```

**Execution Error**:
```python
# Returns error if script execution fails
result = system.python3.callScript("Utils/divide", args=[10, 0])
# Throws: PythonExecutionException: ZeroDivisionError: division by zero
```

**No Result Variable**:
```python
# Warns if script doesn't set 'result' variable
result = system.python3.callScript("Utils/print_hello")
# Returns: None (logs warning: "Script did not set 'result' variable")
```

### Technical Implementation

#### 1. New Methods in Python3ScriptModule.java

```java
@ScriptFunction(docBundlePrefix = "Python3ScriptModule")
public Object callScript(String scriptPath,
                        @KeywordArgs({"args", "kwargs"}) Map<String, Object> params)
                        throws IOException {

    // Extract args and kwargs from params
    List<Object> args = (List<Object>) params.getOrDefault("args", new ArrayList<>());
    Map<String, Object> kwargs = (Map<String, Object>) params.getOrDefault("kwargs", new HashMap<>());

    // Load script from repository
    SavedScript script = scriptRepository.loadScript(scriptPath);
    if (script == null) {
        throw new ScriptNotFoundException("Script not found: " + scriptPath);
    }

    // Prepare execution variables
    Map<String, Object> variables = new HashMap<>(kwargs);
    variables.put("args", args);
    variables.put("kwargs", kwargs);

    // Execute script code
    String result = processPool.execute(script.getCode(), variables);

    return result;
}
```

#### 2. New REST Endpoint in Python3RestEndpoints.java

```java
private static JsonObject handleCallScript(RequestContext req, HttpServletResponse res) {
    try {
        // Parse request body
        JsonObject requestBody = JsonParser.parseString(req.getBodyAsString()).getAsJsonObject();
        String scriptPath = requestBody.get("scriptPath").getAsString();

        JsonArray argsArray = requestBody.has("args") ?
            requestBody.getAsJsonArray("args") : new JsonArray();
        JsonObject kwargsObj = requestBody.has("kwargs") ?
            requestBody.getAsJsonObject("kwargs") : new JsonObject();

        // Load script
        SavedScript script = scriptRepository.loadScript(scriptPath);
        if (script == null) {
            return createErrorResponse("Script not found: " + scriptPath);
        }

        // Prepare variables
        Map<String, Object> variables = new HashMap<>();
        variables.put("args", convertJsonArray(argsArray));
        variables.put("kwargs", convertJsonObject(kwargsObj));

        // Execute
        long startTime = System.currentTimeMillis();
        String result = processPool.execute(script.getCode(), variables);
        long executionTime = System.currentTimeMillis() - startTime;

        // Build response
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("result", result);
        response.addProperty("executionTimeMs", executionTime);
        response.addProperty("timestamp", System.currentTimeMillis());

        return response;

    } catch (Exception e) {
        return createErrorResponse(e.getMessage());
    }
}
```

#### 3. Script Path Resolution in Python3ScriptRepository.java

```java
/**
 * Load script by path (supports folder hierarchy)
 *
 * @param scriptPath Path like "Folder/Subfolder/Script Name"
 * @return SavedScript or null if not found
 */
public SavedScript loadScript(String scriptPath) throws IOException {
    // Normalize path: remove leading/trailing slashes
    String normalizedPath = scriptPath.replaceAll("^/+|/+$", "");

    // Try exact match first
    SavedScript script = loadScriptExact(normalizedPath);
    if (script != null) {
        return script;
    }

    // Try case-insensitive match
    return loadScriptCaseInsensitive(normalizedPath);
}
```

#### 4. Modified Classes

**Python3ScriptModule.java**:
- Add `callScript()` method with @ScriptFunction annotation

**Python3RestEndpoints.java**:
- Add `/call-script` endpoint
- Add `handleCallScript()` method

**Python3ScriptRepository.java**:
- Add `loadScript(String path)` method for path-based lookup
- Handle folder hierarchy in path resolution

**Python3ScriptModule.properties** (Documentation):
```properties
callScript.desc=Executes a saved Python 3 script by path and returns the result
callScript.param.scriptPath=Path to saved script (e.g., "Folder/Script Name")
callScript.param.args=List of positional arguments to pass to script
callScript.param.kwargs=Dictionary of keyword arguments to pass to script
callScript.returns=The value of the 'result' variable set by the script
```

### Use Cases

#### 1. Data Transform in Perspective

```python
# Perspective component custom property script transform
def transform(self, value, quality, timestamp):
    # Call Python 3 script to process complex data
    result = system.python3.callScript(
        "Transforms/process_machine_data",
        args=[value],
        kwargs={"machine_id": self.custom.machineId}
    )
    return result
```

#### 2. Gateway Event Script

```python
# Tag Change Event Script
def valueChanged(tag, tagPath, previousValue, currentValue, initialChange, missedEvents):
    # Use Python 3 for advanced analytics
    anomaly_score = system.python3.callScript(
        "Analytics/detect_anomaly",
        args=[currentValue.value],
        kwargs={
            "baseline": system.tag.readBlocking([tagPath + ".Baseline"])[0].value,
            "sensitivity": 0.95
        }
    )

    if anomaly_score > 0.8:
        system.util.sendEmail(
            smtp="localhost",
            fromAddr="alerts@company.com",
            subject="Anomaly Detected",
            body="Anomaly score: " + str(anomaly_score)
        )
```

#### 3. Named Query Post-Processing

```python
# Named query result processing
raw_data = system.db.runNamedQuery("Production/GetSensorReadings")

# Process with Python 3 (pandas, numpy, etc.)
processed_data = system.python3.callScript(
    "Data Processing/aggregate_sensor_data",
    args=[raw_data],
    kwargs={"interval": "1H", "method": "mean"}
)

return processed_data
```

### Testing Plan

1. **Script Path Resolution**
   - Test root-level scripts: `"My Script"`
   - Test nested scripts: `"Folder/Subfolder/Script"`
   - Test case-insensitive matching
   - Test non-existent scripts (verify error)

2. **Argument Passing**
   - Test positional args: `args=[1, 2, 3]`
   - Test keyword args: `kwargs={"x": 10, "y": 20}`
   - Test mixed args and kwargs
   - Test complex objects (lists, dicts, nested structures)

3. **Return Values**
   - Test primitive returns (int, float, string, bool)
   - Test complex returns (list, dict, nested)
   - Test no result variable (verify warning)
   - Test None return

4. **Error Handling**
   - Test script not found
   - Test Python execution error
   - Test timeout handling
   - Test invalid arguments

5. **Integration Tests**
   - Call from Perspective script transform
   - Call from Gateway event script
   - Call from Expression Structure binding
   - Call from Script Console

### Documentation Updates

**Designer IDE Help**:
- Add "Using Saved Scripts in Ignition" section
- Document `system.python3.callScript()` function
- Provide use case examples
- Document script contract (`result` variable requirement)

**README.md Updates**:
- Add to "API Reference" section
- Add to "Examples" section
- Update architecture diagram

---

## 🎯 v2.0.0 - Professional Python IDE

**Status**: Planned
**Release Target**: After v1.10.0
**Effort Estimate**: 1-2 weeks

### Features

#### 1. Intelligent Code Completion
**Implementation**: Jedi + AutoComplete library

**Features**:
- **Static Completions**:
  - Python keywords (def, class, if, for, etc.)
  - Built-in functions (print, len, range, etc.)
  - Standard library modules

- **Dynamic Completions**:
  - Module imports (import requests → suggests installed packages)
  - Object methods (requests. → shows .get, .post, etc.)
  - Function parameters (shows expected arguments)
  - Variable names in scope

**Trigger**: Type `.` or `Ctrl+Space`

**Python Bridge Extension**:
```python
# Add to python_bridge.py
def get_completions(code, line, column):
    """Get code completion suggestions using Jedi"""
    import jedi
    script = jedi.Script(code)
    completions = script.complete(line, column)
    return [{
        "name": c.name,
        "type": c.type,
        "signature": c.get_signatures()[0].to_string() if c.get_signatures() else "",
        "docstring": c.docstring()
    } for c in completions]
```

**Java Side**:
```java
// Use AutoComplete library from RSyntaxTextArea
AutoCompletion ac = new AutoCompletion(provider);
ac.install(codeEditor);
ac.setAutoActivationEnabled(true);
ac.setAutoActivationDelay(300);
```

#### 2. Real-Time Linting & Error Detection
**Implementation**: Python AST parsing + pylint/flake8

**Features**:
- **Syntax Errors**:
  - Red squiggly underlines
  - Error gutter icons (❌)
  - Hover tooltip with error message

- **Code Warnings**:
  - Yellow squiggly underlines
  - Warning gutter icons (⚠️)
  - PEP 8 style violations
  - Unused imports
  - Undefined variables

- **Real-Time Analysis**:
  - Runs on background thread
  - Updates as you type (debounced 500ms)
  - Doesn't block UI

**Python Bridge Extension**:
```python
def lint_code(code):
    """Lint Python code and return errors/warnings"""
    import ast
    import io
    from pylint import lint
    from pylint.reporters.text import TextReporter

    # Syntax check with AST
    errors = []
    try:
        ast.parse(code)
    except SyntaxError as e:
        errors.append({
            "line": e.lineno,
            "column": e.offset,
            "severity": "error",
            "message": e.msg
        })
        return errors

    # Style check with pylint
    pylint_output = io.StringIO()
    reporter = TextReporter(pylint_output)
    lint.Run(['--from-stdin', 'stdin'], reporter=reporter, exit=False)

    # Parse pylint output and add to errors
    # ... (parse and format)

    return errors
```

**Java Side**:
```java
// Add parser and squiggly line painter
RSyntaxTextArea editor = ...;
editor.addParser(new PythonLintParser(restClient));

// PythonLintParser runs in background thread
class PythonLintParser extends AbstractParser {
    public ParseResult parse(RSyntaxDocument doc, String style) {
        // Call Python bridge for linting
        List<ParserNotice> notices = getPythonLintResults(doc.getText());
        return new DefaultParseResult(this, notices);
    }
}
```

#### 3. Advanced Search & Replace
**Implementation**: RSTALanguageSupport + custom dialogs

**Features**:
- **Find Dialog** (Ctrl+F):
  - Search forward/backward
  - Match case toggle
  - Whole word toggle
  - Regex support
  - Highlight all matches

- **Replace Dialog** (Ctrl+H):
  - Replace next
  - Replace all
  - Preview changes
  - Regex capture groups

- **Find in Files** (Ctrl+Shift+F):
  - Search across all saved scripts
  - Show results in dedicated panel
  - Click result to jump to location

**Java Side**:
```java
// Use built-in find/replace from rstaui
SearchContext context = new SearchContext();
context.setSearchFor("requests");
context.setMatchCase(false);
context.setRegularExpression(false);

boolean found = SearchEngine.find(codeEditor, context);
```

#### 4. Multiple Tabs & File Management
**Features**:
- **Tab Bar** above editor
  - Open multiple scripts simultaneously
  - Tab indicators:
    - `*` prefix = unsaved changes
    - `×` button to close
  - Middle-click to close tab
  - Ctrl+W to close current tab

- **Tab Management**:
  - Ctrl+Tab: Next tab
  - Ctrl+Shift+Tab: Previous tab
  - Ctrl+W: Close current tab
  - Ctrl+Shift+T: Reopen closed tab

- **Session Persistence**:
  - Remember open tabs on Designer restart
  - Restore cursor position per file
  - Restore scroll position

**Java Side**:
```java
JTabbedPane editorTabs = new JTabbedPane();
editorTabs.setTabPlacement(JTabbedPane.TOP);
editorTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

// Add tab with close button
editorTabs.addTab(scriptName, new CloseTabButton(), editorPanel);
```

#### 5. Enhanced Output Rendering
**Features**:
- **ANSI Color Support**:
  - Render terminal colors from Python print statements
  - Library: [JANSI](https://github.com/fusesource/jansi)

- **Formatted Tracebacks**:
  - Syntax-highlighted Python tracebacks
  - Clickable file paths (if local)
  - Collapsible stack frames

- **Rich Output Types**:
  - JSON auto-formatting and syntax highlighting
  - Table rendering for pandas DataFrames
  - Image display for matplotlib plots (base64 encoded)
  - HTML rendering for rich outputs

**Python Bridge Extension**:
```python
def format_output(result):
    """Format output with rich content detection"""
    import json

    # Try JSON formatting
    try:
        obj = json.loads(str(result))
        return {
            "type": "json",
            "content": json.dumps(obj, indent=2)
        }
    except:
        pass

    # Detect pandas DataFrame
    if 'pandas' in str(type(result)):
        return {
            "type": "table",
            "content": result.to_html()
        }

    # Fallback to string
    return {
        "type": "text",
        "content": str(result)
    }
```

#### 6. Indentation Guides
**Implementation**: Built into RSyntaxTextArea

**Features**:
- Vertical lines showing indentation levels
- Different colors for each level
- Critical for Python (indentation-sensitive)

**Java Side**:
```java
codeEditor.setPaintTabLines(true);
codeEditor.setTabLineColor(new Color(200, 200, 200, 100));
```

#### 7. Minimap (Code Overview)
**Implementation**: Custom JPanel rendering

**Features**:
- Tiny view of entire code on right edge
- Shows current viewport location
- Click to jump to section
- Highlights errors/warnings

**Java Side**:
```java
class CodeMinimap extends JPanel {
    private RSyntaxTextArea editor;

    @Override
    protected void paintComponent(Graphics g) {
        // Render entire code at tiny scale
        // Highlight visible region
        // Draw error markers
    }
}
```

### Technical Implementation

#### New Classes
1. **PythonLintParser** (extends AbstractParser)
   - Communicates with Python bridge for linting
   - Returns ParserNotice list
   - Runs on background thread

2. **PythonCompletionProvider** (implements CompletionProvider)
   - Provides code completion suggestions
   - Queries Python bridge via Jedi
   - Caches common completions

3. **CodeMinimap** (extends JPanel)
   - Renders entire code at miniature scale
   - Handles click-to-jump navigation

4. **TabCloseButton** (extends JButton)
   - Small × button in tab header
   - Shows tooltip "Close (Ctrl+W)"

5. **OutputFormatter**
   - Detects output type (JSON, table, image, etc.)
   - Renders appropriately in output panel

#### Modified Classes
1. **Python3IDE.java**
   - Replace single editor with JTabbedPane
   - Add find/replace dialogs
   - Add minimap panel
   - Install AutoCompletion
   - Install PythonLintParser

2. **python_bridge.py**
   - Add get_completions() function
   - Add lint_code() function
   - Add format_output() function
   - Install jedi, pylint if not present

#### Dependencies
Add to `designer/build.gradle.kts`:
```kotlin
dependencies {
    implementation("com.fifesoft:rsyntaxtextarea:3.3.4")
    implementation("com.fifesoft:autocomplete:3.3.1")
    implementation("com.fifesoft:rstaui:3.3.1")
    implementation("com.fifesoft:languagesupport:3.3.1")
    implementation("org.fusesource.jansi:jansi:2.4.0")  // ANSI colors
}
```

Python packages (auto-install on first use):
```bash
pip install jedi pylint flake8
```

### Testing Plan
1. **Code Completion**
   - Type `import req` → verify suggestions
   - Type `requests.` → verify method suggestions
   - Test Ctrl+Space manual trigger

2. **Linting**
   - Write code with syntax error → verify red squiggle
   - Write code with style warning → verify yellow squiggle
   - Hover over error → verify tooltip

3. **Find & Replace**
   - Find text with/without regex
   - Replace single occurrence
   - Replace all occurrences
   - Find in all files

4. **Multiple Tabs**
   - Open 3+ scripts
   - Switch between tabs
   - Close tab (verify unsaved warning)
   - Reopen closed tab

5. **Output Formatting**
   - Print JSON → verify formatted
   - Print ANSI colors → verify rendered
   - Return pandas DataFrame → verify table

---

## 🎨 v2.5.0 - Advanced IDE Features

**Status**: Future
**Release Target**: After v2.0.0
**Effort Estimate**: 3-4 weeks

### Features (Brief)
1. **Interactive Debugger**
   - Breakpoints
   - Step through code
   - Variable inspection at breakpoint
   - Using `debugpy` or `pdb`

2. **Variable Inspector Panel**
   - Live variable viewer
   - Tree view for nested structures
   - Type annotations
   - Memory usage

3. **Script Versioning**
   - Auto-save version history
   - Visual diff viewer
   - Rollback capability
   - Timeline slider

4. **Performance Profiler**
   - Line-by-line execution time
   - Memory profiling
   - Hotspot detection
   - Visual flamegraph

---

## 🚢 v3.0.0 - Enterprise Features

**Status**: Future Vision
**Release Target**: TBD
**Effort Estimate**: 2-3 months

### Features (Brief)
1. **Git Integration**
   - Commit scripts
   - Visual diff
   - Branch management
   - Push/pull to remote

2. **Collaborative Editing**
   - Real-time multi-user editing
   - Presence indicators
   - Comments & annotations
   - Share scripts with team

3. **AI Assistant**
   - Claude API integration
   - Code suggestions
   - Explain code
   - Generate docstrings
   - Bug detection

4. **Integrated Terminal**
   - Run shell commands
   - Install pip packages
   - Test Python snippets
   - Multiple terminal tabs

---

## 📊 Progress Tracking

| Version | Status | Features Complete | Estimated Release |
|---------|--------|-------------------|-------------------|
| v1.8.0 | ✅ Released | 100% | 2025-10-16 |
| v1.9.1 | ✅ Released | 100% | 2025-10-16 |
| v1.9.0 | 🔄 In Progress | 40% | Next (2-3 days) |
| v1.10.0 | 📋 Planned | 0% | After v1.9.0 (2-3 days) |
| v2.0.0 | 📋 Planned | 0% | After v1.10.0 (1-2 weeks) |
| v2.5.0 | 🔮 Future | 0% | TBD |
| v3.0.0 | 🔮 Vision | 0% | TBD |

---

## 🎯 Success Metrics

**v1.9.0 Success Criteria**:
- ✅ Scripts organized in folders
- ✅ Syntax highlighting works for Python
- ✅ Dark theme applied successfully
- ✅ Unsaved changes prompt prevents data loss
- ✅ Export/import works for .py files

**v2.0.0 Success Criteria**:
- ✅ Code completion suggests relevant items
- ✅ Linting shows errors in real-time
- ✅ Find/replace works with regex
- ✅ Multiple tabs can be open simultaneously
- ✅ Output formatting handles JSON, tables, colors

---

## 📚 Resources

### Libraries
- [RSyntaxTextArea](https://github.com/bobbylight/RSyntaxTextArea) - Code editor component
- [Jedi](https://jedi.readthedocs.io/) - Python code completion
- [Pylint](https://pylint.org/) - Python linting
- [JANSI](https://github.com/fusesource/jansi) - ANSI color support

### Documentation
- [RSyntaxTextArea Javadoc](https://javadoc.fifesoft.com/rsyntaxtextarea/)
- [Ignition SDK Docs](https://docs.inductiveautomation.com/display/SE/Ignition+SDK+Programmers+Guide)
- [Python AST Module](https://docs.python.org/3/library/ast.html)

---

**Document Version**: 1.0
**Last Updated**: 2025-10-16
**Maintainer**: Development Team
