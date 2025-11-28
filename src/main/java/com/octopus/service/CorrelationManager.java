package com.octopus.service;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Manages the correlation between requests and responses using correlation IDs.
 * This class is responsible only for managing CompletableFutures, not for
 * sending requests or receiving responses.
 *
 * @param <R> The response type
 */
@Slf4j
public class CorrelationManager<R> {
    private final ConcurrentHashMap<String, CompletableFuture<R>> pendingRequests;
    private final long timeoutSeconds;
    private final Function<String, R> timeoutResponseFactory;

    public CorrelationManager(long timeoutSeconds, Function<String, R> timeoutResponseFactory) {
        this.pendingRequests = new ConcurrentHashMap<>();
        this.timeoutSeconds = timeoutSeconds;
        this.timeoutResponseFactory = timeoutResponseFactory;
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

        CompletableFuture<R> future = new CompletableFuture<>();
        pendingRequests.put(correlationId, future);

        // Set up timeout
        future.orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    log.error("Request timed out for correlation ID: {}", correlationId);
                    pendingRequests.remove(correlationId);
                    return timeoutResponseFactory != null
                            ? timeoutResponseFactory.apply(correlationId)
                            : null;
                });

        return future;
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
