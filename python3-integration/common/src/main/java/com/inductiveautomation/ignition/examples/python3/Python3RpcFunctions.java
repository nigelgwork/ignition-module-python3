package com.inductiveautomation.ignition.examples.python3;

import java.util.List;
import java.util.Map;

/**
 * RPC interface for Python 3 scripting functions.
 *
 * This interface defines all Python 3 functions that can be called from Designer/Client scopes.
 * The Gateway provides the implementation, and Designer/Client call via RPC.
 */
public interface Python3RpcFunctions {

    /**
     * Execute Python code with optional variables.
     *
     * @param code Python code to execute
     * @param variables Variables to pass to Python (can be null)
     * @return Result of the execution (last expression value)
     * @throws Exception if execution fails
     */
    Object exec(String code, Map<String, Object> variables) throws Exception;

    /**
     * Evaluate a Python expression.
     *
     * @param expression Python expression to evaluate
     * @param variables Variables to pass to Python (can be null)
     * @return Result of the evaluation
     * @throws Exception if evaluation fails
     */
    Object eval(String expression, Map<String, Object> variables) throws Exception;

    /**
     * Call a function from a Python module.
     *
     * @param moduleName Python module name (e.g., "math")
     * @param functionName Function name (e.g., "sqrt")
     * @param args Arguments to pass to the function
     * @return Result of the function call
     * @throws Exception if call fails
     */
    Object callModule(String moduleName, String functionName, List<Object> args) throws Exception;

    /**
     * Get Python version information.
     *
     * @return Map with 'available' (boolean) and 'version' (string) keys
     */
    Map<String, Object> getVersion();

    /**
     * Get process pool statistics.
     *
     * @return Map with 'totalSize', 'available', 'inUse', 'healthy' keys
     */
    Map<String, Object> getPoolStats();

    /**
     * Check if Python 3 is available.
     *
     * @return true if Python 3 is available and pool is healthy
     */
    boolean isAvailable();

    /**
     * Run example Python code (for testing).
     *
     * @return Result string from example execution
     * @throws Exception if example fails
     */
    String example() throws Exception;

    /**
     * Get information about the embedded Python distribution.
     *
     * @return Map with distribution information
     */
    Map<String, Object> getDistributionInfo();
}
