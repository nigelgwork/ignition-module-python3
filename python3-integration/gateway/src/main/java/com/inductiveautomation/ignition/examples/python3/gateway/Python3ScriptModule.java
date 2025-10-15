package com.inductiveautomation.ignition.examples.python3.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gateway implementation of Python 3 scripting functions.
 * Executes Python code directly via the process pool.
 * Public methods are automatically exposed as scripting functions.
 */
public class Python3ScriptModule {

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
     * Execute Python 3 code and return the result.
     *
     * @param code Python code to execute
     * @return Result of execution
     */
    public Object exec(String code) {
        return exec(code, Collections.emptyMap());
    }

    /**
     * Execute Python 3 code with variables and return the result.
     *
     * @param code      Python code to execute
     * @param variables Dictionary of variables to pass to Python
     * @return Result of execution
     */
    public Object exec(String code, Map<String, Object> variables) {
        LOGGER.debug("exec() called with code length: {}", code != null ? code.length() : 0);

        try {
            Python3ProcessPool pool = getProcessPool();
            if (pool == null) {
                String errorMsg = "Python 3 process pool is not initialized. Check Gateway logs for initialization errors.";
                LOGGER.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            LOGGER.debug("Executing Python code via process pool");
            Python3Result result = pool.execute(code, variables != null ? variables : Collections.emptyMap());

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
    public Object eval(String expression) {
        return eval(expression, Collections.emptyMap());
    }

    /**
     * Evaluate a Python 3 expression with variables and return the result.
     *
     * @param expression Python expression to evaluate
     * @param variables  Dictionary of variables to pass to Python
     * @return Result of expression
     */
    public Object eval(String expression, Map<String, Object> variables) {
        LOGGER.debug("eval() called with expression: {}", expression);

        try {
            Python3ProcessPool pool = getProcessPool();
            if (pool == null) {
                String errorMsg = "Python 3 process pool is not initialized. Check Gateway logs for initialization errors.";
                LOGGER.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            LOGGER.debug("Evaluating Python expression via process pool");
            Python3Result result = pool.evaluate(expression, variables != null ? variables : Collections.emptyMap());

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
    public Object callModule(String moduleName, String functionName, List<Object> args) {
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
        LOGGER.debug("callModule() called: {}.{}()", moduleName, functionName);

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
                kwargs != null ? kwargs : Collections.emptyMap()
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

            Python3Result result = pool.execute("import sys; result = sys.version", Collections.emptyMap());

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
     * Execute a simple Python 3 example (for testing).
     *
     * @return Example result
     */
    public String example() {
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
}
