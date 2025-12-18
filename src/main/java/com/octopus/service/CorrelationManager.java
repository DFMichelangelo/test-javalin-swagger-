package com.octopus.service;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Manages the correlation between requests and responses using correlation IDs.
 * Provides timeout handling and explicit error completion.
 *
 * @param <R> The response type
 */
@Slf4j
public class CorrelationManager<R> {
    private final ConcurrentHashMap<String, CompletableFuture<R>> pendingRequests;
    private final long timeoutSeconds;
    private final Function<String, R> timeoutResponseFactory;

    /**
     * Creates a CorrelationManager.
     *
     * @param timeoutSeconds Timeout for each request in seconds
     * @param timeoutResponseFactory Factory to create timeout responses (can be null)
     */
    public CorrelationManager(long timeoutSeconds, Function<String, R> timeoutResponseFactory) {
        this.pendingRequests = new ConcurrentHashMap<>();
        this.timeoutSeconds = timeoutSeconds;
        this.timeoutResponseFactory = timeoutResponseFactory;
        log.info("CorrelationManager initialized with {}s timeout", timeoutSeconds);
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

        // Set up timeout handling
        CompletableFuture<R> timeoutHandledFuture = baseFuture
                .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .whenComplete((result, ex) -> {
                    if (ex instanceof TimeoutException) {
                        log.warn("Request timed out for correlation ID: {}", correlationId);
                        pendingRequests.remove(correlationId);
                    }
                })
                .exceptionally(ex -> {
                    // Convert timeout to response, rethrow other exceptions
                    if (ex instanceof TimeoutException || ex.getCause() instanceof TimeoutException) {
                        return timeoutResponseFactory != null
                                ? timeoutResponseFactory.apply(correlationId)
                                : null;
                    }
                    throw (ex instanceof CompletionException)
                        ? (CompletionException) ex
                        : new CompletionException(ex);
                });

        pendingRequests.put(correlationId, baseFuture);
        return timeoutHandledFuture;
    }

    /**
     * Completes a pending request with the given response.
     *
     * @param correlationId The correlation ID
     * @param response The response to complete the future with
     * @return true if a pending request was found and completed, false otherwise
     */
    public boolean complete(String correlationId, R response) {
        log.info("Completing request for correlation ID: {}", correlationId);

        CompletableFuture<R> future = pendingRequests.remove(correlationId);

        if (future != null) {
            future.complete(response);
            log.info("Completed future for correlation ID: {}", correlationId);
            return true;
        } else {
            log.warn("No pending request found for correlation ID: {}", correlationId);
            return false;
        }
    }

    /**
     * Completes a pending request exceptionally with the given error.
     *
     * @param correlationId The correlation ID
     * @param throwable The exception to complete the future with
     * @return true if a pending request was found and completed, false otherwise
     */
    public boolean completeExceptionally(String correlationId, Throwable throwable) {
        log.warn("Completing request exceptionally for correlation ID: {}", correlationId, throwable);

        CompletableFuture<R> future = pendingRequests.remove(correlationId);

        if (future != null) {
            future.completeExceptionally(throwable);
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
        CompletableFuture<R> future = pendingRequests.remove(correlationId);
        if (future != null) {
            future.cancel(true);
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
}
