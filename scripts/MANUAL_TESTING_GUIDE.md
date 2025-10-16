# Manual Testing Guide - Python 3 Integration Module

**Purpose:** Verify all module functions work correctly in Designer Script Console
**Environment:** Ignition Gateway (localhost:9088) + Designer
**Time Required:** ~10-15 minutes

---

## Prerequisites

1. ✅ Module installed in Gateway (Python3Integration-1.3.0.modl)
2. ✅ Gateway running (http://localhost:9088)
3. ✅ Designer connected to Gateway
4. ✅ Script Console open (Tools → Script Console)

---

## Test Scenarios

### Test 1: Module Availability

**Purpose:** Verify module loaded and Python is available

```python
# Test basic availability
available = system.python3.isAvailable()
print("Python 3 Available:", available)
# Expected: True

# Get version information
version_info = system.python3.getVersion()
print("Version Info:", version_info)
# Expected: {'available': True, 'version': '3.x.x ...'}

# Get pool statistics
stats = system.python3.getPoolStats()
print("Pool Stats:", stats)
# Expected: {'totalSize': 3, 'available': 3, 'inUse': 0, 'healthy': 3}
```

**Expected Results:**
- `isAvailable()` returns `True`
- `getVersion()` shows Python version (3.11.x or 3.12.x)
- `getPoolStats()` shows 3 total processes, all available and healthy

---

### Test 2: Simple Evaluation

**Purpose:** Test expression evaluation

```python
# Test basic math
result = system.python3.eval("2 + 2")
print("2 + 2 =", result)
# Expected: 4

# Test power operation (not available in Jython 2.7 easily)
result = system.python3.eval("2 ** 100")
print("2 ** 100 =", result)
# Expected: Large number (1267650600228229401496703205376)

# Test with string
result = system.python3.eval("'Hello ' + 'World'")
print("Concatenation:", result)
# Expected: 'Hello World'

# Test list comprehension
result = system.python3.eval("[x**2 for x in range(5)]")
print("List comprehension:", result)
# Expected: [0, 1, 4, 9, 16]
```

**Expected Results:**
- All evaluations return correct values
- No errors in console

---

### Test 3: Code Execution

**Purpose:** Test code block execution with variables

```python
# Test simple execution
code = """
x = 10
y = 20
result = x + y
"""
result = system.python3.exec(code)
print("Exec result:", result)
# Expected: 30

# Test with variables passed in
code = """
result = name + " is " + str(age) + " years old"
"""
result = system.python3.exec(code, {'name': 'Alice', 'age': 30})
print("With variables:", result)
# Expected: 'Alice is 30 years old'

# Test with imports
code = """
import sys
import platform
result = {
    'python_version': sys.version,
    'platform': platform.system()
}
"""
result = system.python3.exec(code)
print("System info:", result)
# Expected: Dict with version and platform info
```

**Expected Results:**
- Code executes successfully
- Variables are passed correctly
- Python imports work
- Results returned properly

---

### Test 4: Module Function Calls

**Purpose:** Test calling Python standard library functions

```python
# Test math.sqrt
result = system.python3.callModule("math", "sqrt", [144])
print("sqrt(144) =", result)
# Expected: 12.0

# Test math.pow
result = system.python3.callModule("math", "pow", [2, 10])
print("pow(2, 10) =", result)
# Expected: 1024.0

# Test os.getcwd (if available)
result = system.python3.callModule("os", "getcwd", [])
print("Current directory:", result)
# Expected: Some path string

# Test json.dumps
result = system.python3.callModule("json", "dumps", [{'a': 1, 'b': 2}])
print("JSON:", result)
# Expected: '{"a": 1, "b": 2}' or similar
```

**Expected Results:**
- All module functions execute
- Correct return values
- No import errors

---

### Test 5: Error Handling

**Purpose:** Verify errors are handled gracefully

```python
# Test syntax error
try:
    result = system.python3.eval("2 +")
    print("Should not reach here")
except Exception as e:
    print("Syntax error caught:", str(e)[:50])
# Expected: Error message about syntax

# Test undefined variable
try:
    result = system.python3.eval("undefined_variable")
    print("Should not reach here")
except Exception as e:
    print("Name error caught:", str(e)[:50])
# Expected: Error message about undefined variable

# Test division by zero
try:
    result = system.python3.eval("1 / 0")
    print("Should not reach here")
except Exception as e:
    print("Zero division caught:", str(e)[:50])
# Expected: Error message about division by zero

# Test import of non-existent module
try:
    result = system.python3.callModule("nonexistent_module", "func", [])
    print("Should not reach here")
except Exception as e:
    print("Import error caught:", str(e)[:50])
# Expected: Error message about module not found
```

**Expected Results:**
- All errors are caught as exceptions
- Error messages are descriptive
- Module doesn't crash

---

### Test 6: Concurrent Execution

**Purpose:** Test process pool handles multiple requests

```python
# Run multiple evaluations in quick succession
import time

start = time.time()

results = []
for i in range(5):
    result = system.python3.eval("2 ** {}".format(i))
    results.append(result)
    print("Iteration {}: {}".format(i, result))

elapsed = time.time() - start
print("Total time: {:.3f} seconds".format(elapsed))
print("All results:", results)

# Check pool stats after
stats = system.python3.getPoolStats()
print("Pool stats after:", stats)
# Expected: All processes still healthy, available count = 3
```

**Expected Results:**
- All requests complete successfully
- Total time < 1 second (should be fast)
- Pool remains healthy

---

### Test 7: Example Function

**Purpose:** Test the built-in example

```python
# Run the example test
result = system.python3.example()
print("Example result:", result)
# Expected: "Python 3 is working! 2^100 = <large number>"
```

**Expected Results:**
- Example runs successfully
- Returns expected message

---

### Test 8: Real-World Use Case

**Purpose:** Test a practical scenario

```python
# Test: Process a list of sensor readings
code = """
import statistics

# Sensor readings
readings = sensor_data

# Calculate statistics
result = {
    'count': len(readings),
    'mean': statistics.mean(readings),
    'median': statistics.median(readings),
    'stdev': statistics.stdev(readings) if len(readings) > 1 else 0,
    'min': min(readings),
    'max': max(readings)
}
"""

sensor_data = [23.5, 24.1, 23.8, 24.5, 23.9, 24.2, 23.7]
result = system.python3.exec(code, {'sensor_data': sensor_data})

print("Sensor Analysis:")
for key, value in result.items():
    print("  {}: {:.2f}".format(key, value))

# Expected: Dict with statistical analysis
```

**Expected Results:**
- Complex Python code executes
- Statistics calculated correctly
- Results returned as dict

---

## Test Results Template

Copy this template and fill in results:

```
=== PYTHON 3 INTEGRATION MODULE TEST RESULTS ===

Date: _____________
Tester: ___________
Gateway Version: Ignition 8.3.0
Module Version: 1.3.0

Test 1 - Module Availability:        [ ] PASS [ ] FAIL
Test 2 - Simple Evaluation:          [ ] PASS [ ] FAIL
Test 3 - Code Execution:             [ ] PASS [ ] FAIL
Test 4 - Module Function Calls:      [ ] PASS [ ] FAIL
Test 5 - Error Handling:             [ ] PASS [ ] FAIL
Test 6 - Concurrent Execution:       [ ] PASS [ ] FAIL
Test 7 - Example Function:           [ ] PASS [ ] FAIL
Test 8 - Real-World Use Case:        [ ] PASS [ ] FAIL

Overall Status: [ ] ALL PASS [ ] SOME FAILURES

Notes:
_____________________________________
_____________________________________
_____________________________________
```

---

## Troubleshooting

### Module Not Available

**Symptom:** `system.python3.isAvailable()` returns `False`

**Checks:**
1. Check Gateway logs for initialization errors
2. Verify Python installed: `docker exec claude-ignition-test python3 --version`
3. Check module loaded: Gateway → Config → System → Modules
4. Restart Gateway and check logs again

**Log Location:**
```bash
docker logs claude-ignition-test 2>&1 | grep -i python
```

### Functions Return Errors

**Symptom:** Script functions throw exceptions

**Checks:**
1. Check pool statistics: `system.python3.getPoolStats()`
2. Verify processes are healthy (healthy count = 3)
3. Check Gateway logs for Python process errors
4. Try the example function: `system.python3.example()`

### Performance Issues

**Symptom:** Functions run slowly

**Checks:**
1. Check pool stats to see if processes are available
2. If `available` count is 0, increase pool size:
   - Edit `ignition.conf`: `-Dignition.python3.poolsize=5`
3. Check if processes are hung (restart Gateway)

---

## Success Criteria

**All tests should PASS with:**
- ✅ No exceptions (except Test 5 error handling)
- ✅ Correct return values
- ✅ Fast execution (< 1 second per test)
- ✅ Pool remains healthy throughout

**If all tests pass:** Module is working correctly! 🎉

---

## Next Steps After Testing

If all tests pass:
1. Document any issues or observations
2. Test with your actual use cases
3. Install any required Python packages (pip install)
4. Create production scripts using `system.python3.*` functions

If any tests fail:
1. Document the exact error message
2. Check Gateway logs for details
3. Report findings with log excerpts
4. Try restarting Gateway and retesting

---

*Manual testing is required because:*
- No Gateway Script Console in web UI
- REST API servlet had compatibility issues
- Designer Script Console is the most reliable testing method
- Allows verification of actual user experience

Good luck with testing! 🚀
