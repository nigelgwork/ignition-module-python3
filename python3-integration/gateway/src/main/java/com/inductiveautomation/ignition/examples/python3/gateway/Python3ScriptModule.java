package com.inductiveautomation.ignition.examples.python3.gateway;

import com.inductiveautomation.ignition.common.script.hints.ScriptArg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scripting functions for Python 3 integration.
 * These functions are exposed to Ignition scripts under system.python3.*
 */
public class Python3ScriptModule {

    private static final Logger logger = LoggerFactory.getLogger(Python3ScriptModule.class);
    private final Python3ProcessPool processPool;
    private final PythonDistributionManager distributionManager;

    public Python3ScriptModule(Python3ProcessPool processPool, PythonDistributionManager distributionManager) {
        this.processPool = processPool;
        this.distributionManager = distributionManager;
    }

    /**
     * Execute Python 3 code and return the result.
     * Code should set a 'result' variable or return will be all local variables.
     *
     * @param code Python code to execute
     * @return Result of execution
     */
    
    public Object exec(
            @ScriptArg("code") String code) {

        return exec(code, Collections.emptyMap());
    }

    /**
     * Execute Python 3 code with variables and return the result.
     *
     * @param code      Python code to execute
     * @param variables Dictionary of variables to pass to Python
     * @return Result of execution
     */
    
    public Object exec(
            @ScriptArg("code") String code,
            @ScriptArg("variables") Map<String, Object> variables) {

        try {
            if (variables == null) {
                variables = Collections.emptyMap();
            }

            Python3Result result = processPool.execute(code, variables);

            if (result.isSuccess()) {
                return result.getResult();
            } else {
                String errorMsg = "Python error: " + result.getError();
                if (result.getTraceback() != null) {
                    errorMsg += "\n" + result.getTraceback();
                }
                logger.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

        } catch (Python3Exception e) {
            logger.error("Failed to execute Python code", e);
            throw new RuntimeException("Failed to execute Python code: " + e.getMessage(), e);
        }
    }

    /**
     * Evaluate a Python 3 expression and return the result.
     *
     * @param expression Python expression to evaluate
     * @return Result of expression
     */
    
    public Object eval(
            @ScriptArg("expression") String expression) {

        return eval(expression, Collections.emptyMap());
    }

    /**
     * Evaluate a Python 3 expression with variables and return the result.
     *
     * @param expression Python expression to evaluate
     * @param variables  Dictionary of variables to pass to Python
     * @return Result of expression
     */
    
    public Object eval(
            @ScriptArg("expression") String expression,
            @ScriptArg("variables") Map<String, Object> variables) {

        try {
            if (variables == null) {
                variables = Collections.emptyMap();
            }

            Python3Result result = processPool.evaluate(expression, variables);

            if (result.isSuccess()) {
                return result.getResult();
            } else {
                String errorMsg = "Python error: " + result.getError();
                if (result.getTraceback() != null) {
                    errorMsg += "\n" + result.getTraceback();
                }
                logger.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

        } catch (Python3Exception e) {
            logger.error("Failed to evaluate Python expression", e);
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
    
    public Object callModule(
            @ScriptArg("moduleName") String moduleName,
            @ScriptArg("functionName") String functionName,
            @ScriptArg("args") List<Object> args) {

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
    
    public Object callModule(
            @ScriptArg("moduleName") String moduleName,
            @ScriptArg("functionName") String functionName,
            @ScriptArg("args") List<Object> args,
            @ScriptArg("kwargs") Map<String, Object> kwargs) {

        try {
            if (args == null) {
                args = Collections.emptyList();
            }
            if (kwargs == null) {
                kwargs = Collections.emptyMap();
            }

            Python3Result result = processPool.callModule(moduleName, functionName, args, kwargs);

            if (result.isSuccess()) {
                return result.getResult();
            } else {
                String errorMsg = "Python error: " + result.getError();
                if (result.getTraceback() != null) {
                    errorMsg += "\n" + result.getTraceback();
                }
                logger.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

        } catch (Python3Exception e) {
            logger.error("Failed to call Python module function", e);
            throw new RuntimeException("Failed to call Python module function: " + e.getMessage(), e);
        }
    }

    /**
     * Check if Python 3 is available and the process pool is healthy.
     *
     * @return true if Python 3 is available
     */
    
    public boolean isAvailable() {
        return processPool != null && !processPool.isShutdown();
    }

    /**
     * Get Python 3 version information.
     *
     * @return Dictionary with version information
     */
    
    public Map<String, Object> getVersion() {
        try {
            Python3Result result = processPool.execute("import sys; result = sys.version", Collections.emptyMap());

            if (result.isSuccess()) {
                Map<String, Object> versionInfo = new HashMap<>();
                versionInfo.put("version", result.getResult());
                versionInfo.put("available", true);
                return versionInfo;
            } else {
                Map<String, Object> versionInfo = new HashMap<>();
                versionInfo.put("available", false);
                versionInfo.put("error", result.getError());
                return versionInfo;
            }

        } catch (Exception e) {
            logger.error("Failed to get Python version", e);
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
        Python3ProcessPool.PoolStats stats = processPool.getStats();

        Map<String, Object> statsMap = new HashMap<>();
        statsMap.put("totalSize", stats.totalSize);
        statsMap.put("available", stats.available);
        statsMap.put("inUse", stats.inUse);
        statsMap.put("healthy", stats.healthy);

        return statsMap;
    }

    /**
     * Execute a simple Python 3 example (for testing).
     *
     * @return Example result
     */
    
    public String example() {
        try {
            // Test basic math
            Object result = eval("2 ** 100");
            return "Python 3 is working! 2^100 = " + result;

        } catch (Exception e) {
            return "Python 3 error: " + e.getMessage();
        }
    }

    /**
     * Get Python distribution status and installation info.
     *
     * @return Dictionary with distribution information
     */
    
    public Map<String, Object> getDistributionInfo() {
        if (distributionManager != null) {
            return distributionManager.getStatus();
        } else {
            Map<String, Object> info = new HashMap<>();
            info.put("available", false);
            info.put("error", "Distribution manager not initialized");
            return info;
        }
    }
}
