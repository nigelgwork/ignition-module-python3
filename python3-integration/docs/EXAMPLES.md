# Python 3 Integration Examples

## Basic Usage

### Hello World

```python
# Simplest example - evaluate an expression
result = system.python3.eval("'Hello from Python 3!'")
print(result)
```

### Large Number Calculations

```python
# Beyond Jython's integer limits
huge_number = system.python3.eval("2 ** 1000")
print("2^1000 =", huge_number)

# Factorial
code = """
import math
result = math.factorial(100)
"""
factorial = system.python3.exec(code)
print("100! =", factorial)
```

## Working with Data

### JSON Processing

```python
code = """
import json

data = {
    'sensors': [
        {'id': 1, 'value': 25.5},
        {'id': 2, 'value': 30.2},
        {'id': 3, 'value': 28.7}
    ]
}

result = json.dumps(data, indent=2)
"""

json_str = system.python3.exec(code)
print(json_str)
```

### List Operations

```python
code = """
data = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]

# Filter even numbers and square them
result = [x**2 for x in data if x % 2 == 0]
"""

squares = system.python3.exec(code)
print("Even squares:", squares)
```

### Dictionary Operations

```python
code = """
tags = {
    'Pump1/Speed': 1500,
    'Pump2/Speed': 1750,
    'Tank1/Level': 75.5,
    'Tank2/Level': 82.3
}

# Filter by type
pumps = {k: v for k, v in tags.items() if 'Pump' in k}
tanks = {k: v for k, v in tags.items() if 'Tank' in k}

result = {
    'pumps': pumps,
    'tanks': tanks
}
"""

organized = system.python3.exec(code)
print("Pumps:", organized['pumps'])
print("Tanks:", organized['tanks'])
```

## Using Variables

### Pass Data from Ignition

```python
# Get tag values in Ignition
temp = system.tag.readBlocking(['[default]Temperature'])[0].value
pressure = system.tag.readBlocking(['[default]Pressure'])[0].value

# Process in Python 3
code = """
# Calculate derived value
result = (temp + pressure) / 2
"""

average = system.python3.exec(code, {'temp': temp, 'pressure': pressure})
print("Average:", average)
```

### Process Tag Dataset

```python
# Read multiple tags
tag_values = system.tag.readBlocking([
    '[default]Sensor1',
    '[default]Sensor2',
    '[default]Sensor3'
])

values = [tv.value for tv in tag_values]

# Statistical analysis
code = """
import statistics

result = {
    'mean': statistics.mean(values),
    'median': statistics.median(values),
    'stdev': statistics.stdev(values) if len(values) > 1 else 0
}
"""

stats = system.python3.exec(code, {'values': values})
print("Statistics:", stats)
```

## Module Functions

### Math Module

```python
# Square root
result = system.python3.callModule("math", "sqrt", [144])
print("sqrt(144) =", result)

# Sine
import math as jython_math
angle_rad = jython_math.radians(45)
result = system.python3.callModule("math", "sin", [angle_rad])
print("sin(45°) =", result)

# Power
result = system.python3.callModule("math", "pow", [2, 10])
print("2^10 =", result)
```

### Random Module

```python
# Random integer
result = system.python3.callModule("random", "randint", [1, 100])
print("Random number:", result)

# Random choice
code = """
import random
choices = ['Option A', 'Option B', 'Option C']
result = random.choice(choices)
"""
choice = system.python3.exec(code)
print("Random choice:", choice)
```

### Datetime Module

```python
code = """
from datetime import datetime, timedelta

now = datetime.now()
tomorrow = now + timedelta(days=1)

result = {
    'now': now.isoformat(),
    'tomorrow': tomorrow.isoformat(),
    'day_of_week': now.strftime('%A')
}
"""

dates = system.python3.exec(code)
print("Today:", dates['now'])
print("Tomorrow:", dates['tomorrow'])
print("Day:", dates['day_of_week'])
```

## Advanced Examples

### Regular Expressions

```python
code = """
import re

text = "Sensor readings: Temp=25.5°C, Pressure=101.3kPa, Flow=150L/min"

# Extract values with regex
pattern = r'(\w+)=([0-9.]+)'
matches = re.findall(pattern, text)

result = {name: float(value) for name, value in matches}
"""

readings = system.python3.exec(code)
print("Parsed readings:", readings)
```

### File Operations (if Gateway has access)

```python
code = """
import os
import json

# Read configuration file
config_path = '/path/to/config.json'

if os.path.exists(config_path):
    with open(config_path, 'r') as f:
        result = json.load(f)
else:
    result = {'error': 'File not found'}
"""

config = system.python3.exec(code)
print("Config:", config)
```

### HTTP Requests (requires requests package)

```python
code = """
import requests
import json

# Make API call
response = requests.get('https://api.example.com/data')

if response.status_code == 200:
    result = response.json()
else:
    result = {'error': f'HTTP {response.status_code}'}
"""

api_data = system.python3.exec(code)
print("API response:", api_data)
```

### Data Processing with Collections

```python
code = """
from collections import Counter, defaultdict

# Sample data
events = [
    {'type': 'alarm', 'severity': 'high'},
    {'type': 'alarm', 'severity': 'low'},
    {'type': 'event', 'severity': 'info'},
    {'type': 'alarm', 'severity': 'high'},
    {'type': 'event', 'severity': 'info'},
]

# Count by type
type_counts = Counter(event['type'] for event in events)

# Group by severity
by_severity = defaultdict(list)
for event in events:
    by_severity[event['severity']].append(event)

result = {
    'type_counts': dict(type_counts),
    'by_severity': dict(by_severity)
}
"""

analysis = system.python3.exec(code)
print("Type counts:", analysis['type_counts'])
print("High severity:", len(analysis['by_severity']['high']))
```

## Integration with Ignition

### Tag Event Script

```python
# In a tag change event script
def valueChanged(tag, tagPath, previousValue, currentValue, initialChange, missedEvents):
    if currentValue.value > 100:
        # Use Python 3 for advanced calculation
        code = """
import math
# Calculate exponential decay
result = 100 * math.exp(-0.1 * (value - 100))
"""

        adjusted = system.python3.exec(code, {'value': currentValue.value})

        # Write back to another tag
        system.tag.writeBlocking(['[default]AdjustedValue'], [adjusted])
```

### Gateway Timer Script

```python
# In a gateway timer script that runs every hour
code = """
from datetime import datetime
import statistics

# Process hourly data
result = {
    'timestamp': datetime.now().isoformat(),
    'summary': 'Hourly processing complete'
}
"""

# Get historical data (example)
end_date = system.date.now()
start_date = system.date.addHours(end_date, -1)

# Query database or tags
# ... get data ...

# Process with Python 3
summary = system.python3.exec(code)

# Log result
system.util.getLogger('Python3Integration').info(str(summary))
```

### Button Action Script

```python
# In a button actionPerformed event
def runAnalysis():
    # Get data from table
    data = event.source.parent.getComponent('Table').data

    # Convert to Python list
    values = []
    for row in range(data.rowCount):
        values.append(data.getValueAt(row, 0))

    # Analyze with Python 3
    code = """
import statistics

if len(values) > 0:
    result = {
        'count': len(values),
        'sum': sum(values),
        'mean': statistics.mean(values),
        'min': min(values),
        'max': max(values)
    }
else:
    result = {'error': 'No data'}
"""

    stats = system.python3.exec(code, {'values': values})

    # Display results
    system.gui.messageBox(str(stats))

runAnalysis()
```

## Testing and Debugging

### Check Python 3 Availability

```python
# Check if Python 3 is working
if system.python3.isAvailable():
    print("Python 3 is available!")

    # Get version
    version_info = system.python3.getVersion()
    print("Version:", version_info['version'])

    # Run test
    test_result = system.python3.example()
    print("Test:", test_result)
else:
    print("Python 3 is NOT available - check Gateway logs")
```

### Monitor Process Pool

```python
# Check pool health
stats = system.python3.getPoolStats()

print("Total processes:", stats['totalSize'])
print("Available:", stats['available'])
print("In use:", stats['inUse'])
print("Healthy:", stats['healthy'])

# Alert if pool is exhausted
if stats['available'] == 0:
    system.util.getLogger('Python3').warn("Process pool exhausted!")
```

### Error Handling

```python
def safeExecute(code, variables=None):
    """Execute Python 3 code with error handling"""
    try:
        result = system.python3.exec(code, variables)
        return {'success': True, 'result': result}
    except Exception as e:
        logger = system.util.getLogger('Python3')
        logger.error("Python 3 execution failed: " + str(e))
        return {'success': False, 'error': str(e)}

# Usage
outcome = safeExecute("result = 1 / 0")  # Division by zero
if outcome['success']:
    print("Result:", outcome['result'])
else:
    print("Error:", outcome['error'])
```

## Performance Tips

### Batch Operations

```python
# GOOD: Process everything in one call
code = """
data = [1, 2, 3, 4, 5]
results = []
for x in data:
    results.append(x ** 2)
result = results
"""
all_results = system.python3.exec(code)

# BAD: Multiple calls (slower)
results = []
for x in [1, 2, 3, 4, 5]:
    result = system.python3.eval(str(x) + " ** 2")
    results.append(result)
```

### Reuse Calculations

```python
# GOOD: Calculate once, use multiple times
code = """
import math

# Expensive calculation
expensive_result = sum(i**2 for i in range(10000))

# Use result multiple times
result = {
    'squared': expensive_result ** 2,
    'sqrt': math.sqrt(expensive_result),
    'doubled': expensive_result * 2
}
"""
results = system.python3.exec(code)

# Access all results from one call
print(results['squared'])
print(results['sqrt'])
print(results['doubled'])
```

## Troubleshooting Examples

### Debug Variable Passing

```python
# Test what Python 3 receives
test_data = {
    'number': 42,
    'text': 'hello',
    'list': [1, 2, 3],
    'dict': {'a': 1, 'b': 2}
}

code = """
import json
result = json.dumps({
    'received': locals(),
    'types': {k: str(type(v)) for k, v in locals().items()}
}, indent=2)
"""

debug_info = system.python3.exec(code, test_data)
print(debug_info)
```

### Test Module Import

```python
# Check if a module is available
module_name = "pandas"

code = """
import importlib
try:
    importlib.import_module(module_name)
    result = f"{module_name} is available"
except ImportError:
    result = f"{module_name} is NOT installed"
"""

availability = system.python3.exec(code, {'module_name': module_name})
print(availability)
```
