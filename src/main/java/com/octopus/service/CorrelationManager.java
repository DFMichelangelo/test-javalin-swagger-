package com.octopus.service;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Manages the correlation between requests and responses using correlation IDs.
 * This class is responsible only for managing CompletableFutures, not for
 * sending requests or receiving responses.
 *
 * Includes automatic cleanup of orphaned futures and metrics tracking.
 *
 * @param <R> The response type
 */
@Slf4j
public class CorrelationManager<R> {
    private record PendingRequest<R>(CompletableFuture<R> future, Instant registeredAt) {}

    private final ConcurrentHashMap<String, PendingRequest<R>> pendingRequests;
    private final long timeoutSeconds;
    private final Function<String, R> timeoutResponseFactory;
    private final ScheduledExecutorService cleanupScheduler;
    private final long cleanupIntervalMinutes;

    /**
     * Creates a CorrelationManager with default cleanup interval of 5 minutes.
     */
    public CorrelationManager(long timeoutSeconds, Function<String, R> timeoutResponseFactory) {
        this(timeoutSeconds, timeoutResponseFactory, 5);
    }

    /**
     * Creates a CorrelationManager with custom cleanup interval.
     *
     * @param timeoutSeconds Timeout for each request in seconds
     * @param timeoutResponseFactory Factory to create timeout responses
     * @param cleanupIntervalMinutes How often to run cleanup of orphaned futures
     */
    public CorrelationManager(long timeoutSeconds, Function<String, R> timeoutResponseFactory, long cleanupIntervalMinutes) {
        this.pendingRequests = new ConcurrentHashMap<>();
        this.timeoutSeconds = timeoutSeconds;
        this.timeoutResponseFactory = timeoutResponseFactory;
        this.cleanupIntervalMinutes = cleanupIntervalMinutes;
        this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "CorrelationManager-Cleanup");
            thread.setDaemon(true);
            return thread;
        });

        // Schedule periodic cleanup
        startCleanupTask();
        log.info("CorrelationManager initialized with {}s timeout and {}min cleanup interval",
                timeoutSeconds, cleanupIntervalMinutes);
    }

    /**
     * Starts the periodic cleanup task.
     */
    private void startCleanupTask() {
        cleanupScheduler.scheduleAtFixedRate(
                this::cleanupOrphanedFutures,
                cleanupIntervalMinutes,
                cleanupIntervalMinutes,
                TimeUnit.MINUTES
        );
    }

    /**
     * Registers a new pending request and returns a CompletableFuture that will
     * be completed when a response with the same correlation ID is received.
     *
     * @param correlationId The correlation ID
     * @return A CompletableFuture that will be completed with the response
     */
    public CompletableFuture<R> register(String correlationId) {
        log.info("Registering pending request with correlation ID: {}", correlationId);

        CompletableFuture<R> baseFuture = new CompletableFuture<>();

        // Set up timeout with guaranteed cleanup
        CompletableFuture<R> timeoutHandledFuture = baseFuture
                .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .whenComplete((result, ex) -> {
                    if (ex instanceof TimeoutException) {
                        log.warn("Request timed out for correlation ID: {}", correlationId);
                        // Remove from pending requests - this ensures cleanup even if exceptionally fails
                        pendingRequests.remove(correlationId);
                    }
                })
                .exceptionally(ex -> {
                    // Handle timeout exception by creating a timeout response
                    // Note: orTimeout() throws TimeoutException directly, not wrapped
                    if (ex instanceof TimeoutException || ex.getCause() instanceof TimeoutException) {
                        return timeoutResponseFactory != null
                                ? timeoutResponseFactory.apply(correlationId)
                                : null;
                    }
                    // For other exceptions, remove and rethrow
                    pendingRequests.remove(correlationId);
                    // Rethrow the exception without wrapping it again
                    if (ex instanceof CompletionException) {
                        throw (CompletionException) ex;
                    }
                    throw new CompletionException(ex);
                });

        // Store the base future (which we'll complete/cancel) but return the timeout-handled future
        PendingRequest<R> pendingRequest = new PendingRequest<>(baseFuture, Instant.now());
        pendingRequests.put(correlationId, pendingRequest);

        return timeoutHandledFuture;
    }

    /**
     * Completes a pending request with the given response.
     * If no pending request is found for the correlation ID, logs a warning.
     *
     * @param correlationId The correlation ID
     * @param response The response to complete the future with
     * @return true if a pending request was found and completed, false otherwise
     */
    public boolean complete(String correlationId, R response) {
        log.info("Completing request for correlation ID: {}", correlationId);

        PendingRequest<R> pendingRequest = pendingRequests.remove(correlationId);

        if (pendingRequest != null) {
            pendingRequest.future().complete(response);
            log.info("Completed future for correlation ID: {}", correlationId);
            return true;
        } else {
            log.warn("No pending request found for correlation ID: {}", correlationId);
            return false;
        }
    }

    /**
     * Completes a pending request exceptionally with the given error.
     * This allows external code to fail a request with a specific exception.
     *
     * @param correlationId The correlation ID
     * @param throwable The exception to complete the future with
     * @return true if a pending request was found and completed, false otherwise
     */
    public boolean completeExceptionally(String correlationId, Throwable throwable) {
        log.warn("Completing request exceptionally for correlation ID: {}", correlationId, throwable);

        PendingRequest<R> pendingRequest = pendingRequests.remove(correlationId);

        if (pendingRequest != null) {
            pendingRequest.future().completeExceptionally(throwable);
            log.info("Completed future exceptionally for correlation ID: {}", correlationId);
            return true;
        } else {
            log.warn("No pending request found for correlation ID: {}", correlationId);
            return false;
        }
    }

    /**
     * Cancels a pending request.
     *
     * @param correlationId The correlation ID
     * @return true if a pending request was found and cancelled, false otherwise
     */
    public boolean cancel(String correlationId) {
        PendingRequest<R> pendingRequest = pendingRequests.remove(correlationId);
        if (pendingRequest != null) {
            pendingRequest.future().cancel(true);
            log.info("Cancelled future for correlation ID: {}", correlationId);
            return true;
        }
        return false;
    }

    /**
     * Returns the number of pending requests.
     */
    public int getPendingCount() {
        return pendingRequests.size();
    }

    /**
     * Clears all pending requests.
     */
    public void clear() {
        pendingRequests.clear();
    }

    /**
     * Cleans up orphaned futures that are done (completed, cancelled, or timed out)
     * but still in the map. This is a safety mechanism to prevent memory leaks.
     */
    private void cleanupOrphanedFutures() {
        try {
            int beforeCount = pendingRequests.size();
            int removedCount = 0;

            // Remove completed, cancelled, or exceptionally completed futures
            pendingRequests.entrySet().removeIf(entry -> {
                CompletableFuture<R> future = entry.getValue().future();
                boolean isDone = future.isDone() || future.isCancelled() || future.isCompletedExceptionally();
                if (isDone) {
                    log.debug("Cleaning up orphaned future for correlation ID: {}", entry.getKey());
                    return true;
                }
                return false;
            });

            removedCount = beforeCount - pendingRequests.size();

            if (removedCount > 0) {
                log.info("Cleanup task removed {} orphaned futures. Pending requests: {} -> {}",
                        removedCount, beforeCount, pendingRequests.size());
            } else {
                log.debug("Cleanup task found no orphaned futures. Current pending: {}", beforeCount);
            }
        } catch (Exception e) {
            log.error("Error during cleanup task", e);
        }
    }

    /**
     * Shuts down the cleanup scheduler. Call this when the application is shutting down.
     * This method will wait up to 10 seconds for the scheduler to terminate gracefully.
     */
    public void shutdown() {
        log.info("Shutting down CorrelationManager cleanup scheduler");
        cleanupScheduler.shutdown();
        try {
            if (!cleanupScheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                cleanupScheduler.shutdownNow();
                log.warn("Cleanup scheduler did not terminate gracefully, forced shutdown");
            }
        } catch (InterruptedException e) {
            cleanupScheduler.shutdownNow();
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for cleanup scheduler to terminate", e);
        }
        log.info("CorrelationManager shutdown complete");
    }
}
