package com.inductiveautomation.ignition.examples.python3.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Collects performance metrics for Python3 script executions.
 * Tracks execution counts, errors, timing, and Gateway impact.
 */
public class Python3MetricsCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(Python3MetricsCollector.class);

    // Execution counters
    private final AtomicLong totalExecutions = new AtomicLong(0);
    private final AtomicLong successfulExecutions = new AtomicLong(0);
    private final AtomicLong failedExecutions = new AtomicLong(0);

    // Timing metrics (milliseconds)
    private final AtomicLong totalExecutionTime = new AtomicLong(0);
    private final AtomicLong minExecutionTime = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxExecutionTime = new AtomicLong(0);

    // Pool metrics
    private final AtomicInteger currentPoolSize = new AtomicInteger(0);
    private final AtomicInteger activeExecutions = new AtomicInteger(0);
    private final AtomicLong poolWaitTimeTotal = new AtomicLong(0);
    private final AtomicInteger poolWaitCount = new AtomicInteger(0);

    // Error tracking
    private final Map<String, AtomicInteger> errorCounts = new ConcurrentHashMap<>();

    // Start time for uptime calculation
    private final long startTime = System.currentTimeMillis();

    /**
     * Record a successful execution
     */
    public void recordExecution(long executionTimeMs) {
        totalExecutions.incrementAndGet();
        successfulExecutions.incrementAndGet();
        totalExecutionTime.addAndGet(executionTimeMs);

        // Update min/max
        updateMin(minExecutionTime, executionTimeMs);
        updateMax(maxExecutionTime, executionTimeMs);

        LOGGER.debug("Recorded execution: {}ms (total: {})", executionTimeMs, totalExecutions.get());
    }

    /**
     * Record a failed execution
     */
    public void recordFailure(String errorType, long executionTimeMs) {
        totalExecutions.incrementAndGet();
        failedExecutions.incrementAndGet();
        totalExecutionTime.addAndGet(executionTimeMs);

        // Track error types
        errorCounts.computeIfAbsent(errorType, k -> new AtomicInteger(0)).incrementAndGet();

        LOGGER.debug("Recorded failure: {} ({}ms)", errorType, executionTimeMs);
    }

    /**
     * Record pool wait time
     */
    public void recordPoolWait(long waitTimeMs) {
        poolWaitTimeTotal.addAndGet(waitTimeMs);
        poolWaitCount.incrementAndGet();
    }

    /**
     * Update pool size
     */
    public void setPoolSize(int size) {
        currentPoolSize.set(size);
    }

    /**
     * Increment active execution count
     */
    public void incrementActiveExecutions() {
        activeExecutions.incrementAndGet();
    }

    /**
     * Decrement active execution count
     */
    public void decrementActiveExecutions() {
        activeExecutions.decrementAndGet();
    }

    /**
     * Get comprehensive metrics
     */
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Execution metrics
        long total = totalExecutions.get();
        long successful = successfulExecutions.get();
        long failed = failedExecutions.get();

        metrics.put("total_executions", total);
        metrics.put("successful_executions", successful);
        metrics.put("failed_executions", failed);
        metrics.put("success_rate", total > 0 ? (double) successful / total * 100.0 : 100.0);

        // Timing metrics
        long totalTime = totalExecutionTime.get();
        metrics.put("total_execution_time_ms", totalTime);
        metrics.put("average_execution_time_ms", total > 0 ? totalTime / total : 0);
        metrics.put("min_execution_time_ms", minExecutionTime.get() == Long.MAX_VALUE ? 0 : minExecutionTime.get());
        metrics.put("max_execution_time_ms", maxExecutionTime.get());

        // Pool metrics
        metrics.put("pool_size", currentPoolSize.get());
        metrics.put("active_executions", activeExecutions.get());
        metrics.put("pool_utilization", currentPoolSize.get() > 0 ?
            (double) activeExecutions.get() / currentPoolSize.get() * 100.0 : 0.0);

        int waitCount = poolWaitCount.get();
        metrics.put("pool_wait_count", waitCount);
        metrics.put("average_pool_wait_ms", waitCount > 0 ? poolWaitTimeTotal.get() / waitCount : 0);

        // Error metrics
        Map<String, Integer> errors = new HashMap<>();
        errorCounts.forEach((key, value) -> errors.put(key, value.get()));
        metrics.put("error_counts", errors);

        // Uptime
        long uptimeMs = System.currentTimeMillis() - startTime;
        metrics.put("uptime_ms", uptimeMs);
        metrics.put("uptime_hours", uptimeMs / (1000.0 * 60 * 60));

        // Health score (0-100, based on success rate and pool availability)
        double successRate = total > 0 ? (double) successful / total * 100.0 : 100.0;
        double poolAvailability = currentPoolSize.get() > 0 ?
            (double) (currentPoolSize.get() - activeExecutions.get()) / currentPoolSize.get() * 100.0 : 100.0;
        double healthScore = (successRate * 0.7) + (poolAvailability * 0.3);  // Weighted average
        metrics.put("health_score", Math.round(healthScore));

        return metrics;
    }

    /**
     * Get Gateway impact assessment
     */
    public Map<String, Object> getGatewayImpact() {
        Map<String, Object> impact = new HashMap<>();

        long total = totalExecutions.get();
        long totalTime = totalExecutionTime.get();
        long uptimeMs = System.currentTimeMillis() - startTime;

        // Execution rate (executions per minute)
        double executionRate = uptimeMs > 0 ? (double) total / (uptimeMs / 60000.0) : 0.0;
        impact.put("executions_per_minute", Math.round(executionRate * 100.0) / 100.0);

        // Average CPU time consumed
        impact.put("average_cpu_time_ms", total > 0 ? totalTime / total : 0);

        // Pool contention (wait time indicates resource pressure)
        int waitCount = poolWaitCount.get();
        impact.put("pool_contention_events", waitCount);
        impact.put("average_wait_time_ms", waitCount > 0 ? poolWaitTimeTotal.get() / waitCount : 0);

        // Resource utilization
        double poolUtil = currentPoolSize.get() > 0 ?
            (double) activeExecutions.get() / currentPoolSize.get() * 100.0 : 0.0;
        impact.put("pool_utilization_percent", Math.round(poolUtil * 100.0) / 100.0);

        // Impact level assessment
        String impactLevel;
        if (poolUtil > 80 || executionRate > 50) {
            impactLevel = "HIGH";
        } else if (poolUtil > 50 || executionRate > 20) {
            impactLevel = "MEDIUM";
        } else {
            impactLevel = "LOW";
        }
        impact.put("impact_level", impactLevel);

        return impact;
    }

    /**
     * Reset all metrics
     */
    public void reset() {
        totalExecutions.set(0);
        successfulExecutions.set(0);
        failedExecutions.set(0);
        totalExecutionTime.set(0);
        minExecutionTime.set(Long.MAX_VALUE);
        maxExecutionTime.set(0);
        poolWaitTimeTotal.set(0);
        poolWaitCount.set(0);
        errorCounts.clear();

        LOGGER.info("Metrics reset");
    }

    /**
     * Atomic min update
     */
    private void updateMin(AtomicLong atomic, long value) {
        long current;
        do {
            current = atomic.get();
            if (value >= current) {
                break;
            }
        } while (!atomic.compareAndSet(current, value));
    }

    /**
     * Atomic max update
     */
    private void updateMax(AtomicLong atomic, long value) {
        long current;
        do {
            current = atomic.get();
            if (value <= current) {
                break;
            }
        } while (!atomic.compareAndSet(current, value));
    }
}
