# Python 3 IDE Plan for Ignition Designer
**Version:** v1.7.0+ Roadmap
**Status:** Planning Phase
**Last Updated:** 2025-10-16

## Executive Summary

This document outlines the plan to create an **IDE-type function** in the Ignition Designer that allows developers to write Python 3 code with execution happening on the Gateway, along with real-time performance diagnostics and feedback.

## Vision

Create a Designer-based Python 3 development environment that provides:
- **Code Editor**: Syntax-highlighted Python 3 code editor in Designer
- **Gateway Execution**: All code execution happens on Gateway (not Designer-side)
- **Real-time Diagnostics**: Live performance metrics, execution time, pool status
- **Interactive Testing**: Run Python code and see results immediately
- **Script Management**: Save, load, and organize Python scripts
- **Error Feedback**: Clear error messages with tracebacks

## Use Cases

### 1. Interactive Python Development
Developer writes Python 3 code in Designer, clicks "Run", and sees results without leaving Designer.

```python
# Developer writes in Designer IDE:
import pandas as pd
import numpy as np

data = {
    'values': [1, 2, 3, 4, 5],
    'squared': [x**2 for x in [1, 2, 3, 4, 5]]
}

df = pd.DataFrame(data)
result = df.describe()
```

**Output Panel Shows:**
```
Execution Time: 45ms
Pool Status: 3 available, 1 in use
Python Version: 3.11.5

Results:
       values   squared
count     5.0      5.00
mean      3.0     11.00
std       1.58     12.08
...
```

### 2. Performance Testing
Developer tests Python code performance with built-in diagnostics:

```python
# Test data processing performance
import time

start = time.time()
result = [x**2 for x in range(1000000)]
elapsed = time.time() - start

print(f"Processed 1M items in {elapsed:.3f}s")
```

**Diagnostics Panel Shows:**
- Execution Time: 156ms
- Memory Usage: 45MB
- Pool Status: Healthy
- Process ID: #2
- Python Version: 3.11.5

### 3. Script Library Management
Developer saves frequently-used Python scripts for reuse:

```
My Python Scripts/
├── Data Processing/
│   ├── CSV Import.py
│   ├── Data Cleaning.py
│   └── Export to Excel.py
├── Calculations/
│   ├── Statistics.py
│   └── Financial.py
└── Testing/
    └── Unit Tests.py
```

## Technical Architecture

### Overview

```
┌─────────────────────────────────────────────────────────┐
│                    Ignition Designer                     │
│                                                          │
│  ┌────────────────────────────────────────────────────┐ │
│  │         Python 3 IDE Component (Swing UI)          │ │
│  │                                                     │ │
│  │  ┌───────────────────────────────────────────────┐ │ │
│  │  │  Code Editor (ExtensionFunctionPanel)         │ │ │
│  │  │  - Syntax highlighting                        │ │ │
│  │  │  - Auto-completion                            │ │ │
│  │  │  - Line numbers                               │ │ │
│  │  └───────────────────────────────────────────────┘ │ │
│  │                                                     │ │
│  │  ┌───────────────────────────────────────────────┐ │ │
│  │  │  Output Panel                                 │ │ │
│  │  │  - Execution results                          │ │ │
│  │  │  - Error messages with tracebacks             │ │ │
│  │  └───────────────────────────────────────────────┘ │ │
│  │                                                     │ │
│  │  ┌───────────────────────────────────────────────┐ │ │
│  │  │  Diagnostics Panel                            │ │ │
│  │  │  - Execution time                             │ │ │
│  │  │  - Pool status (real-time)                    │ │ │
│  │  │  - Python version                             │ │ │
│  │  │  - Memory usage                               │ │ │
│  │  │  - Process ID                                 │ │ │
│  │  └───────────────────────────────────────────────┘ │ │
│  │                                                     │ │
│  │  [ Run ] [ Clear ] [ Save ] [ Load ]               │ │
│  └────────────────────────────────────────────────────┘ │
│                                                          │
│                          │                              │
│                          │ RPC / REST API               │
│                          ▼                              │
└─────────────────────────────────────────────────────────┘
                           │
                           │
┌─────────────────────────────────────────────────────────┐
│                    Ignition Gateway                      │
│                                                          │
│  ┌────────────────────────────────────────────────────┐ │
│  │        Python 3 Integration Module                 │ │
│  │                                                     │ │
│  │  ┌───────────────────────────────────────────────┐ │ │
│  │  │  REST API Endpoints (Ignition 8.3 OpenAPI)    │ │ │
│  │  │  /data/python3integration/api/v1/             │ │ │
│  │  │  - POST /exec (with timing)                   │ │ │
│  │  │  - GET /diagnostics (enhanced)                │ │ │
│  │  │  - GET /pool-stats (real-time)                │ │ │
│  │  └───────────────────────────────────────────────┘ │ │
│  │                                                     │ │
│  │  ┌───────────────────────────────────────────────┐ │ │
│  │  │  Python3ProcessPool (3-5 processes)           │ │ │
│  │  │  - Process #1 [AVAILABLE]                     │ │ │
│  │  │  - Process #2 [IN_USE] ← Current execution    │ │ │
│  │  │  - Process #3 [AVAILABLE]                     │ │ │
│  │  └───────────────────────────────────────────────┘ │ │
│  │                                                     │ │
│  │  ┌───────────────────────────────────────────────┐ │ │
│  │  │  Performance Metrics Collector                │ │ │
│  │  │  - Execution timing                           │ │ │
│  │  │  - Memory profiling                           │ │ │
│  │  │  - Pool utilization stats                     │ │ │
│  │  └───────────────────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

### Communication Layers

1. **Designer → Gateway**: RPC or REST API calls
2. **Gateway Execution**: Python3ProcessPool handles execution
3. **Gateway → Designer**: Results + diagnostics returned
4. **Real-time Updates**: WebSocket or polling for live pool stats

## Implementation Phases

### Phase 1: Designer UI Component (v1.7.0)

**Goal**: Create basic Python IDE in Designer

**Components to Build**:

1. **Python3IDE.java** - Main IDE container
   ```java
   public class Python3IDE extends JPanel {
       private ExtensionFunctionPanel codeEditor;
       private JTextArea outputPanel;
       private JPanel diagnosticsPanel;
       private DesignerContext designerContext;
       private Python3RPC rpc;
   }
   ```

2. **Enable Designer Scope** in `build.gradle.kts`:
   ```kotlin
   projectScopes.putAll(
       mapOf(
           ":common" to "G",
           ":gateway" to "G",
           ":designer" to "D"   // Enable Designer scope
       )
   )
   ```

3. **DesignerHook.java** - Designer lifecycle management
   ```java
   public class DesignerHook extends AbstractDesignerModuleHook {
       @Override
       public void startup(LicenseState licenseState) {
           // Initialize RPC connection to Gateway
           rpc = ModuleRPCFactory.create(
               Constants.MODULE_ID,
               Python3RpcFunctions.class
           );

           // Add Tools menu item
           addToolsMenuItem();
       }
   }
   ```

4. **Add Tools Menu Item**:
   ```java
   private void addToolsMenuItem() {
       context.addMenuAction(
           "Python 3 IDE",
           KeyStroke.getKeyStroke(KeyEvent.VK_P,
               KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK),
           () -> showPython3IDE()
       );
   }
   ```

**Features**:
- ✅ Code editor with Python syntax highlighting
- ✅ "Run" button to execute code on Gateway
- ✅ Output panel for results
- ✅ Basic error handling
- ✅ RPC communication with Gateway

**Testing Criteria**:
- Can open Python IDE from Designer Tools menu
- Can write Python code in editor
- Can execute code and see results
- Errors display clearly in output panel

### Phase 2: Enhanced Diagnostics (v1.7.1)

**Goal**: Add real-time performance diagnostics

**Components to Build**:

1. **DiagnosticsPanel.java** - Real-time diagnostics display
   ```java
   public class DiagnosticsPanel extends JPanel {
       private JLabel executionTimeLabel;
       private JLabel poolStatusLabel;
       private JLabel pythonVersionLabel;
       private JLabel memoryUsageLabel;
       private Timer updateTimer;
   }
   ```

2. **Enhance REST API** - Add timing to exec endpoint:
   ```java
   private static JsonObject handleExec(RequestContext req, HttpServletResponse res) {
       long startTime = System.nanoTime();

       try {
           Object result = scriptModule.exec(code, variables);
           long executionTime = (System.nanoTime() - startTime) / 1_000_000; // ms

           JsonObject response = new JsonObject();
           response.addProperty("success", true);
           response.addProperty("result", result != null ? result.toString() : null);
           response.addProperty("executionTimeMs", executionTime);
           response.addProperty("processId", getUsedProcessId());
           response.addProperty("timestamp", System.currentTimeMillis());

           return response;
       } catch (Exception e) {
           // ... error handling
       }
   }
   ```

3. **Real-time Pool Status Updates**:
   - Option A: Polling (every 1-2 seconds) - Simple
   - Option B: WebSocket (push updates) - More complex but better UX
   - Option C: SSE (Server-Sent Events) - Middle ground

**Features**:
- ✅ Execution time display (ms)
- ✅ Pool status indicator (X available, Y in use)
- ✅ Python version display
- ✅ Process ID that executed code
- ✅ Timestamp of execution
- ✅ Auto-refresh pool stats

**Testing Criteria**:
- Diagnostics update after each execution
- Pool status reflects real-time state
- Long-running code shows intermediate updates

### Phase 3: Script Management (v1.7.2)

**Goal**: Save, load, and organize Python scripts

**Components to Build**:

1. **Python3Script Resource Type**:
   ```java
   public class Python3Script implements Resource {
       private String name;
       private String code;
       private Map<String, Object> metadata;
       private long lastModified;
   }
   ```

2. **Script Library Panel**:
   ```java
   public class ScriptLibraryPanel extends JPanel {
       private JTree scriptTree;
       private DefaultMutableTreeNode rootNode;

       // File operations
       public void saveScript(String name, String code);
       public String loadScript(String name);
       public void deleteScript(String name);
       public void createFolder(String folderName);
   }
   ```

3. **Integration with Project Resources**:
   - Scripts saved in Designer project under `scripts/python3/`
   - Synced across Designer instances
   - Version control friendly (plain text)

**Features**:
- ✅ Save scripts with names
- ✅ Load saved scripts
- ✅ Organize in folders
- ✅ Recent scripts list
- ✅ Quick search/filter
- ✅ Import/export scripts

**Testing Criteria**:
- Scripts persist across Designer sessions
- Folder organization works
- Search finds scripts quickly
- Import/export maintains code integrity

### Phase 4: Advanced IDE Features (v1.8.0)

**Goal**: Professional IDE experience

**Components to Build**:

1. **Code Completion**:
   - Python standard library completion
   - Variable completion from context
   - Import statement assistance

2. **Error Highlighting**:
   - Real-time syntax checking
   - Inline error markers
   - Hover tooltips for errors

3. **Variable Inspector**:
   ```java
   public class VariableInspectorPanel extends JPanel {
       private JTable variableTable;

       // Shows variables in scope after execution
       public void displayVariables(Map<String, Object> variables);
   }
   ```

4. **Execution History**:
   - Keep history of executed code
   - Re-run previous executions
   - Compare results

5. **Performance Profiling**:
   - Line-by-line execution timing
   - Memory usage per operation
   - Call graph visualization

**Features**:
- ✅ Auto-completion (Ctrl+Space)
- ✅ Error squiggles in editor
- ✅ Variable inspector panel
- ✅ Execution history with replay
- ✅ Performance profiler
- ✅ Code snippets library

**Testing Criteria**:
- Auto-completion suggests correct functions
- Error highlighting identifies issues before run
- Variable inspector shows all variables
- History can replay past executions

## API Enhancements Required

### Enhanced Exec Endpoint

**Current**: `/data/python3integration/api/v1/exec`

**Enhanced Response**:
```json
{
  "success": true,
  "result": "...",
  "executionTimeMs": 45,
  "processId": 2,
  "timestamp": 1760571024031,
  "metrics": {
    "memoryUsedBytes": 4567890,
    "cpuTimeMs": 42,
    "ioTimeMs": 3
  },
  "variables": {
    "x": 10,
    "result": 100,
    "data": "[DataFrame with 100 rows]"
  }
}
```

### New Diagnostics Streaming Endpoint

**New**: `/data/python3integration/api/v1/diagnostics/stream`

**Purpose**: Server-Sent Events (SSE) for real-time pool status

**Response** (every 1 second):
```
event: pool-status
data: {"available": 3, "inUse": 0, "healthy": 3, "totalSize": 3}

event: pool-status
data: {"available": 2, "inUse": 1, "healthy": 3, "totalSize": 3}

event: pool-status
data: {"available": 3, "inUse": 0, "healthy": 3, "totalSize": 3}
```

### Enhanced RPC Functions

**Add to Python3RpcFunctions.java**:

```java
public interface Python3RpcFunctions {
    // Existing methods...
    Object exec(String code, Map<String, Object> variables);
    Object eval(String expression, Map<String, Object> variables);
    // ...

    // New methods for IDE:

    /**
     * Execute code with enhanced diagnostics
     */
    Python3ExecutionResult execWithDiagnostics(String code, Map<String, Object> variables);

    /**
     * Get real-time pool status
     */
    PoolStatus getPoolStatus();

    /**
     * Validate Python syntax without executing
     */
    SyntaxCheckResult checkSyntax(String code);

    /**
     * Get available Python modules
     */
    List<String> getAvailableModules();

    /**
     * Get function signatures for auto-completion
     */
    List<FunctionSignature> getFunctionSignatures(String module);
}
```

## UI/UX Design

### IDE Window Layout

```
┌─────────────────────────────────────────────────────────────────┐
│  Python 3 IDE                                          [_][□][X] │
├─────────────────────────────────────────────────────────────────┤
│  File  Edit  Run  Tools  Help                                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────────┐  ┌──────────────────────────────────────┐  │
│  │ Script Library │  │  Code Editor                         │  │
│  │                │  │  ─────────────────────────────────── │  │
│  │ □ Recent       │  │   1  import pandas as pd             │  │
│  │ □ Data         │  │   2  import numpy as np               │  │
│  │   ├─ Import    │  │   3                                   │  │
│  │   └─ Export    │  │   4  data = {                        │  │
│  │ □ Calc         │  │   5      'x': [1, 2, 3, 4, 5],      │  │
│  │ □ Testing      │  │   6      'y': [2, 4, 6, 8, 10]      │  │
│  │                │  │   7  }                               │  │
│  │ [New] [Save]   │  │   8                                   │  │
│  │ [Load] [Del]   │  │   9  df = pd.DataFrame(data)         │  │
│  │                │  │  10  result = df.describe()          │  │
│  │                │  │  11  print(result)                    │  │
│  └────────────────┘  │                                       │  │
│                      │  [ Run ] [ Clear ] [ Stop ]           │  │
│                      └──────────────────────────────────────┘  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Output                                                    │  │
│  │ ────────────────────────────────────────────────────────  │  │
│  │              x          y                                 │  │
│  │ count  5.000000   5.000000                               │  │
│  │ mean   3.000000   6.000000                               │  │
│  │ std    1.581139   3.162278                               │  │
│  │ min    1.000000   2.000000                               │  │
│  │ ...                                                       │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Diagnostics                                               │  │
│  │ ────────────────────────────────────────────────────────  │  │
│  │ Execution Time: 45ms     Process: #2                     │  │
│  │ Pool Status: ●●○ (2 available, 1 in use)                │  │
│  │ Python: 3.11.5           Memory: 4.5 MB                  │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
│  Status: Ready                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Visual Design Elements

**Color Scheme**:
- Code Editor: Dark theme (optional light theme)
- Output Panel: Monospace font, clear background
- Diagnostics: Green (healthy), Yellow (busy), Red (error)

**Icons**:
- ▶ Run button (green triangle)
- ◼ Stop button (red square)
- 🗑 Clear button
- 💾 Save button
- 📂 Load button

**Status Indicators**:
- ●●● Pool Status (filled = available, empty = in use)
- ✓ Execution successful (green checkmark)
- ✗ Execution failed (red X)
- ⏱ Execution time badge

## Technical Considerations

### Performance

**Code Editor**:
- Use ExtensionFunctionPanel (built-in, optimized)
- Syntax highlighting via RSyntaxTextArea
- Lazy loading for large scripts

**Real-time Updates**:
- Polling interval: 1-2 seconds for pool status
- Throttle updates during rapid executions
- Cache diagnostic data

**Memory Management**:
- Limit output panel size (max 10,000 lines)
- Clear old execution history (keep last 50)
- Stream large results instead of loading all

### Security

**Input Validation**:
- Sanitize code before sending to Gateway
- Size limits on code submissions (max 1MB)
- Rate limiting on executions (max 100/minute)

**Access Control**:
- Designer IDE respects Gateway permissions
- Can't execute if user lacks permissions
- Audit log of IDE executions

### Error Handling

**Network Errors**:
- Detect lost Gateway connection
- Show reconnect UI
- Queue executions for retry

**Python Errors**:
- Display full traceback
- Highlight error line in editor
- Suggest fixes for common errors

**Gateway Errors**:
- Pool exhaustion: Show wait indicator
- Process crash: Retry automatically
- Timeout: Allow user to cancel

## Migration Path

### v1.6.1 → v1.7.0 (Phase 1)

**Changes Required**:
1. Enable Designer scope in `build.gradle.kts`
2. Create `designer/` subproject
3. Implement `DesignerHook.java`
4. Implement `Python3IDE.java` (basic UI)
5. Enable RPC in `GatewayHook.java`
6. Test Designer → Gateway communication

**Testing**:
- Install v1.7.0 in test Gateway
- Open Designer
- Open Python 3 IDE from Tools menu
- Execute simple Python code
- Verify results display correctly

**Rollback Plan**:
- If Designer issues occur, users can:
  - Continue using Gateway scripts
  - Use REST API directly
  - Revert to v1.6.1 if needed

### v1.7.0 → v1.7.1 (Phase 2)

**Changes Required**:
1. Enhance `/exec` endpoint with timing
2. Add `DiagnosticsPanel.java`
3. Implement pool status polling
4. Display metrics in UI

**Testing**:
- Execute code and verify timing accuracy
- Check pool status updates in real-time
- Test with multiple concurrent executions

### v1.7.1 → v1.7.2 (Phase 3)

**Changes Required**:
1. Create `Python3Script` resource type
2. Implement `ScriptLibraryPanel.java`
3. Add save/load functionality
4. Integrate with Designer project

**Testing**:
- Save scripts and verify persistence
- Load scripts across Designer restarts
- Test folder organization
- Verify scripts sync across Designers

### v1.7.2 → v1.8.0 (Phase 4)

**Changes Required**:
1. Add auto-completion engine
2. Implement syntax checking
3. Create variable inspector
4. Build execution history
5. Add performance profiler

**Testing**:
- Test auto-completion accuracy
- Verify syntax checking detects errors
- Check variable inspector displays all vars
- Test history replay functionality

## Success Criteria

### Phase 1 Success
- ✅ Can open IDE from Designer Tools menu
- ✅ Can write and execute Python 3 code
- ✅ Results display correctly
- ✅ Errors show with tracebacks
- ✅ No Designer lockups or crashes

### Phase 2 Success
- ✅ Execution time displays accurately
- ✅ Pool status updates in real-time
- ✅ Diagnostics panel shows all metrics
- ✅ Performance is acceptable (<100ms update latency)

### Phase 3 Success
- ✅ Scripts save and load reliably
- ✅ Folder organization works intuitively
- ✅ Search finds scripts quickly
- ✅ Import/export works correctly

### Phase 4 Success
- ✅ Auto-completion feels responsive
- ✅ Syntax checking catches errors
- ✅ Variable inspector shows all variables
- ✅ History replay works correctly
- ✅ IDE feels like professional tool

## Timeline Estimate

### Phase 1: Designer UI Component
**Effort**: 40-60 hours
**Timeline**: 2-3 weeks

**Breakdown**:
- Enable Designer scope: 4 hours
- Create DesignerHook: 6 hours
- Build Python3IDE UI: 16 hours
- Implement RPC communication: 10 hours
- Testing and debugging: 12 hours
- Documentation: 8 hours

### Phase 2: Enhanced Diagnostics
**Effort**: 24-32 hours
**Timeline**: 1-2 weeks

**Breakdown**:
- Enhance REST API: 8 hours
- Build DiagnosticsPanel: 10 hours
- Implement polling/streaming: 8 hours
- Testing and debugging: 6 hours

### Phase 3: Script Management
**Effort**: 32-40 hours
**Timeline**: 1-2 weeks

**Breakdown**:
- Create Python3Script resource: 8 hours
- Build ScriptLibraryPanel: 12 hours
- Implement save/load: 8 hours
- Testing and debugging: 8 hours

### Phase 4: Advanced IDE Features
**Effort**: 60-80 hours
**Timeline**: 3-4 weeks

**Breakdown**:
- Auto-completion engine: 20 hours
- Syntax checking: 12 hours
- Variable inspector: 10 hours
- Execution history: 10 hours
- Performance profiler: 16 hours
- Testing and debugging: 16 hours

**Total Estimated Effort**: 156-212 hours (4-6 weeks of full-time work)

## Risks and Mitigations

### Risk 1: Designer Lockups (Experienced in v1.2.x)

**Mitigation**:
- All code execution via RPC/REST (non-blocking)
- Use SwingWorker for background tasks
- Implement proper timeout handling
- Add cancel button for long executions

### Risk 2: Performance Issues with Large Results

**Mitigation**:
- Limit output panel size
- Stream large results
- Add pagination for datasets
- Implement "show more" functionality

### Risk 3: RPC Connection Instability

**Mitigation**:
- Implement reconnect logic
- Cache last known state
- Queue operations during disconnect
- Show clear connection status

### Risk 4: Complex UI Development

**Mitigation**:
- Use existing Ignition UI components
- Start with ExtensionFunctionPanel
- Iterate based on user feedback
- Keep UI simple in Phase 1

## Next Steps

### Immediate Actions (Before Starting Phase 1)

1. **User Approval**: Get approval on this plan
2. **Update Documentation**: Save this plan in docs/
3. **Research**: Review ExtensionFunctionPanel API in detail
4. **Prototype**: Create simple proof-of-concept
5. **Test Environment**: Set up Designer development environment

### Decision Points

**Before Phase 1**:
- Confirm Designer scope approach (RPC vs REST vs hybrid)
- Decide on UI framework (Swing vs JavaFX)
- Choose code editor component

**Before Phase 2**:
- Decide on real-time update mechanism (polling vs SSE vs WebSocket)
- Determine diagnostic metrics to collect

**Before Phase 3**:
- Decide on script storage approach (project resources vs database)
- Plan folder structure

**Before Phase 4**:
- Prioritize advanced features
- Get user feedback on Phase 1-3

## Conclusion

This plan provides a comprehensive roadmap for building a **Python 3 IDE in Ignition Designer** with:
- ✅ Gateway-based execution (no Designer-side Python)
- ✅ Real-time performance diagnostics
- ✅ Professional IDE features
- ✅ Phased implementation approach
- ✅ Clear success criteria
- ✅ Risk mitigation strategies

The phased approach allows for:
- Early user feedback
- Incremental value delivery
- Risk reduction
- Course correction opportunities

**Estimated Timeline**: 4-6 weeks for full implementation (all 4 phases)

**Minimum Viable Product (MVP)**: Phase 1 only (2-3 weeks)

---

**Ready to proceed?** Awaiting approval to begin Phase 1 implementation.
