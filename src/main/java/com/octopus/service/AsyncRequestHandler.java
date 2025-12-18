package com.octopus.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Handles async request/response patterns using correlation IDs.
 * This class delegates correlation management to CorrelationManager and
 * allows custom request sending and response receiving logic.
 *
 * @param <T> The request type
 * @param <R> The response type
 */
@Slf4j
public class AsyncRequestHandler<T extends CorrelationIdProvider, R extends CorrelationIdProvider> {
    /**
     * -- GETTER --
     *  Returns the underlying correlation manager.
     */
    @Getter
    private final CorrelationManager<R> correlationManager;
    private final Consumer<T> requestSender;

    /**
     * Creates an AsyncRequestHandler with a correlation manager and request sender.
     *
     * @param correlationManager Manages correlation between requests and responses
     * @param requestSender Function to send the request (e.g., to a queue, external service)
     */
    public AsyncRequestHandler(CorrelationManager<R> correlationManager, Consumer<T> requestSender) {
        this.correlationManager = correlationManager;
        this.requestSender = requestSender;
    }

    /**
     * Sends a request and returns a CompletableFuture that will be completed
     * when a response with the same correlation ID is received.
     *
     * @param request The request to send
     * @return A CompletableFuture that will be completed with the response
     */
    public CompletableFuture<R> sendRequest(T request) {
        log.info("Sending request with correlation ID: {}", request.getCorrelationId());

        // Register the request with the correlation manager
        CompletableFuture<R> future = correlationManager.register(request.getCorrelationId());

        // Send the request using the provided sender
        if (requestSender != null) {
            requestSender.accept(request);
        }

        return future;
    }

    /**
     * Receives a response and completes the corresponding CompletableFuture.
     *
     * @param response The response received
     */
    public void receiveResponse(R response) {
        log.info("Received response for correlation ID: {}", response.getCorrelationId());
        correlationManager.complete(response.getCorrelationId(), response);
    }

    /**
     * Completes a pending request exceptionally with an error.
     * Use this when you need to explicitly fail a request.
     *
     * @param correlationId The correlation ID
     * @param throwable The exception to complete the future with
     * @return true if a pending request was found and completed exceptionally, false otherwise
     */
    public boolean completeExceptionally(String correlationId, Throwable throwable) {
        log.warn("Completing request exceptionally for correlation ID: {}", correlationId);
        return correlationManager.completeExceptionally(correlationId, throwable);
    }

    /**
     * Returns the number of pending requests.
     */
    public int getPendingRequestCount() {
        return correlationManager.getPendingCount();
    }
}
