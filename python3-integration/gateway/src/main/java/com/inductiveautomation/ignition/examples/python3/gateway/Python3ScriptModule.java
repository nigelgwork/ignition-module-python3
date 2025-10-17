package com.inductiveautomation.ignition.examples.python3.gateway;

import com.inductiveautomation.ignition.examples.python3.Python3RpcFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gateway implementation of Python 3 scripting functions.
 * Executes Python code directly via the process pool.
 * Also implements RPC interface so Designer/Client can call these functions remotely.
 */
public class Python3ScriptModule implements Python3RpcFunctions {

    private static final Logger LOGGER = LoggerFactory.getLogger(Python3ScriptModule.class);
    private final GatewayHook gatewayHook;

    public Python3ScriptModule(GatewayHook gatewayHook) {
        this.gatewayHook = gatewayHook;
        LOGGER.info("Python3ScriptModule created");
    }

    /**
     * Lazily get the process pool from the gateway hook.
     * This allows the script module to be registered before the pool is initialized.
     */
    private Python3ProcessPool getProcessPool() {
        return gatewayHook.getProcessPool();
    }

    /**
     * Lazily get the distribution manager from the gateway hook.
     */
    private PythonDistributionManager getDistributionManager() {
        return gatewayHook.getDistributionManager();
    }

    /**
     * Lazily get the script repository from the gateway hook.
     */
    private Python3ScriptRepository getScriptRepository() {
        return gatewayHook.getScriptRepository();
    }

    /**
     * Execute Python 3 code and return the result.
     *
     * @param code Python code to execute
     * @return Result of execution
     */
    public Object exec(String code) throws Exception {
        return exec(code, Collections.emptyMap());
    }

    /**
     * Execute Python 3 code with variables and return the result.
     *
     * @param code      Python code to execute
     * @param variables Dictionary of variables to pass to Python
     * @return Result of execution
     */
    @Override
    public Object exec(String code, Map<String, Object> variables) throws Exception {
        return exec(code, variables, "RESTRICTED");
    }

    /**
     * Execute Python 3 code with variables and security mode.
     *
     * @param code         Python code to execute
     * @param variables    Dictionary of variables to pass to Python
     * @param securityMode Security mode: "RESTRICTED" or "ADMIN"
     * @return Result of execution
     */
    public Object exec(String code, Map<String, Object> variables, String securityMode) throws Exception {
        LOGGER.debug("exec() called with code length: {}, security mode: {}",
                    code != null ? code.length() : 0, securityMode);

        try {
            Python3ProcessPool pool = getProcessPool();
            if (pool == null) {
                String errorMsg = "Python 3 process pool is not initialized. Check Gateway logs for initialization errors.";
                LOGGER.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            LOGGER.debug("Executing Python code via process pool");
            Python3Result result = pool.execute(code, variables != null ? variables : Collections.emptyMap(), securityMode);

            if (result.isSuccess()) {
                LOGGER.debug("Python code executed successfully");
                return result.getResult();
            } else {
                String errorMsg = "Python error: " + result.getError();
                if (result.getTraceback() != null) {
                    errorMsg += "\n" + result.getTraceback();
                }
                LOGGER.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

        } catch (Python3Exception e) {
            LOGGER.error("Failed to execute Python code", e);
            throw new RuntimeException("Failed to execute Python code: " + e.getMessage(), e);
        }
    }

    /**
     * Evaluate a Python 3 expression and return the result.
     *
     * @param expression Python expression to evaluate
     * @return Result of expression
     */
    public Object eval(String expression) throws Exception {
        return eval(expression, Collections.emptyMap());
    }

    /**
     * Evaluate a Python 3 expression with variables and return the result.
     *
     * @param expression Python expression to evaluate
     * @param variables  Dictionary of variables to pass to Python
     * @return Result of expression
     */
    @Override
    public Object eval(String expression, Map<String, Object> variables) throws Exception {
        return eval(expression, variables, "RESTRICTED");
    }

    /**
     * Evaluate a Python 3 expression with variables and security mode.
     *
     * @param expression   Python expression to evaluate
     * @param variables    Dictionary of variables to pass to Python
     * @param securityMode Security mode: "RESTRICTED" or "ADMIN"
     * @return Result of expression
     */
    public Object eval(String expression, Map<String, Object> variables, String securityMode) throws Exception {
        LOGGER.debug("eval() called with expression: {}, security mode: {}", expression, securityMode);

        try {
            Python3ProcessPool pool = getProcessPool();
            if (pool == null) {
                String errorMsg = "Python 3 process pool is not initialized. Check Gateway logs for initialization errors.";
                LOGGER.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            LOGGER.debug("Evaluating Python expression via process pool");
            Python3Result result = pool.evaluate(expression, variables != null ? variables : Collections.emptyMap(), securityMode);

            if (result.isSuccess()) {
                LOGGER.debug("Python expression evaluated successfully");
                return result.getResult();
            } else {
                String errorMsg = "Python error: " + result.getError();
                if (result.getTraceback() != null) {
                    errorMsg += "\n" + result.getTraceback();
                }
                LOGGER.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

        } catch (Python3Exception e) {
            LOGGER.error("Failed to evaluate Python expression", e);
            throw new RuntimeException("Failed to evaluate Python expression: " + e.getMessage(), e);
        }
    }

    /**
     * Call a function from a Python 3 module.
     *
     * @param moduleName   Module name (e.g., "math")
     * @param functionName Function name (e.g., "sqrt")
     * @param args         List of positional arguments
     * @return Result of function call
     */
    @Override
    public Object callModule(String moduleName, String functionName, List<Object> args) throws Exception {
        return callModule(moduleName, functionName, args, Collections.emptyMap());
    }

    /**
     * Call a function from a Python 3 module with keyword arguments.
     *
     * @param moduleName   Module name (e.g., "math")
     * @param functionName Function name (e.g., "sqrt")
     * @param args         List of positional arguments
     * @param kwargs       Dictionary of keyword arguments
     * @return Result of function call
     */
    public Object callModule(String moduleName, String functionName, List<Object> args, Map<String, Object> kwargs) {
        return callModule(moduleName, functionName, args, kwargs, "RESTRICTED");
    }

    /**
     * Call a function from a Python 3 module with keyword arguments and security mode.
     *
     * @param moduleName   Module name (e.g., "math")
     * @param functionName Function name (e.g., "sqrt")
     * @param args         List of positional arguments
     * @param kwargs       Dictionary of keyword arguments
     * @param securityMode Security mode: "RESTRICTED" or "ADMIN"
     * @return Result of function call
     */
    public Object callModule(String moduleName, String functionName, List<Object> args, Map<String, Object> kwargs, String securityMode) {
        LOGGER.debug("callModule() called: {}.{}(), security mode: {}", moduleName, functionName, securityMode);

        try {
            Python3ProcessPool pool = getProcessPool();
            if (pool == null) {
                String errorMsg = "Python 3 process pool is not initialized. Check Gateway logs for initialization errors.";
                LOGGER.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            LOGGER.debug("Calling Python module function via process pool");
            Python3Result result = pool.callModule(
                moduleName,
                functionName,
                args != null ? args : Collections.emptyList(),
                kwargs != null ? kwargs : Collections.emptyMap(),
                securityMode
            );

            if (result.isSuccess()) {
                LOGGER.debug("Python module function called successfully");
                return result.getResult();
            } else {
                String errorMsg = "Python error: " + result.getError();
                if (result.getTraceback() != null) {
                    errorMsg += "\n" + result.getTraceback();
                }
                LOGGER.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

        } catch (Python3Exception e) {
            LOGGER.error("Failed to call Python module function", e);
            throw new RuntimeException("Failed to call Python module function: " + e.getMessage(), e);
        }
    }

    /**
     * Check if Python 3 is available and the process pool is healthy.
     *
     * @return true if Python 3 is available
     */
    @Override
    public boolean isAvailable() {
        Python3ProcessPool pool = getProcessPool();
        boolean available = pool != null && !pool.isShutdown();
        LOGGER.debug("isAvailable() = {}", available);
        return available;
    }

    /**
     * Get Python 3 version information.
     *
     * @return Dictionary with version information
     */
    @Override
    public Map<String, Object> getVersion() {
        LOGGER.debug("getVersion() called");

        try {
            Python3ProcessPool pool = getProcessPool();
            if (pool == null) {
                Map<String, Object> versionInfo = new HashMap<>();
                versionInfo.put("available", false);
                versionInfo.put("error", "Python 3 process pool is not initialized");
                LOGGER.warn("getVersion() - pool not initialized");
                return versionInfo;
            }

            // v2.0.17: Use ADMIN mode to allow sys import (safe read-only operation)
            Python3Result result = pool.execute("import sys; result = sys.version", Collections.emptyMap(), "ADMIN");

            if (result.isSuccess()) {
                Map<String, Object> versionInfo = new HashMap<>();
                versionInfo.put("version", result.getResult());
                versionInfo.put("available", true);
                LOGGER.debug("getVersion() successful: {}", result.getResult());
                return versionInfo;
            } else {
                Map<String, Object> versionInfo = new HashMap<>();
                versionInfo.put("available", false);
                versionInfo.put("error", result.getError());
                LOGGER.error("getVersion() failed: {}", result.getError());
                return versionInfo;
            }

        } catch (Exception e) {
            LOGGER.error("Failed to get Python version", e);
            Map<String, Object> versionInfo = new HashMap<>();
            versionInfo.put("available", false);
            versionInfo.put("error", e.getMessage());
            return versionInfo;
        }
    }

    /**
     * Get process pool statistics.
     *
     * @return Dictionary with pool statistics
     */
    @Override
    public Map<String, Object> getPoolStats() {
        LOGGER.debug("getPoolStats() called");

        Python3ProcessPool pool = getProcessPool();
        if (pool == null) {
            Map<String, Object> statsMap = new HashMap<>();
            statsMap.put("error", "Python 3 process pool is not initialized");
            LOGGER.warn("getPoolStats() - pool not initialized");
            return statsMap;
        }

        Python3ProcessPool.PoolStats stats = pool.getStats();

        Map<String, Object> statsMap = new HashMap<>();
        statsMap.put("totalSize", stats.totalSize);
        statsMap.put("available", stats.available);
        statsMap.put("inUse", stats.inUse);
        statsMap.put("healthy", stats.healthy);

        LOGGER.debug("getPoolStats() - total: {}, available: {}, inUse: {}, healthy: {}",
            stats.totalSize, stats.available, stats.inUse, stats.healthy);

        return statsMap;
    }

    /**
     * Resize the process pool to a new size (1-20).
     *
     * @param newSize the new pool size (1-20)
     * @throws IllegalArgumentException if newSize is out of range
     * @throws IllegalStateException if pool is not initialized or shutdown
     *
     * v1.17.2: Added for dynamic pool size adjustment
     */
    public void resizePool(int newSize) {
        LOGGER.debug("resizePool() called with newSize: {}", newSize);

        Python3ProcessPool pool = getProcessPool();
        if (pool == null) {
            String errorMsg = "Python 3 process pool is not initialized";
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        pool.resizePool(newSize);
        LOGGER.info("Process pool resized to {}", newSize);
    }

    /**
     * Execute a simple Python 3 example (for testing).
     *
     * @return Example result
     */
    @Override
    public String example() throws Exception {
        LOGGER.info("example() called - running test");

        try {
            // Test basic math
            Object result = eval("2 ** 100");
            String successMsg = "Python 3 is working! 2^100 = " + result;
            LOGGER.info("example() successful");
            return successMsg;

        } catch (Exception e) {
            String errorMsg = "Python 3 error: " + e.getMessage();
            LOGGER.error("example() failed: {}", errorMsg);
            return errorMsg;
        }
    }

    /**
     * Get Python distribution status and installation info.
     *
     * @return Dictionary with distribution information
     */
    @Override
    public Map<String, Object> getDistributionInfo() {
        LOGGER.debug("getDistributionInfo() called");

        PythonDistributionManager manager = getDistributionManager();
        if (manager != null) {
            Map<String, Object> info = manager.getStatus();
            LOGGER.debug("getDistributionInfo() returned status");
            return info;
        } else {
            Map<String, Object> info = new HashMap<>();
            info.put("available", false);
            info.put("error", "Distribution manager not initialized");
            LOGGER.warn("getDistributionInfo() - manager not initialized");
            return info;
        }
    }

    /**
     * Call a saved Python script by path with arguments.
     * The script's 'result' variable will be returned.
     *
     * @param scriptPath Path to the script (e.g., "My Script" or "Folder/My Script")
     * @param args       Positional arguments to pass to the script
     * @param kwargs     Keyword arguments to pass to the script
     * @return The value of the 'result' variable from the script
     * @throws Exception if script not found or execution fails
     */
    @Override
    public Object callScript(String scriptPath, List<Object> args, Map<String, Object> kwargs) throws Exception {
        LOGGER.debug("callScript() called with path: {}", scriptPath);

        try {
            // Load the script from repository
            Python3ScriptRepository repository = getScriptRepository();
            if (repository == null) {
                String errorMsg = "Script repository is not initialized";
                LOGGER.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            Python3ScriptRepository.SavedScript script = repository.loadScriptByPath(scriptPath);
            if (script == null) {
                String errorMsg = "Script not found: " + scriptPath;
                LOGGER.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            LOGGER.debug("Loaded script: {} with code length: {}", script.getName(), script.getCode().length());

            // Prepare variables to inject into the script
            Map<String, Object> variables = new HashMap<>();
            if (args != null) {
                variables.put("args", args);
            } else {
                variables.put("args", Collections.emptyList());
            }
            if (kwargs != null) {
                variables.put("kwargs", kwargs);
            } else {
                variables.put("kwargs", Collections.emptyMap());
            }

            LOGGER.debug("Executing script with {} args and {} kwargs",
                args != null ? args.size() : 0,
                kwargs != null ? kwargs.size() : 0);

            // Execute the script
            Python3ProcessPool pool = getProcessPool();
            if (pool == null) {
                String errorMsg = "Python 3 process pool is not initialized. Check Gateway logs for initialization errors.";
                LOGGER.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            Python3Result result = pool.execute(script.getCode(), variables);

            if (result.isSuccess()) {
                LOGGER.debug("Script executed successfully");
                return result.getResult();
            } else {
                String errorMsg = "Python error in script '" + scriptPath + "': " + result.getError();
                if (result.getTraceback() != null) {
                    errorMsg += "\n" + result.getTraceback();
                }
                LOGGER.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

        } catch (Python3Exception e) {
            LOGGER.error("Failed to execute script: {}", scriptPath, e);
            throw new RuntimeException("Failed to execute script '" + scriptPath + "': " + e.getMessage(), e);
        }
    }

    /**
     * Call a saved Python script by path (without arguments).
     *
     * @param scriptPath Path to the script
     * @return The value of the 'result' variable from the script
     * @throws Exception if script not found or execution fails
     */
    public Object callScript(String scriptPath) throws Exception {
        return callScript(scriptPath, Collections.emptyList(), Collections.emptyMap());
    }

    /**
     * Check Python code for syntax errors.
     * Uses AST parser and pyflakes for validation.
     *
     * @param code Python code to check
     * @return Dictionary with "errors" list containing error details
     */
    public Map<String, Object> checkSyntax(String code) {
        LOGGER.debug("checkSyntax() called with code length: {}", code != null ? code.length() : 0);

        try {
            Python3ProcessPool pool = getProcessPool();
            if (pool == null) {
                String errorMsg = "Python 3 process pool is not initialized";
                LOGGER.error(errorMsg);
                Map<String, Object> result = new HashMap<>();
                result.put("errors", Collections.emptyList());
                result.put("error", errorMsg);
                return result;
            }

            // Execute syntax check via pool
            Python3Result result = pool.checkSyntax(code != null ? code : "");

            if (result.isSuccess()) {
                Object resultObj = result.getResult();

                // Result should be a Map with "errors" list
                if (resultObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resultMap = (Map<String, Object>) resultObj;
                    LOGGER.debug("Syntax check completed, found {} errors",
                            resultMap.containsKey("errors") && resultMap.get("errors") instanceof List
                                    ? ((List<?>) resultMap.get("errors")).size() : 0);
                    return resultMap;
                } else {
                    // Unexpected result format
                    Map<String, Object> fallback = new HashMap<>();
                    fallback.put("errors", Collections.emptyList());
                    LOGGER.warn("Syntax check returned unexpected format: {}", resultObj);
                    return fallback;
                }
            } else {
                // Syntax check itself failed
                String errorMsg = "Syntax check failed: " + result.getError();
                LOGGER.error(errorMsg);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("errors", Collections.emptyList());
                errorResult.put("error", errorMsg);
                return errorResult;
            }

        } catch (Exception e) {
            LOGGER.error("Failed to check syntax", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("errors", Collections.emptyList());
            errorResult.put("error", e.getMessage());
            return errorResult;
        }
    }

    /**
     * Get code completions at cursor position.
     * Uses Jedi library for intelligent code completion.
     *
     * @param code   Python code
     * @param line   Line number (1-based)
     * @param column Column number (0-based)
     * @return Dictionary with "completions" list containing completion details
     */
    public Map<String, Object> getCompletions(String code, int line, int column) {
        LOGGER.debug("getCompletions() called at line {}, column {}", line, column);

        try {
            Python3ProcessPool pool = getProcessPool();
            if (pool == null) {
                String errorMsg = "Python 3 process pool is not initialized";
                LOGGER.error(errorMsg);
                Map<String, Object> result = new HashMap<>();
                result.put("completions", Collections.emptyList());
                result.put("error", errorMsg);
                return result;
            }

            // Execute completions request via pool
            Python3Result result = pool.getCompletions(code != null ? code : "", line, column);

            if (result.isSuccess()) {
                Object resultObj = result.getResult();

                // Result should be a Map with "completions" list
                if (resultObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resultMap = (Map<String, Object>) resultObj;
                    LOGGER.debug("Completions request completed, found {} completions",
                            resultMap.containsKey("completions") && resultMap.get("completions") instanceof List
                                    ? ((List<?>) resultMap.get("completions")).size() : 0);
                    return resultMap;
                } else {
                    // Unexpected result format
                    Map<String, Object> fallback = new HashMap<>();
                    fallback.put("completions", Collections.emptyList());
                    LOGGER.warn("Completions request returned unexpected format: {}", resultObj);
                    return fallback;
                }
            } else {
                // Completions request itself failed
                String errorMsg = "Completions request failed: " + result.getError();
                LOGGER.error(errorMsg);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("completions", Collections.emptyList());
                errorResult.put("error", errorMsg);
                return errorResult;
            }

        } catch (Exception e) {
            LOGGER.error("Failed to get completions", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("completions", Collections.emptyList());
            errorResult.put("error", e.getMessage());
            return errorResult;
        }
    }
}
