package com.inductiveautomation.ignition.examples.python3.designer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.Map;
import java.util.function.Consumer;

/**
 * SwingWorker for executing Python code asynchronously via REST API.
 *
 * <p>This worker prevents Designer UI blocking by running REST API calls
 * in a background thread and updating the UI on the Swing event dispatch thread.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * Python3ExecutionWorker worker = new Python3ExecutionWorker(
 *     restClient,
 *     "result = 2 + 2",
 *     new HashMap&lt;&gt;(),
 *     result -&gt; {
 *         if (result.isSuccess()) {
 *             outputPanel.setText(result.getResult());
 *         } else {
 *             errorPanel.setText(result.getError());
 *         }
 *     },
 *     error -&gt; errorPanel.setText("Execution failed: " + error.getMessage())
 * );
 * worker.execute();
 * </pre>
 */
public class Python3ExecutionWorker extends SwingWorker<ExecutionResult, Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Python3ExecutionWorker.class);

    private final Python3RestClient restClient;
    private final String code;
    private final Map<String, Object> variables;
    private final boolean isEvaluation;
    private final Consumer<ExecutionResult> onSuccess;
    private final Consumer<Exception> onError;

    /**
     * Creates a worker for executing Python code.
     *
     * @param restClient the REST API client
     * @param code the Python code to execute
     * @param variables variables to pass to Python environment
     * @param onSuccess callback for successful execution (runs on Swing thread)
     * @param onError callback for errors (runs on Swing thread)
     */
    public Python3ExecutionWorker(
            Python3RestClient restClient,
            String code,
            Map<String, Object> variables,
            Consumer<ExecutionResult> onSuccess,
            Consumer<Exception> onError) {
        this.restClient = restClient;
        this.code = code;
        this.variables = variables;
        this.isEvaluation = false;
        this.onSuccess = onSuccess;
        this.onError = onError;
    }

    /**
     * Creates a worker for executing or evaluating Python code.
     *
     * @param restClient the REST API client
     * @param code the Python code or expression
     * @param variables variables to pass to Python environment
     * @param isEvaluation true to evaluate as expression, false to execute as code
     * @param onSuccess callback for successful execution (runs on Swing thread)
     * @param onError callback for errors (runs on Swing thread)
     */
    public Python3ExecutionWorker(
            Python3RestClient restClient,
            String code,
            Map<String, Object> variables,
            boolean isEvaluation,
            Consumer<ExecutionResult> onSuccess,
            Consumer<Exception> onError) {
        this.restClient = restClient;
        this.code = code;
        this.variables = variables;
        this.isEvaluation = isEvaluation;
        this.onSuccess = onSuccess;
        this.onError = onError;
    }

    /**
     * Executes the Python code in a background thread.
     *
     * @return the execution result
     * @throws Exception if execution fails
     */
    @Override
    protected ExecutionResult doInBackground() throws Exception {
        LOGGER.debug("Executing Python code in background thread");

        try {
            if (isEvaluation) {
                return restClient.evaluateExpression(code, variables);
            } else {
                return restClient.executeCode(code, variables);
            }
        } catch (Exception e) {
            LOGGER.error("Python execution failed", e);
            throw e;
        }
    }

    /**
     * Called on the Swing event dispatch thread when execution completes.
     * Invokes the appropriate callback (onSuccess or onError).
     */
    @Override
    protected void done() {
        if (isCancelled()) {
            LOGGER.debug("Python execution was cancelled");
            if (onError != null) {
                onError.accept(new InterruptedException("Execution cancelled by user"));
            }
            return;
        }

        try {
            ExecutionResult result = get();

            LOGGER.debug("Python execution completed: {}", result);

            if (onSuccess != null) {
                onSuccess.accept(result);
            }

        } catch (Exception e) {
            LOGGER.error("Error retrieving execution result", e);

            if (onError != null) {
                onError.accept(e);
            }
        }
    }

    /**
     * Builder pattern for creating Python3ExecutionWorker instances.
     */
    public static class Builder {
        private Python3RestClient restClient;
        private String code;
        private Map<String, Object> variables;
        private boolean isEvaluation = false;
        private Consumer<ExecutionResult> onSuccess;
        private Consumer<Exception> onError;

        public Builder restClient(Python3RestClient restClient) {
            this.restClient = restClient;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder variables(Map<String, Object> variables) {
            this.variables = variables;
            return this;
        }

        public Builder evaluation(boolean isEvaluation) {
            this.isEvaluation = isEvaluation;
            return this;
        }

        public Builder onSuccess(Consumer<ExecutionResult> onSuccess) {
            this.onSuccess = onSuccess;
            return this;
        }

        public Builder onError(Consumer<Exception> onError) {
            this.onError = onError;
            return this;
        }

        public Python3ExecutionWorker build() {
            if (restClient == null || code == null) {
                throw new IllegalStateException("restClient and code are required");
            }
            return new Python3ExecutionWorker(
                    restClient, code, variables, isEvaluation, onSuccess, onError);
        }

        public void execute() {
            build().execute();
        }
    }
}
