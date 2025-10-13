# Python 3 Integration Module for Ignition

This module enables Python 3 scripting functions in Ignition 8.3+, allowing you to use modern Python 3 features and libraries alongside Ignition's built-in Jython 2.7 environment.

## Features

- **Python 3 Execution**: Execute Python 3 code from Ignition scripts
- **Process Pool**: Efficient subprocess pooling for minimal overhead
- **Full Python 3 Library Support**: Use numpy, pandas, requests, and any Python 3 package
- **Simple API**: Easy-to-use scripting functions under `system.python3.*`
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

# Module will be in: build/libs/python3-integration-1.0.0-SNAPSHOT.modl
```

## Installation

1. Open Ignition Gateway web interface (http://localhost:8088)
2. Navigate to **Config → System → Modules**
3. Scroll to bottom and click **Install or Upgrade a Module**
4. Select the `.modl` file
5. Click **Install**
6. Module status should show **Running**

## Usage

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
├── common/                  # Shared code
├── gateway/                 # Gateway scope
│   └── src/main/
│       ├── java/           # Java source
│       └── resources/      # Python bridge script
├── build.gradle.kts        # Root build file
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

Future enhancements:

- [ ] Virtual environment support
- [ ] Package manager UI in Gateway
- [ ] Designer integration (Python 3 script editor)
- [ ] Async/callback support
- [ ] Binary data handling (bytes, numpy arrays)
- [ ] Streaming results for large datasets
- [ ] Python process resource limits (CPU, memory)
- [ ] Multiple Python versions support

## Examples

See the usage examples above for common Python 3 integration patterns. Additional examples can be found in the module documentation and test scripts.

## Credits

Built using the Ignition SDK:
- https://github.com/inductiveautomation/ignition-sdk-examples
- https://www.sdk-docs.inductiveautomation.com/

## Changelog

### 1.0.0 (Initial Release)
- Python 3 subprocess execution
- Process pool management
- Basic scripting functions
- Auto-detection of Python path
- Health checking and auto-restart
