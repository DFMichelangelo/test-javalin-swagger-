package com.octopus.service;

import com.octopus.dto.Request;
import com.octopus.dto.Response;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Comprehensive unit tests for AsyncRequestHandler.
 * Tests cover request/response handling, error scenarios,
 * integration with CorrelationManager, and thread safety.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AsyncRequestHandlerTest {

    private Consumer<Request> requestSender;
    private List<Request> sentRequests;

    private CorrelationManager<Response> correlationManager;
    private AsyncRequestHandler<Request, Response> requestHandler;

    private static final long TIMEOUT_SECONDS = 3;

    @BeforeEach
    void setUp() {
        // Use a real Consumer instead of mocking due to Mockito compatibility issues
        sentRequests = new CopyOnWriteArrayList<>();
        requestSender = request -> sentRequests.add(request);

        correlationManager = new CorrelationManager<>(
            TIMEOUT_SECONDS,
            correlationId -> new Response(correlationId, "Request timed out", false),
            1 // cleanup interval in minutes
        );

        requestHandler = new AsyncRequestHandler<>(correlationManager, requestSender);
    }

    @AfterEach
    void tearDown() {
        if (requestHandler != null) {
            requestHandler.shutdown();
        }
    }

    // ==================== Basic Functionality Tests ====================

    @Test
    @Order(1)
    @DisplayName("Should send request and complete successfully when response arrives")
    void testSuccessfulRequestResponse() throws ExecutionException, InterruptedException {
        // Given
        Request request = new Request("test-payload");
        String correlationId = request.getCorrelationId();

        // When
        CompletableFuture<Response> future = requestHandler.sendRequest(request);

        // Simulate response arrival
        Response response = new Response(correlationId, "Success", true);
        requestHandler.receiveResponse(response);

        // Then
        assertThat(sentRequests).hasSize(1);
        assertThat(sentRequests.get(0)).isEqualTo(request);
        assertThat(future.isDone()).isTrue();
        assertThat(future.get()).isEqualTo(response);
        assertThat(future.get().isSuccess()).isTrue();
        assertThat(requestHandler.getPendingRequestCount()).isEqualTo(0);
    }

    @Test
    @Order(2)
    @DisplayName("Should invoke request sender with correct request")
    void testRequestSenderInvocation() {
        // Given
        Request request = new Request("test-data");

        // When
        requestHandler.sendRequest(request);

        // Then
        assertThat(sentRequests).hasSize(1);
        Request capturedRequest = sentRequests.get(0);
        assertThat(capturedRequest).isEqualTo(request);
        assertThat(capturedRequest.getPayload()).isEqualTo("test-data");
    }

    @Test
    @Order(3)
    @DisplayName("Should handle multiple concurrent requests")
    void testMultipleConcurrentRequests() throws ExecutionException, InterruptedException {
        // Given
        int requestCount = 10;
        List<Request> requests = new ArrayList<>();
        List<CompletableFuture<Response>> futures = new ArrayList<>();

        // When - send multiple requests
        for (int i = 0; i < requestCount; i++) {
            Request request = new Request("payload-" + i);
            requests.add(request);
            futures.add(requestHandler.sendRequest(request));
        }

        // Simulate responses for all requests
        for (Request request : requests) {
            Response response = new Response(
                request.getCorrelationId(),
                "Response for " + request.getPayload(),
                true
            );
            requestHandler.receiveResponse(response);
        }

        // Then
        assertThat(sentRequests).hasSize(requestCount);

        for (int i = 0; i < requestCount; i++) {
            assertThat(futures.get(i).isDone()).isTrue();
            Response response = futures.get(i).get();
            assertThat(response.getCorrelationId()).isEqualTo(requests.get(i).getCorrelationId());
            assertThat(response.isSuccess()).isTrue();
        }

        assertThat(requestHandler.getPendingRequestCount()).isEqualTo(0);
    }

    @Test
    @Order(4)
    @DisplayName("Should return pending request count correctly")
    void testGetPendingRequestCount() {
        // Given
        assertThat(requestHandler.getPendingRequestCount()).isEqualTo(0);

        // When - send requests without responses
        for (int i = 0; i < 5; i++) {
            requestHandler.sendRequest(new Request("payload-" + i));
        }

        // Then
        assertThat(requestHandler.getPendingRequestCount()).isEqualTo(5);

        // Complete one request
        Request request = new Request("complete-me");
        String correlationId = request.getCorrelationId();
        requestHandler.sendRequest(request);
        assertThat(requestHandler.getPendingRequestCount()).isEqualTo(6);

        requestHandler.receiveResponse(new Response(correlationId, "Done", true));
        assertThat(requestHandler.getPendingRequestCount()).isEqualTo(5);
    }

    @Test
    @Order(5)
    @DisplayName("Should provide access to underlying correlation manager")
    void testGetCorrelationManager() {
        // When
        CorrelationManager<Response> manager = requestHandler.getCorrelationManager();

        // Then
        assertThat(manager).isNotNull();
        assertThat(manager).isSameAs(correlationManager);
    }

    // ==================== Timeout Tests ====================

    @Test
    @Order(10)
    @DisplayName("Should timeout when no response arrives")
    void testRequestTimeout() {
        // Given
        Request request = new Request("timeout-test");
        String correlationId = request.getCorrelationId();

        // When
        CompletableFuture<Response> future = requestHandler.sendRequest(request);

        // Wait for timeout
        await()
            .atMost(Duration.ofSeconds(TIMEOUT_SECONDS + 2))
            .pollInterval(Duration.ofMillis(100))
            .untilAsserted(() -> assertThat(future.isDone()).isTrue());

        // Then
        Response response = future.join();
        assertThat(response.getCorrelationId()).isEqualTo(correlationId);
        assertThat(response.getResult()).isEqualTo("Request timed out");
        assertThat(response.isSuccess()).isFalse();
        assertThat(requestHandler.getPendingRequestCount()).isEqualTo(0);
    }

    @Test
    @Order(11)
    @DisplayName("Should complete normally before timeout occurs")
    void testCompletionBeforeTimeout() throws ExecutionException, InterruptedException {
        // Given
        Request request = new Request("fast-response");
        String correlationId = request.getCorrelationId();

        // When
        CompletableFuture<Response> future = requestHandler.sendRequest(request);

        // Complete immediately
        Response response = new Response(correlationId, "Fast", true);
        requestHandler.receiveResponse(response);

        // Then
        assertThat(future.isDone()).isTrue();
        assertThat(future.get()).isEqualTo(response);
        assertThat(future.get().isSuccess()).isTrue();
    }

    // ==================== Error Handling Tests ====================

    @Test
    @Order(20)
    @DisplayName("Should complete request exceptionally")
    void testCompleteExceptionally() {
        // Given
        Request request = new Request("error-test");
        String correlationId = request.getCorrelationId();
        CompletableFuture<Response> future = requestHandler.sendRequest(request);

        // When
        RuntimeException error = new RuntimeException("Processing failed");
        boolean completed = requestHandler.completeExceptionally(correlationId, error);

        // Then
        assertThat(completed).isTrue();
        assertThat(future.isCompletedExceptionally()).isTrue();

        assertThatThrownBy(future::join)
            .isInstanceOf(CompletionException.class)
            .hasCauseInstanceOf(RuntimeException.class)
            .hasMessageContaining("Processing failed");

        assertThat(requestHandler.getPendingRequestCount()).isEqualTo(0);
    }

    @Test
    @Order(21)
    @DisplayName("Should return false when completing non-existent request exceptionally")
    void testCompleteExceptionallyNonExistent() {
        // Given
        String nonExistentId = "does-not-exist";
        RuntimeException error = new RuntimeException("Error");

        // When
        boolean completed = requestHandler.completeExceptionally(nonExistentId, error);

        // Then
        assertThat(completed).isFalse();
    }

    @Test
    @Order(22)
    @DisplayName("Should handle request sender throwing exception")
    void testRequestSenderException() {
        // Given - create a handler with a failing sender
        Consumer<Request> failingSender = request -> {
            throw new RuntimeException("Sender failed");
        };
        AsyncRequestHandler<Request, Response> failingHandler =
            new AsyncRequestHandler<>(correlationManager, failingSender);

        Request request = new Request("fail-to-send");

        try {
            // When/Then - should throw when sending
            assertThatThrownBy(() -> failingHandler.sendRequest(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Sender failed");
        } finally {
            failingHandler.shutdown();
        }
    }

    // ==================== Response Handling Tests ====================

    @Test
    @Order(30)
    @DisplayName("Should ignore response for non-existent correlation ID")
    void testReceiveResponseForNonExistentCorrelation() {
        // Given
        Response orphanResponse = new Response("non-existent-id", "Orphan", true);

        // When
        requestHandler.receiveResponse(orphanResponse);

        // Then - should not throw, just log warning
        assertThat(requestHandler.getPendingRequestCount()).isEqualTo(0);
    }

    @Test
    @Order(31)
    @DisplayName("Should handle null request sender gracefully")
    void testNullRequestSender() {
        // Given
        AsyncRequestHandler<Request, Response> handlerWithNullSender =
            new AsyncRequestHandler<>(correlationManager, null);

        Request request = new Request("no-sender");
        String correlationId = request.getCorrelationId();

        try {
            // When
            CompletableFuture<Response> future = handlerWithNullSender.sendRequest(request);

            // Simulate response
            Response response = new Response(correlationId, "Success", true);
            handlerWithNullSender.receiveResponse(response);

            // Then - should still work, just won't actually send
            assertThat(future.isDone()).isTrue();
            assertThat(future.join()).isEqualTo(response);
        } finally {
            handlerWithNullSender.shutdown();
        }
    }

    @Test
    @Order(32)
    @DisplayName("Should handle response with null fields")
    void testResponseWithNullFields() throws ExecutionException, InterruptedException {
        // Given
        Request request = new Request("null-response-test");
        String correlationId = request.getCorrelationId();
        CompletableFuture<Response> future = requestHandler.sendRequest(request);

        // When - receive response with null result
        Response response = new Response(correlationId, null, false);
        requestHandler.receiveResponse(response);

        // Then
        assertThat(future.isDone()).isTrue();
        Response received = future.get();
        assertThat(received.getCorrelationId()).isEqualTo(correlationId);
        assertThat(received.getResult()).isNull();
        assertThat(received.isSuccess()).isFalse();
    }

    // ==================== Thread Safety Tests ====================

    @Test
    @Order(40)
    @DisplayName("Should handle concurrent sends and receives safely")
    void testConcurrentSendsAndReceives() throws InterruptedException {
        // Given
        int operationCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch sendLatch = new CountDownLatch(operationCount);
        CountDownLatch receiveLatch = new CountDownLatch(operationCount);
        List<CompletableFuture<Response>> futures = new CopyOnWriteArrayList<>();
        List<String> correlationIds = new CopyOnWriteArrayList<>();

        // When - send requests from multiple threads
        for (int i = 0; i < operationCount; i++) {
            int index = i;
            executor.submit(() -> {
                try {
                    Request request = new Request("concurrent-" + index);
                    correlationIds.add(request.getCorrelationId());
                    CompletableFuture<Response> future = requestHandler.sendRequest(request);
                    futures.add(future);
                } finally {
                    sendLatch.countDown();
                }
            });
        }

        // Wait for all sends to complete
        sendLatch.await(10, TimeUnit.SECONDS);

        // Receive responses from multiple threads
        for (int i = 0; i < operationCount; i++) {
            int index = i;
            executor.submit(() -> {
                try {
                    Thread.sleep(10); // Small delay
                    String correlationId = correlationIds.get(index);
                    Response response = new Response(correlationId, "Result-" + index, true);
                    requestHandler.receiveResponse(response);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    receiveLatch.countDown();
                }
            });
        }

        receiveLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Then
        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                assertThat(futures).allMatch(CompletableFuture::isDone);
                assertThat(requestHandler.getPendingRequestCount()).isEqualTo(0);
            });
    }

    @Test
    @Order(41)
    @DisplayName("Should handle high load scenario")
    void testHighLoadScenario() throws InterruptedException {
        // Given
        int requestCount = 500;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(requestCount * 2);
        AtomicInteger successCount = new AtomicInteger(0);

        // When - simulate high load
        for (int i = 0; i < requestCount; i++) {
            int index = i;

            // Send request
            executor.submit(() -> {
                try {
                    Request request = new Request("load-" + index);
                    String correlationId = request.getCorrelationId();
                    CompletableFuture<Response> future = requestHandler.sendRequest(request);

                    future.thenAccept(response -> {
                        if (response.isSuccess()) {
                            successCount.incrementAndGet();
                        }
                    });

                    // Simulate immediate response for half
                    if (index % 2 == 0) {
                        executor.submit(() -> {
                            try {
                                Thread.sleep(5);
                                Response response = new Response(correlationId, "Quick-" + index, true);
                                requestHandler.receiveResponse(response);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        });
                    }
                } finally {
                    latch.countDown();
                }
            });

            latch.countDown(); // Count for the response task
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Then - wait for completions
        await()
            .atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                // At least the ones we responded to should be successful
                assertThat(successCount.get()).isGreaterThanOrEqualTo(requestCount / 2);
            });
    }

    // ==================== Integration Tests ====================

    @Test
    @Order(50)
    @DisplayName("Should handle realistic request-response flow")
    void testRealisticFlow() throws ExecutionException, InterruptedException {
        // Given - simulate a realistic scenario
        List<CompletableFuture<Response>> futures = new ArrayList<>();

        // Scenario 1: Normal request-response
        Request normalRequest = new Request("normal-operation");
        CompletableFuture<Response> normalFuture = requestHandler.sendRequest(normalRequest);
        futures.add(normalFuture);

        // Scenario 2: Delayed response
        Request delayedRequest = new Request("delayed-operation");
        CompletableFuture<Response> delayedFuture = requestHandler.sendRequest(delayedRequest);
        futures.add(delayedFuture);

        // Scenario 3: Error during processing
        Request errorRequest = new Request("error-operation");
        CompletableFuture<Response> errorFuture = requestHandler.sendRequest(errorRequest);
        futures.add(errorFuture);

        // Verify all requests were sent
        assertThat(sentRequests).hasSize(3);

        // When - simulate responses
        // Immediate response
        requestHandler.receiveResponse(
            new Response(normalRequest.getCorrelationId(), "Completed", true)
        );

        // Delayed response after 1 second
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1000);
                requestHandler.receiveResponse(
                    new Response(delayedRequest.getCorrelationId(), "Completed after delay", true)
                );
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Error response
        requestHandler.completeExceptionally(
            errorRequest.getCorrelationId(),
            new RuntimeException("Processing error")
        );

        // Then - verify outcomes
        // Normal request
        assertThat(normalFuture.isDone()).isTrue();
        assertThat(normalFuture.get().isSuccess()).isTrue();

        // Delayed request
        await()
            .atMost(Duration.ofSeconds(3))
            .untilAsserted(() -> assertThat(delayedFuture.isDone()).isTrue());
        assertThat(delayedFuture.get().isSuccess()).isTrue();

        // Error request
        assertThat(errorFuture.isCompletedExceptionally()).isTrue();
        assertThatThrownBy(errorFuture::join)
            .isInstanceOf(CompletionException.class)
            .hasMessageContaining("Processing error");
    }

    @Test
    @Order(51)
    @DisplayName("Should handle request-response-chain scenario")
    void testRequestResponseChaining() throws ExecutionException, InterruptedException {
        // Given - send first request
        Request request1 = new Request("step-1");
        CompletableFuture<Response> future1 = requestHandler.sendRequest(request1);

        // When - chain responses
        CompletableFuture<Response> chainedFuture = future1.thenCompose(response1 -> {
            // Based on first response, send second request
            Request request2 = new Request("step-2-based-on-" + response1.getResult());
            return requestHandler.sendRequest(request2);
        });

        // Simulate responses
        requestHandler.receiveResponse(
            new Response(request1.getCorrelationId(), "first-result", true)
        );

        // Get the correlation ID from the chained request
        await()
            .atMost(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                assertThat(sentRequests).hasSize(2);
            });

        // Complete the second request
        Request request2 = sentRequests.get(1);

        requestHandler.receiveResponse(
            new Response(request2.getCorrelationId(), "second-result", true)
        );

        // Then
        Response finalResponse = chainedFuture.get();
        assertThat(finalResponse.getResult()).isEqualTo("second-result");
        assertThat(finalResponse.isSuccess()).isTrue();
    }

    // ==================== Shutdown Tests ====================

    @Test
    @Order(60)
    @DisplayName("Should shutdown gracefully")
    void testShutdown() {
        // Given
        requestHandler.sendRequest(new Request("pending"));

        // When
        assertDoesNotThrow(() -> requestHandler.shutdown());

        // Then - should complete without hanging
    }

    @Test
    @Order(61)
    @DisplayName("Should shutdown with pending requests")
    void testShutdownWithPending() {
        // Given
        for (int i = 0; i < 10; i++) {
            requestHandler.sendRequest(new Request("pending-" + i));
        }

        // When/Then
        assertDoesNotThrow(() -> requestHandler.shutdown());
    }
}
