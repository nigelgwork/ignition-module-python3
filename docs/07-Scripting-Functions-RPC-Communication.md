# Ignition SDK - Scripting Functions and RPC Communication

## Overview

The Ignition SDK allows you to add custom scripting functions available in Python scripts throughout Ignition (Gateway, Designer, Vision Client). You can also implement RPC (Remote Procedure Call) for Designer-Gateway communication.

## Scripting Functions

### Architecture

Scripting functions can be registered in any scope:
- **Gateway**: Available in Gateway scripts (tag event scripts, scheduled scripts)
- **Designer**: Available in Designer scripting console
- **Client**: Available in Vision client scripts

Functions are organized into modules (e.g., `system.util`, `system.tag`) and called from Python:
```python
result = system.example.myFunction(arg1, arg2)
```

## Basic Scripting Function

### Java Implementation

```java
package com.company.module.gateway;

import com.inductiveautomation.ignition.common.script.hints.ScriptArg;
import com.inductiveautomation.ignition.common.script.hints.ScriptFunction;
import org.python.core.PyObject;

/**
 * Scripting functions implementation
 */
public class ScriptFunctions {
    
    /**
     * Multiply two numbers
     * 
     * @param a First number
     * @param b Second number
     * @return Product of a and b
     */
    @ScriptFunction(docBundlePrefix = "ScriptFunctions")
    public static double multiply(
            @ScriptArg("a") double a,
            @ScriptArg("b") double b) {
        return a * b;
    }
    
    /**
     * Convert string to uppercase
     */
    @ScriptFunction(docBundlePrefix = "ScriptFunctions")
    public static String toUpper(
            @ScriptArg("text") String text) {
        if (text == null) {
            return "";
        }
        return text.toUpperCase();
    }
    
    /**
     * Check if value is in list
     */
    @ScriptFunction(docBundlePrefix = "ScriptFunctions")
    public static boolean contains(
            @ScriptArg("list") PyObject[] list,
            @ScriptArg("value") Object value) {
        
        for (PyObject item : list) {
            if (item.__tojava__(Object.class).equals(value)) {
                return true;
            }
        }
        return false;
    }
}
```

### Registration in Gateway Hook

```java
package com.company.module.gateway;

import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.common.script.ScriptManager;
import com.inductiveautomation.ignition.common.script.hints.PropertiesFileDocProvider;
import com.inductiveautomation.ignition.gateway.model.AbstractGatewayModuleHook;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;

public class GatewayHook extends AbstractGatewayModuleHook {
    
    private GatewayContext context;
    
    @Override
    public void setup(GatewayContext context) {
        this.context = context;
    }
    
    @Override
    public void startup(LicenseState licenseState) {
        // Nothing needed here
    }
    
    @Override
    public void shutdown() {
        // Cleanup
    }
    
    @Override
    public void initializeScriptManager(ScriptManager manager) {
        super.initializeScriptManager(manager);
        
        // Register scripting functions
        manager.addScriptModule(
            "system.example",                           // Module name
            new ScriptFunctions(),                      // Implementation
            new PropertiesFileDocProvider()             // Documentation provider
        );
    }
}
```

### Documentation Properties File

Create `ScriptFunctions.properties` in resources:

```properties
multiply.desc=Multiplies two numbers together
multiply.param.a=First number
multiply.param.b=Second number
multiply.returns=Product of a and b

toUpper.desc=Converts string to uppercase
toUpper.param.text=Text to convert
toUpper.returns=Uppercase version of text

contains.desc=Check if value is in list
contains.param.list=List to search
contains.param.value=Value to find
contains.returns=True if value is in list
```

### Usage in Python

```python
# In Gateway/Designer/Client scripts
result = system.example.multiply(5, 3)
print(result)  # 15

text = system.example.toUpper("hello")
print(text)  # "HELLO"

items = [1, 2, 3, 4, 5]
found = system.example.contains(items, 3)
print(found)  # True
```

## Advanced Scripting Features

### Working with Datasets

```java
import com.inductiveautomation.ignition.common.Dataset;
import com.inductiveautomation.ignition.common.BasicDataset;

@ScriptFunction(docBundlePrefix = "ScriptFunctions")
public static Dataset filterDataset(
        @ScriptArg("dataset") Dataset input,
        @ScriptArg("column") String column,
        @ScriptArg("value") Object value) {
    
    List<Object[]> filteredRows = new ArrayList<>();
    
    int colIndex = input.getColumnIndex(column);
    if (colIndex < 0) {
        return input;  // Column not found
    }
    
    for (int row = 0; row < input.getRowCount(); row++) {
        Object cellValue = input.getValueAt(row, colIndex);
        if (cellValue != null && cellValue.equals(value)) {
            Object[] rowData = new Object[input.getColumnCount()];
            for (int col = 0; col < input.getColumnCount(); col++) {
                rowData[col] = input.getValueAt(row, col);
            }
            filteredRows.add(rowData);
        }
    }
    
    // Create new dataset
    String[] columnNames = new String[input.getColumnCount()];
    Class<?>[] columnTypes = new Class[input.getColumnCount()];
    for (int i = 0; i < input.getColumnCount(); i++) {
        columnNames[i] = input.getColumnName(i);
        columnTypes[i] = input.getColumnType(i);
    }
    
    return new BasicDataset(columnNames, columnTypes, filteredRows);
}
```

### Async Operations

```java
import java.util.concurrent.*;

@ScriptFunction(docBundlePrefix = "ScriptFunctions")
public static Future<String> asyncOperation(
        @ScriptArg("param") String param) {
    
    ExecutorService executor = Executors.newSingleThreadExecutor();
    
    return executor.submit(() -> {
        // Long-running operation
        Thread.sleep(5000);
        return "Result: " + param;
    });
}

// Usage in Python:
# future = system.example.asyncOperation("test")
# # Do other work...
# result = future.get()  # Wait for completion
```

### Error Handling

```java
@ScriptFunction(docBundlePrefix = "ScriptFunctions")
public static Object safeOperation(
        @ScriptArg("value") Object value) throws Exception {
    
    if (value == null) {
        throw new IllegalArgumentException("Value cannot be null");
    }
    
    try {
        // Perform operation
        return processValue(value);
    } catch (Exception e) {
        // Log error
        logger.error("Error processing value", e);
        // Re-throw or return error indicator
        throw new Exception("Failed to process value: " + e.getMessage());
    }
}
```

## RPC Communication

RPC allows Designer and Gateway to communicate directly, useful for:
- Querying Gateway data from Designer
- Triggering Gateway operations from Designer
- Real-time configuration updates

### RPC Interface (Common)

```java
package com.company.module.common;

/**
 * RPC interface shared between Gateway and Designer
 */
public interface ModuleRPC {
    
    /**
     * Get system status from Gateway
     */
    String getStatus();
    
    /**
     * Query data from Gateway
     */
    List<String> queryData(String query);
    
    /**
     * Trigger operation on Gateway
     */
    boolean executeOperation(String operation, Map<String, Object> params);
    
    /**
     * Get configuration value
     */
    Object getConfigValue(String key);
    
    /**
     * Set configuration value
     */
    void setConfigValue(String key, Object value);
}
```

### RPC Implementation (Gateway)

```java
package com.company.module.gateway;

import com.company.module.common.ModuleRPC;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Gateway-side RPC implementation
 */
public class ModuleRPCImpl implements ModuleRPC {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final GatewayContext context;
    
    public ModuleRPCImpl(GatewayContext context) {
        this.context = context;
    }
    
    @Override
    public String getStatus() {
        logger.info("RPC: getStatus called");
        return "Gateway is running normally";
    }
    
    @Override
    public List<String> queryData(String query) {
        logger.info("RPC: queryData called with: {}", query);
        
        // Perform database query or other operation
        List<String> results = new ArrayList<>();
        
        try {
            // Example: Query database
            // results = performDatabaseQuery(query);
            results.add("Result 1");
            results.add("Result 2");
        } catch (Exception e) {
            logger.error("Error querying data", e);
        }
        
        return results;
    }
    
    @Override
    public boolean executeOperation(String operation, Map<String, Object> params) {
        logger.info("RPC: executeOperation - {} with params: {}", operation, params);
        
        try {
            // Execute requested operation
            switch (operation) {
                case "restart":
                    // Restart something
                    return true;
                case "refresh":
                    // Refresh data
                    return true;
                default:
                    logger.warn("Unknown operation: {}", operation);
                    return false;
            }
        } catch (Exception e) {
            logger.error("Error executing operation", e);
            return false;
        }
    }
    
    @Override
    public Object getConfigValue(String key) {
        // Get from persistent storage
        return context.getSystemManager().getSystemSettings().get(key);
    }
    
    @Override
    public void setConfigValue(String key, Object value) {
        // Save to persistent storage
        context.getSystemManager().getSystemSettings().put(key, value);
    }
}
```

### RPC Registration (Gateway Hook)

```java
@Override
public Object getRPCHandler(ClientReqSession session, String projectId) {
    return new ModuleRPCImpl(context);
}
```

### RPC Usage (Designer)

```java
package com.company.module.designer;

import com.company.module.common.ModuleRPC;
import com.inductiveautomation.ignition.client.gateway_interface.ModuleRPCFactory;
import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.designer.model.AbstractDesignerModuleHook;
import com.inductiveautomation.ignition.designer.model.DesignerContext;

public class DesignerHook extends AbstractDesignerModuleHook {
    
    private static final String MODULE_ID = "com.company.module";
    private DesignerContext context;
    private ModuleRPC rpc;
    
    @Override
    public void setup(DesignerContext context) {
        this.context = context;
    }
    
    @Override
    public void startup(LicenseState licenseState) {
        // Create RPC proxy
        rpc = ModuleRPCFactory.create(
            MODULE_ID,
            ModuleRPC.class
        );
        
        // Use RPC
        String status = rpc.getStatus();
        System.out.println("Gateway status: " + status);
    }
    
    @Override
    public void shutdown() {
        // Cleanup
    }
    
    // Provide access to RPC for other Designer components
    public ModuleRPC getRPC() {
        return rpc;
    }
}
```

### Using RPC from Designer UI

```java
// In a Designer panel or action
public class ConfigurationPanel extends JPanel {
    
    private ModuleRPC rpc;
    
    public ConfigurationPanel(DesignerContext context) {
        // Get RPC from hook
        DesignerHook hook = (DesignerHook) context.getModule(MODULE_ID).get();
        this.rpc = hook.getRPC();
        
        // Create UI
        JButton queryButton = new JButton("Query Data");
        queryButton.addActionListener(e -> {
            List<String> results = rpc.queryData("SELECT * FROM table");
            // Display results
        });
        
        add(queryButton);
    }
}
```

## Scripting in Multiple Scopes

### Same Functions in All Scopes

```java
// Gateway Hook
@Override
public void initializeScriptManager(ScriptManager manager) {
    super.initializeScriptManager(manager);
    manager.addScriptModule(
        "system.example",
        new ScriptFunctions(context),  // Gateway implementation
        new PropertiesFileDocProvider()
    );
}

// Designer Hook
@Override
public void initializeScriptManager(ScriptManager manager) {
    super.initializeScriptManager(manager);
    
    // Use RPC to delegate to Gateway
    ModuleRPC rpc = ModuleRPCFactory.create(MODULE_ID, ModuleRPC.class);
    
    manager.addScriptModule(
        "system.example",
        new ScriptFunctionsDesigner(rpc),  // Designer implementation using RPC
        new PropertiesFileDocProvider()
    );
}
```

### Designer Implementation Using RPC

```java
public class ScriptFunctionsDesigner {
    
    private final ModuleRPC rpc;
    
    public ScriptFunctionsDesigner(ModuleRPC rpc) {
        this.rpc = rpc;
    }
    
    @ScriptFunction(docBundlePrefix = "ScriptFunctions")
    public String getServerStatus() {
        // Delegate to Gateway via RPC
        return rpc.getStatus();
    }
    
    @ScriptFunction(docBundlePrefix = "ScriptFunctions")
    public List<String> queryServerData(String query) {
        // Delegate to Gateway via RPC
        return rpc.queryData(query);
    }
}
```

## Best Practices

### ✅ Do
- Use @ScriptFunction annotation
- Provide clear JavaDoc
- Create documentation properties file
- Handle null inputs gracefully
- Use descriptive parameter names
- Return appropriate types
- Log important operations
- Consider thread safety
- Test in all scopes

### ❌ Don't
- Block indefinitely
- Return null without documentation
- Use complex Java objects (use primitives, String, Dataset)
- Forget error handling
- Ignore security implications
- Perform expensive operations synchronously
- Modify global state without synchronization

## Python Type Conversions

### Java to Python

```java
// Java primitives/wrappers → Python
int → PyInteger
double → PyFloat
String → PyString
boolean → PyBoolean
Object[] → PyList
Map → PyDictionary
null → None

// Converting in Java:
PyObject pyList = Py.java2py(javaList);
```

### Python to Java

```python
# Python → Java
42 → int/Integer
3.14 → double/Double
"text" → String
True → boolean/Boolean
[1,2,3] → Object[]
{"key": "value"} → Map
None → null
```

### Handling Python Objects in Java

```java
@ScriptFunction
public Object processItems(@ScriptArg("items") PyObject items) {
    if (items instanceof PyList) {
        PyList list = (PyList) items;
        for (Object item : list) {
            // Process each item
            Object javaObject = ((PyObject) item).__tojava__(Object.class);
            // Work with javaObject
        }
    }
    return null;
}
```

## Testing

### Unit Tests for Scripting Functions

```java
public class ScriptFunctionsTest {
    
    @Test
    public void testMultiply() {
        double result = ScriptFunctions.multiply(5, 3);
        assertEquals(15.0, result, 0.001);
    }
    
    @Test
    public void testToUpper() {
        String result = ScriptFunctions.toUpper("hello");
        assertEquals("HELLO", result);
    }
    
    @Test
    public void testToUpperNull() {
        String result = ScriptFunctions.toUpper(null);
        assertEquals("", result);
    }
}
```

### Integration Tests for RPC

```java
public class RPCIntegrationTest {
    
    private ModuleRPC rpc;
    
    @Before
    public void setup() {
        // Create RPC proxy
        rpc = ModuleRPCFactory.create(MODULE_ID, ModuleRPC.class);
    }
    
    @Test
    public void testGetStatus() {
        String status = rpc.getStatus();
        assertNotNull(status);
        assertTrue(status.length() > 0);
    }
    
    @Test
    public void testQueryData() {
        List<String> results = rpc.queryData("test query");
        assertNotNull(results);
    }
}
```

## Troubleshooting

**Functions not appearing**:
- Check ScriptManager initialization
- Verify module name (system.example)
- Check function annotations
- Restart Designer/Gateway

**RPC not working**:
- Verify getRPCHandler implementation
- Check module ID matches
- Ensure interface is in Common scope
- Review Gateway logs

**Type conversion errors**:
- Use proper Python/Java conversions
- Check parameter types
- Handle None/null properly
- Test with various input types

**Performance issues**:
- Avoid expensive operations in scripting functions
- Use async for long operations
- Cache results when possible
- Profile with JVM tools
