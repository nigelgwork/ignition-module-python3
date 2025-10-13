package com.inductiveautomation.ignition.examples.python3.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages a pool of Python 3 processes for efficient execution.
 * Processes are kept alive and reused across multiple script executions.
 */
public class Python3ProcessPool {

    private static final Logger logger = LoggerFactory.getLogger(Python3ProcessPool.class);

    private final String pythonPath;
    private final int poolSize;
    private final BlockingQueue<Python3Executor> availableExecutors;
    private final CopyOnWriteArrayList<Python3Executor> allExecutors;
    private final ScheduledExecutorService healthCheckExecutor;
    private volatile boolean isShutdown = false;
    private final AtomicInteger executorIdCounter = new AtomicInteger(0);

    /**
     * Create a new process pool
     *
     * @param pythonPath Path to Python 3 executable
     * @param poolSize   Number of processes to maintain
     * @throws IOException if processes cannot be started
     */
    public Python3ProcessPool(String pythonPath, int poolSize) throws IOException {
        this.pythonPath = pythonPath;
        this.poolSize = poolSize;
        this.availableExecutors = new LinkedBlockingQueue<>(poolSize);
        this.allExecutors = new CopyOnWriteArrayList<>();

        logger.info("Initializing Python 3 process pool with {} processes", poolSize);

        // Create initial pool
        for (int i = 0; i < poolSize; i++) {
            Python3Executor executor = createExecutor();
            allExecutors.add(executor);
            availableExecutors.offer(executor);
        }

        // Start health check scheduler
        healthCheckExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Python3-HealthCheck");
            t.setDaemon(true);
            return t;
        });

        healthCheckExecutor.scheduleAtFixedRate(
                this::performHealthCheck,
                30, // Initial delay
                30, // Period
                TimeUnit.SECONDS
        );

        logger.info("Python 3 process pool initialized successfully");
    }

    /**
     * Create a new executor instance
     */
    private Python3Executor createExecutor() throws IOException {
        int id = executorIdCounter.incrementAndGet();
        logger.debug("Creating Python executor #{}", id);

        try {
            Python3Executor executor = new Python3Executor(pythonPath);
            logger.info("Python executor #{} created successfully", id);
            return executor;
        } catch (IOException e) {
            logger.error("Failed to create Python executor #{}", id, e);
            throw e;
        }
    }

    /**
     * Borrow an executor from the pool
     *
     * @param timeout  Maximum time to wait
     * @param timeUnit Time unit
     * @return An available executor
     * @throws InterruptedException if interrupted while waiting
     * @throws TimeoutException     if no executor becomes available in time
     * @throws IllegalStateException if pool is shutdown
     */
    public Python3Executor borrowExecutor(long timeout, TimeUnit timeUnit)
            throws InterruptedException, TimeoutException {

        if (isShutdown) {
            throw new IllegalStateException("Process pool is shutdown");
        }

        Python3Executor executor = availableExecutors.poll(timeout, timeUnit);

        if (executor == null) {
            throw new TimeoutException("No Python executor available within " + timeout + " " + timeUnit);
        }

        // Double-check executor is healthy
        if (!executor.isHealthy()) {
            logger.warn("Borrowed executor is unhealthy, attempting to replace");
            try {
                replaceExecutor(executor);
                // Try to borrow again (non-blocking)
                executor = availableExecutors.poll();
                if (executor == null) {
                    throw new TimeoutException("No healthy executor available");
                }
            } catch (IOException e) {
                logger.error("Failed to replace unhealthy executor", e);
                throw new TimeoutException("No healthy executor available");
            }
        }

        logger.debug("Executor borrowed, {} available", availableExecutors.size());
        return executor;
    }

    /**
     * Return an executor to the pool
     *
     * @param executor The executor to return
     */
    public void returnExecutor(Python3Executor executor) {
        if (executor == null || isShutdown) {
            return;
        }

        // Check if executor is still healthy
        if (!executor.isHealthy()) {
            logger.warn("Returned executor is unhealthy, will be replaced");
            try {
                replaceExecutor(executor);
            } catch (IOException e) {
                logger.error("Failed to replace unhealthy executor", e);
            }
        } else {
            availableExecutors.offer(executor);
            logger.debug("Executor returned, {} available", availableExecutors.size());
        }
    }

    /**
     * Execute code using a pooled executor
     *
     * @param code      Python code to execute
     * @param variables Variables to pass
     * @return Result
     * @throws Python3Exception if execution fails
     */
    public Python3Result execute(String code, java.util.Map<String, Object> variables) throws Python3Exception {
        Python3Executor executor = null;
        try {
            executor = borrowExecutor(30, TimeUnit.SECONDS);
            return executor.execute(code, variables);
        } catch (InterruptedException | TimeoutException e) {
            throw new Python3Exception("Failed to acquire executor: " + e.getMessage(), e);
        } finally {
            if (executor != null) {
                returnExecutor(executor);
            }
        }
    }

    /**
     * Evaluate expression using a pooled executor
     */
    public Python3Result evaluate(String expression, java.util.Map<String, Object> variables) throws Python3Exception {
        Python3Executor executor = null;
        try {
            executor = borrowExecutor(30, TimeUnit.SECONDS);
            return executor.evaluate(expression, variables);
        } catch (InterruptedException | TimeoutException e) {
            throw new Python3Exception("Failed to acquire executor: " + e.getMessage(), e);
        } finally {
            if (executor != null) {
                returnExecutor(executor);
            }
        }
    }

    /**
     * Call module function using a pooled executor
     */
    public Python3Result callModule(String moduleName, String functionName,
                                     java.util.List<Object> args,
                                     java.util.Map<String, Object> kwargs) throws Python3Exception {
        Python3Executor executor = null;
        try {
            executor = borrowExecutor(30, TimeUnit.SECONDS);
            return executor.callModule(moduleName, functionName, args, kwargs);
        } catch (InterruptedException | TimeoutException e) {
            throw new Python3Exception("Failed to acquire executor: " + e.getMessage(), e);
        } finally {
            if (executor != null) {
                returnExecutor(executor);
            }
        }
    }

    /**
     * Replace an unhealthy executor with a new one
     */
    private synchronized void replaceExecutor(Python3Executor oldExecutor) throws IOException {
        logger.info("Replacing unhealthy executor");

        // Shutdown old executor
        try {
            oldExecutor.shutdown();
        } catch (Exception e) {
            logger.error("Error shutting down old executor", e);
        }

        // Remove from all executors list
        allExecutors.remove(oldExecutor);

        // Create new executor
        Python3Executor newExecutor = createExecutor();
        allExecutors.add(newExecutor);
        availableExecutors.offer(newExecutor);

        logger.info("Executor replaced successfully");
    }

    /**
     * Perform health check on all executors
     */
    private void performHealthCheck() {
        if (isShutdown) {
            return;
        }

        logger.debug("Performing health check on {} executors", allExecutors.size());

        for (Python3Executor executor : allExecutors) {
            if (!executor.isHealthy()) {
                logger.warn("Executor failed health check, attempting to replace");
                try {
                    replaceExecutor(executor);
                } catch (IOException e) {
                    logger.error("Failed to replace unhealthy executor during health check", e);
                }
            }
        }
    }

    /**
     * Get pool statistics
     */
    public PoolStats getStats() {
        return new PoolStats(
                poolSize,
                availableExecutors.size(),
                poolSize - availableExecutors.size(),
                (int) allExecutors.stream().filter(Python3Executor::isHealthy).count()
        );
    }

    /**
     * Shutdown the process pool
     */
    public void shutdown() {
        logger.info("Shutting down Python 3 process pool");
        isShutdown = true;

        // Stop health check
        healthCheckExecutor.shutdownNow();

        // Shutdown all executors
        for (Python3Executor executor : allExecutors) {
            try {
                executor.shutdown();
            } catch (Exception e) {
                logger.error("Error shutting down executor", e);
            }
        }

        allExecutors.clear();
        availableExecutors.clear();

        logger.info("Python 3 process pool shutdown complete");
    }

    /**
     * Check if pool is shutdown
     */
    public boolean isShutdown() {
        return isShutdown;
    }

    /**
     * Pool statistics
     */
    public static class PoolStats {
        public final int totalSize;
        public final int available;
        public final int inUse;
        public final int healthy;

        public PoolStats(int totalSize, int available, int inUse, int healthy) {
            this.totalSize = totalSize;
            this.available = available;
            this.inUse = inUse;
            this.healthy = healthy;
        }

        @Override
        public String toString() {
            return String.format("PoolStats{total=%d, available=%d, inUse=%d, healthy=%d}",
                    totalSize, available, inUse, healthy);
        }
    }
}
