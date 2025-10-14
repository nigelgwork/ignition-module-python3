# Python 3 Integration Architecture

## Overview

This module enables Python 3 execution in Ignition through a **subprocess process pool** approach, providing a bridge between Ignition's Jython 2.7 environment and modern Python 3.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                      Ignition Gateway                           │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │              Ignition Script (Jython 2.7)               │  │
│  │                                                          │  │
│  │  result = system.python3.exec("import pandas...")       │  │
│  └────────────────────┬─────────────────────────────────────┘  │
│                       │                                         │
│                       ▼                                         │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │           Python3ScriptModule (Java)                    │  │
│  │  - @ScriptFunction annotations                          │  │
│  │  - exec(), eval(), callModule()                         │  │
│  └────────────────────┬─────────────────────────────────────┘  │
│                       │                                         │
│                       ▼                                         │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │           Python3ProcessPool (Java)                     │  │
│  │  - Maintains 3-5 warm Python processes                  │  │
│  │  - Thread-safe borrowing/returning                      │  │
│  │  - Health checking & auto-restart                       │  │
│  │  - Blocking queue for availability                      │  │
│  └────────────────────┬─────────────────────────────────────┘  │
│                       │                                         │
│                       ▼                                         │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │           Python3Executor (Java)                        │  │
│  │  - Manages single Python process                        │  │
│  │  - stdin/stdout JSON communication                      │  │
│  │  - Timeout handling                                     │  │
│  │  - Error processing                                     │  │
│  └────────────────────┬─────────────────────────────────────┘  │
│                       │                                         │
└───────────────────────┼─────────────────────────────────────────┘
                        │ JSON over stdin/stdout
                        │
          ┌─────────────┴─────────────┬─────────────┬───────────┐
          ▼                           ▼             ▼           ▼
    ┌──────────┐              ┌──────────┐    ┌──────────┐  (more...)
    │ Python 3 │              │ Python 3 │    │ Python 3 │
    │ Process  │              │ Process  │    │ Process  │
    │    #1    │              │    #2    │    │    #3    │
    └─────┬────┘              └─────┬────┘    └─────┬────┘
          │                         │               │
          ▼                         ▼               ▼
    ┌──────────┐              ┌──────────┐    ┌──────────┐
    │ python_  │              │ python_  │    │ python_  │
    │ bridge.py│              │ bridge.py│    │ bridge.py│
    └──────────┘              └──────────┘    └──────────┘
```

## Component Details

### 1. GatewayHook
**Location**: `gateway/GatewayHook.java`

**Responsibilities:**
- Module lifecycle management (setup, startup, shutdown)
- Python path configuration and auto-detection
- Process pool initialization
- Script manager registration

**Key Methods:**
- `setup()`: Early initialization, load configuration
- `startup()`: Create process pool
- `shutdown()`: Clean shutdown of all processes
- `initializeScriptManager()`: Register scripting functions

### 2. Python3ProcessPool
**Location**: `gateway/Python3ProcessPool.java`

**Responsibilities:**
- Maintain pool of N Python processes (default: 3)
- Thread-safe process borrowing/returning
- Health monitoring with scheduled checks
- Automatic process replacement on failure
- Pool statistics

**Key Features:**
```java
// Borrow process (blocking with timeout)
Python3Executor executor = pool.borrowExecutor(30, TimeUnit.SECONDS);

// Execute and return
try {
    result = executor.execute(code, variables);
} finally {
    pool.returnExecutor(executor);
}

// Convenience methods (handle borrow/return)
pool.execute(code, variables);
pool.evaluate(expression, variables);
pool.callModule(module, function, args, kwargs);
```

**Health Checking:**
- Runs every 30 seconds
- Pings each process
- Replaces unhealthy processes
- Logs health issues

### 3. Python3Executor
**Location**: `gateway/Python3Executor.java`

**Responsibilities:**
- Manage single Python subprocess
- JSON-based request/response protocol
- Timeout enforcement
- Stream management
- Process lifecycle

**Communication Flow:**
```
1. Extract python_bridge.py to temp file
2. Start: python3 -u python_bridge.py
3. Wait for "ready" signal
4. Send JSON request via stdin
5. Read JSON response from stdout (with timeout)
6. Parse and return result
```

**Request Format:**
```json
{
  "command": "execute",
  "code": "result = 2 + 2",
  "variables": {}
}
```

**Response Format:**
```json
{
  "success": true,
  "result": 4
}
```

Or on error:
```json
{
  "success": false,
  "error": "ZeroDivisionError: division by zero",
  "traceback": "Traceback (most recent call last)..."
}
```

### 4. Python3ScriptModule
**Location**: `gateway/Python3ScriptModule.java`

**Responsibilities:**
- Expose Python 3 functions to Ignition scripts
- Parameter validation
- Error handling and logging
- Type conversion

**Functions Exposed:**
```java
@ScriptFunction
public Object exec(String code, Map<String, Object> variables)

@ScriptFunction
public Object eval(String expression, Map<String, Object> variables)

@ScriptFunction
public Object callModule(String module, String function, List args, Map kwargs)

@ScriptFunction
public boolean isAvailable()

@ScriptFunction
public Map<String, Object> getVersion()

@ScriptFunction
public Map<String, Object> getPoolStats()

@ScriptFunction
public String example()
```

### 5. python_bridge.py
**Location**: `gateway/src/main/resources/python_bridge.py`

**Responsibilities:**
- Python-side request handler
- Code execution in isolated scope
- Module importing and function calling
- Error capture and serialization
- Clean JSON responses

**Command Types:**
- `execute`: Execute code block, return 'result' variable
- `evaluate`: Evaluate expression, return value
- `call_module`: Import module and call function
- `version`: Return Python version info
- `list_modules`: List installed packages
- `clear_globals`: Clear global variable scope
- `ping`: Health check response
- `shutdown`: Graceful shutdown

**Key Features:**
```python
class PythonBridge:
    def __init__(self):
        self.globals_dict = {}  # Persistent scope

    def execute_code(self, code, variables):
        exec_globals = self.globals_dict.copy()
        exec_globals.update(variables)
        exec(code, exec_globals, exec_locals)
        # ...

    def run(self):
        # Line-based JSON protocol
        while True:
            line = sys.stdin.readline()
            request = json.loads(line)
            response = self.process_request(request)
            sys.stdout.write(json.dumps(response) + '\n')
            sys.stdout.flush()
```

## Data Flow

### Example: `system.python3.eval("2 ** 100")`

```
1. Jython script calls: system.python3.eval("2 ** 100")
   ↓
2. Python3ScriptModule.eval() invoked
   ↓
3. ProcessPool.evaluate() called
   ↓
4. Borrow executor from pool (wait if none available)
   ↓
5. Python3Executor.evaluate() sends JSON:
   {"command": "evaluate", "expression": "2 ** 100", "variables": {}}
   ↓
6. python_bridge.py receives, evaluates, responds:
   {"success": true, "result": 1267650600228229401496703205376}
   ↓
7. Python3Executor parses response
   ↓
8. Return executor to pool
   ↓
9. Return result to Jython: 1267650600228229401496703205376
```

## Thread Safety

### Pool Level
- `BlockingQueue<Python3Executor>` for available executors
- Thread-safe borrow/return operations
- Synchronized process replacement

### Executor Level
- `synchronized (executionLock)` for command execution
- Only one command per executor at a time
- Multiple executors = concurrent execution

### Example: 5 Concurrent Calls, Pool Size = 3
```
Call 1: Borrows Executor #1 → executes
Call 2: Borrows Executor #2 → executes
Call 3: Borrows Executor #3 → executes
Call 4: Waits for available executor
Call 5: Waits for available executor

Call 1 completes → Returns Executor #1 → Call 4 borrows #1
Call 2 completes → Returns Executor #2 → Call 5 borrows #2
```

## Error Handling

### Process Crashes
1. Health check detects unresponsive process
2. Old process shutdown attempted
3. New process started
4. New process added to pool
5. Old process removed

### Communication Timeout
1. Request sent, timeout timer started
2. If no response within timeout → cancel read
3. Mark executor as unhealthy
4. Return to pool (will be replaced by health check)

### Python Exceptions
1. Exception raised in Python code
2. python_bridge.py catches exception
3. Formats error + traceback
4. Returns: `{"success": false, "error": "...", "traceback": "..."}`
5. Java throws RuntimeException with full details

## Configuration

### Python Path Priority
1. System property: `-Dignition.python3.path=/path/to/python`
2. Environment variable: `IGNITION_PYTHON3_PATH`
3. Auto-detection (OS-specific paths)
4. Default: `python3`

### Pool Size
- System property: `-Dignition.python3.poolsize=5`
- Default: 3 processes

### Timeouts
- Command execution: 30 seconds (hardcoded in Executor)
- Process startup: 5 seconds (hardcoded)
- Graceful shutdown: 5 seconds (hardcoded)
- Health check interval: 30 seconds (hardcoded)

## Performance Characteristics

### First Call Overhead
- Process already running: ~10-50ms
- No startup overhead (unlike pure subprocess approach)

### Subsequent Calls
- Process pool hit: ~10-50ms
- Mostly serialization/deserialization overhead
- Python execution time + communication latency

### Concurrent Load
- Max concurrent: Pool size (default 3)
- Beyond max: Requests wait (30s timeout)
- Queue depth: Unlimited (memory bound)

### Memory Usage
- Each Python process: ~50-100MB base
- Plus imported libraries (numpy, pandas, etc.)
- Java overhead: Minimal (<10MB)

## Security Considerations

### Code Execution
- **No sandboxing by default**
- Python code runs with Gateway process permissions
- Can access filesystem, network, etc.

**Mitigation strategies:**
- Run Ignition Gateway under restricted user
- Use firewall to limit network access
- Mount filesystems read-only where possible
- Implement custom whitelist/validation in ScriptModule

### Resource Limits
- No CPU/memory limits enforced
- Runaway scripts can consume resources
- Timeout provides some protection (30s)

**Mitigation strategies:**
- Monitor with health checks
- Use OS-level resource limits (cgroups on Linux)
- Implement custom resource monitoring

### Input Validation
- JSON serialization provides some protection
- Malicious code can still be executed
- Module import restrictions not enforced

## Extension Points

### Custom Commands
Add new command types to python_bridge.py:

```python
def process_request(self, request):
    command = request.get('command')

    if command == 'my_custom_command':
        return self.my_custom_handler(request)
    # ...
```

### Custom Serialization
Override `_serialize()` method for custom object handling:

```python
def _serialize(self, obj):
    if isinstance(obj, MyCustomClass):
        return obj.to_dict()
    # ... existing code
```

### Designer Scope
Add Designer hook to expose functions in Designer:

```java
public class DesignerHook extends AbstractDesignerModuleHook {
    @Override
    public void initializeScriptManager(ScriptManager manager) {
        // Register same functions for Designer scripts
        manager.addScriptModule("system.python3", ...);
    }
}
```

## Testing Strategy

### Unit Tests
- Test Python3Executor communication
- Test process pool borrowing/returning
- Test error handling

### Integration Tests
- Test scripting functions in Ignition
- Test with various Python versions
- Test concurrent load

### Manual Testing
```python
# In Ignition Script Console
system.python3.example()
system.python3.getVersion()
system.python3.getPoolStats()
```

## Troubleshooting

### Debug Logging
Add to GatewayHook.java:
```java
logger.setLevel(org.slf4j.event.Level.DEBUG);
```

View logs:
```bash
tail -f <ignition>/logs/wrapper.log | grep Python3
```

### Process Inspection
Check running Python processes:
```bash
# Linux/Mac
ps aux | grep python_bridge

# Windows
tasklist | findstr python
```

### Test Python Path
```bash
# Test configured path
/path/to/python3 --version
/path/to/python3 -c "import sys; print(sys.executable)"
```

## Future Enhancements

### Virtual Environment Support
```java
// Detect and activate venv
if (new File(pythonPath, "pyvenv.cfg").exists()) {
    // Set VIRTUAL_ENV
    pb.environment().put("VIRTUAL_ENV", venvPath);
}
```

### Binary Data Support
```java
// Use base64 encoding for bytes
{
  "result": {
    "type": "bytes",
    "data": "base64encodeddata..."
  }
}
```

### Streaming Results
```java
// For large datasets, stream results
interface StreamCallback {
    void onChunk(Object chunk);
}
```

### Multiple Python Versions
```java
// Pool per Python version
Map<String, Python3ProcessPool> pools;
pools.put("python3.9", new Python3ProcessPool("/usr/bin/python3.9", 3));
pools.put("python3.11", new Python3ProcessPool("/usr/bin/python3.11", 3));
```
