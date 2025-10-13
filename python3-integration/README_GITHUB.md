# Python 3 Integration Module for Ignition 8.3

A self-contained module that brings Python 3.11 to Ignition, with automatic installation and no manual setup required.

🚀 **Auto-downloads Python 3** on first use  
⚡ **Process pool** for fast execution  
🔧 **Self-contained** - no separate installation needed  
🌍 **Cross-platform** - Windows, Linux, macOS

## Quick Start

1. Install the `.modl` file in Ignition Gateway
2. Use Python 3 in your scripts:

```python
# Large numbers beyond Jython capability
result = system.python3.eval('2 ** 1000')

# Use Python 3 libraries  
result = system.python3.callModule('math', 'sqrt', [144])
```

See [README.md](README.md) for full documentation.

