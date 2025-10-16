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

## 🎯 v2.0.0 - Professional Python IDE

**Status**: Planned
**Release Target**: After v1.9.0
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
| v1.9.0 | 🔄 In Progress | 0% | Next (3-5 days) |
| v2.0.0 | 📋 Planned | 0% | After v1.9.0 (1-2 weeks) |
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
