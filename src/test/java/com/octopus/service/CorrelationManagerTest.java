package com.octopus.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for CorrelationManager.
 * Tests cover basic functionality, timeout handling, error handling,
 * cleanup mechanisms, thread safety, and edge cases.
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CorrelationManagerTest {

    private CorrelationManager<TestResponse> correlationManager;
    private static final long SHORT_TIMEOUT_SECONDS = 2;

    // Test response class
    record TestResponse(String correlationId, String data, boolean success) {}

    @BeforeEach
    void setUp() {
        Function<String, TestResponse> timeoutFactory =
            correlationId -> new TestResponse(correlationId, "Timeout", false);

        correlationManager = new CorrelationManager<>(SHORT_TIMEOUT_SECONDS, timeoutFactory);
    }

    // ==================== Basic Functionality Tests ====================

    @Test
    @Order(1)
    @DisplayName("Should register a pending request and return a CompletableFuture")
    void testRegisterRequest() {
        // Given
        String correlationId = "test-001";

        // When
        CompletableFuture<TestResponse> future = correlationManager.register(correlationId);

        // Then
        assertThat(future).isNotNull();
        assertThat(future.isDone()).isFalse();
        assertThat(correlationManager.getPendingCount()).isEqualTo(1);
    }

    @Test
    @Order(2)
    @DisplayName("Should complete a pending request successfully")
    void testCompleteRequest() throws ExecutionException, InterruptedException {
        // Given
        String correlationId = "test-002";
        CompletableFuture<TestResponse> future = correlationManager.register(correlationId);
        TestResponse response = new TestResponse(correlationId, "Success", true);

        // When
        boolean completed = correlationManager.complete(correlationId, response);

        // Then
        assertThat(completed).isTrue();
        assertThat(future.isDone()).isTrue();
        assertThat(future.get()).isEqualTo(response);
        assertThat(correlationManager.getPendingCount()).isEqualTo(0);
    }

    @Test
    @Order(3)
    @DisplayName("Should return false when completing non-existent correlation ID")
    void testCompleteNonExistentRequest() {
        // Given
        String correlationId = "non-existent";
        TestResponse response = new TestResponse(correlationId, "Data", true);

        // When
        boolean completed = correlationManager.complete(correlationId, response);

        // Then
        assertThat(completed).isFalse();
        assertThat(correlationManager.getPendingCount()).isEqualTo(0);
    }

    @Test
    @Order(4)
    @DisplayName("Should cancel a pending request")
    void testCancelRequest() {
        // Given
        String correlationId = "test-004";
        CompletableFuture<TestResponse> future = correlationManager.register(correlationId);

        // When
        boolean cancelled = correlationManager.cancel(correlationId);

        // Then
        assertThat(cancelled).isTrue();
        // Note: The returned future is a chain, so it completes exceptionally when base is cancelled
        assertThat(future.isDone()).isTrue();
        assertThat(future.isCompletedExceptionally()).isTrue();
        assertThat(correlationManager.getPendingCount()).isEqualTo(0);
    }

    @Test
    @Order(5)
    @DisplayName("Should clear all pending requests")
    void testClearAllRequests() {
        // Given
        for (int i = 0; i < 5; i++) {
            correlationManager.register("test-" + i);
        }
        assertThat(correlationManager.getPendingCount()).isEqualTo(5);

        // When
        correlationManager.clear();

        // Then
        assertThat(correlationManager.getPendingCount()).isEqualTo(0);
    }

    // ==================== Timeout Handling Tests ====================

    @Test
    @Order(10)
    @DisplayName("Should handle timeout and call timeout response factory")
    void testRequestTimeout() {
        // Given
        String correlationId = "timeout-001";
        CompletableFuture<TestResponse> future = correlationManager.register(correlationId);

        // When - wait for timeout
        await()
            .atMost(Duration.ofSeconds(SHORT_TIMEOUT_SECONDS + 2))
            .pollInterval(Duration.ofMillis(100))
            .untilAsserted(() -> {
                assertThat(future.isDone()).isTrue();
            });

        // Then
        assertThat(future.isDone()).isTrue();
        assertThat(future.isCompletedExceptionally()).isFalse();

        // Verify timeout response was created
        TestResponse response = future.join();
        assertThat(response.correlationId()).isEqualTo(correlationId);
        assertThat(response.data()).isEqualTo("Timeout");
        assertThat(response.success()).isFalse();

        // Verify cleanup happened
        assertThat(correlationManager.getPendingCount()).isEqualTo(0);
    }

    @Test
    @Order(11)
    @DisplayName("Should handle timeout without response factory (null)")
    void testTimeoutWithNullFactory() {
        // Given
        CorrelationManager<TestResponse> managerWithoutFactory = new CorrelationManager<>(1, null);
        String correlationId = "timeout-002";
        CompletableFuture<TestResponse> future = managerWithoutFactory.register(correlationId);

        // When - wait for timeout
        await()
            .atMost(Duration.ofSeconds(3))
            .pollInterval(Duration.ofMillis(100))
            .untilAsserted(() -> assertThat(future.isDone()).isTrue());

        // Then - should complete with null
        assertThat(future.join()).isNull();
        assertThat(managerWithoutFactory.getPendingCount()).isEqualTo(0);
    }

    @Test
    @Order(12)
    @DisplayName("Should complete normally before timeout occurs")
    void testCompleteBeforeTimeout() throws ExecutionException, InterruptedException {
        // Given
        String correlationId = "no-timeout-001";
        CompletableFuture<TestResponse> future = correlationManager.register(correlationId);
        TestResponse response = new TestResponse(correlationId, "Fast response", true);

        // When - complete immediately
        boolean completed = correlationManager.complete(correlationId, response);

        // Then
        assertThat(completed).isTrue();
        assertThat(future.isDone()).isTrue();
        assertThat(future.get()).isEqualTo(response);
        assertThat(correlationManager.getPendingCount()).isEqualTo(0);
    }

    // ==================== Error Handling Tests ====================

    @Test
    @Order(20)
    @DisplayName("Should complete request exceptionally")
    void testCompleteExceptionally() {
        // Given
        String correlationId = "error-001";
        CompletableFuture<TestResponse> future = correlationManager.register(correlationId);
        RuntimeException error = new RuntimeException("Test error");

        // When
        boolean completed = correlationManager.completeExceptionally(correlationId, error);

        // Then
        assertThat(completed).isTrue();
        assertThat(future.isCompletedExceptionally()).isTrue();

        assertThatThrownBy(future::join)
            .isInstanceOf(CompletionException.class)
            .hasCauseInstanceOf(RuntimeException.class)
            .hasMessageContaining("Test error");

        assertThat(correlationManager.getPendingCount()).isEqualTo(0);
    }

    @Test
    @Order(21)
    @DisplayName("Should return false when completing non-existent request exceptionally")
    void testCompleteExceptionallyNonExistent() {
        // Given
        String correlationId = "non-existent";
        RuntimeException error = new RuntimeException("Test error");

        // When
        boolean completed = correlationManager.completeExceptionally(correlationId, error);

        // Then
        assertThat(completed).isFalse();
    }

    @Test
    @Order(22)
    @DisplayName("Should handle different exception types")
    void testCompleteExceptionallyWithDifferentExceptions() {
        // Test with various exception types
        // Note: TimeoutException is excluded because it's handled specially by the timeout logic
        List<Throwable> exceptions = List.of(
            new IllegalArgumentException("Invalid argument"),
            new IllegalStateException("Invalid state"),
            new RuntimeException("Runtime error"),
            new InterruptedException("Interrupted")
        );

        for (int i = 0; i < exceptions.size(); i++) {
            String correlationId = "error-type-" + i;
            CompletableFuture<TestResponse> future = correlationManager.register(correlationId);
            Throwable exception = exceptions.get(i);

            correlationManager.completeExceptionally(correlationId, exception);

            assertThat(future.isCompletedExceptionally()).isTrue();
            assertThatThrownBy(future::join)
                .isInstanceOf(CompletionException.class)
                .hasCause(exception);
        }
    }

    // ==================== Cleanup Tests ====================

    @Test
    @Order(30)
    @DisplayName("Should clean up completed futures during periodic cleanup")
    void testPeriodicCleanup() {
        // Given
        // Note: Futures are actually cleaned up immediately on completion via the complete() method
        // This test verifies that completed futures don't accumulate in the map

        // Create and complete several requests
        int requestCount = 5;
        for (int i = 0; i < requestCount; i++) {
            String correlationId = "cleanup-" + i;
            CompletableFuture<TestResponse> future = correlationManager.register(correlationId);
            correlationManager.complete(correlationId, new TestResponse(correlationId, "Done", true));
        }

        // Then - futures should be cleaned up immediately on completion
        assertThat(correlationManager.getPendingCount()).isEqualTo(0);
    }

    @Test
    @Order(31)
    @DisplayName("Should clean up cancelled futures")
    void testCleanupCancelledFutures() {
        // Given
        List<String> correlationIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String correlationId = "cancel-cleanup-" + i;
            correlationIds.add(correlationId);
            correlationManager.register(correlationId);
        }

        // When - cancel all
        for (String id : correlationIds) {
            correlationManager.cancel(id);
        }

        // Then - should be cleaned up immediately by cancel method
        assertThat(correlationManager.getPendingCount()).isEqualTo(0);
    }

    // ==================== Thread Safety Tests ====================

    @Test
    @Order(40)
    @DisplayName("Should handle concurrent registrations safely")
    void testConcurrentRegistrations() throws InterruptedException {
        // Given
        int threadCount = 10;
        int requestsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // When - register from multiple threads
        for (int t = 0; t < threadCount; t++) {
            int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < requestsPerThread; i++) {
                        String correlationId = "thread-" + threadId + "-req-" + i;
                        correlationManager.register(correlationId);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Then
        assertThat(correlationManager.getPendingCount()).isEqualTo(threadCount * requestsPerThread);
    }

    @Test
    @Order(41)
    @DisplayName("Should handle concurrent completions safely")
    void testConcurrentCompletions() throws InterruptedException, ExecutionException {
        // Given
        int requestCount = 100;
        List<CompletableFuture<TestResponse>> futures = new ArrayList<>();
        List<String> correlationIds = new ArrayList<>();

        for (int i = 0; i < requestCount; i++) {
            String correlationId = "concurrent-" + i;
            correlationIds.add(correlationId);
            futures.add(correlationManager.register(correlationId));
        }

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(requestCount);

        // When - complete from multiple threads
        for (int i = 0; i < requestCount; i++) {
            int index = i;
            executor.submit(() -> {
                try {
                    String correlationId = correlationIds.get(index);
                    TestResponse response = new TestResponse(correlationId, "Data-" + index, true);
                    correlationManager.complete(correlationId, response);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Then - all futures should be completed
        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                assertThat(futures).allMatch(CompletableFuture::isDone);
                assertThat(correlationManager.getPendingCount()).isEqualTo(0);
            });

        for (int i = 0; i < requestCount; i++) {
            TestResponse response = futures.get(i).get();
            assertThat(response.correlationId()).isEqualTo(correlationIds.get(i));
        }
    }

    @Test
    @Order(42)
    @DisplayName("Should handle concurrent register and complete operations")
    void testConcurrentRegisterAndComplete() throws InterruptedException {
        // Given
        int operationCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch registerLatch = new CountDownLatch(operationCount);
        CountDownLatch completeLatch = new CountDownLatch(operationCount);
        List<CompletableFuture<TestResponse>> futures = new CopyOnWriteArrayList<>();

        // When - register and complete concurrently
        for (int i = 0; i < operationCount; i++) {
            int index = i;

            // Register
            executor.submit(() -> {
                try {
                    String correlationId = "race-" + index;
                    CompletableFuture<TestResponse> future = correlationManager.register(correlationId);
                    futures.add(future);
                } finally {
                    registerLatch.countDown();
                }
            });

            // Complete (might happen before or after registration)
            executor.submit(() -> {
                try {
                    // Small delay to increase chance of race
                    Thread.sleep(10);
                    String correlationId = "race-" + index;
                    TestResponse response = new TestResponse(correlationId, "Race-" + index, true);
                    correlationManager.complete(correlationId, response);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        registerLatch.await(10, TimeUnit.SECONDS);
        completeLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Then - verify all operations completed
        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                assertThat(correlationManager.getPendingCount()).isEqualTo(0);
            });
    }

    // ==================== Edge Cases ====================

    @Test
    @Order(50)
    @DisplayName("Should handle duplicate correlation IDs (overwrite)")
    void testDuplicateCorrelationIds() throws ExecutionException, InterruptedException {
        // Given
        String correlationId = "duplicate-001";
        CompletableFuture<TestResponse> future1 = correlationManager.register(correlationId);
        CompletableFuture<TestResponse> future2 = correlationManager.register(correlationId);

        // When - complete the correlation ID
        TestResponse response = new TestResponse(correlationId, "Final", true);
        correlationManager.complete(correlationId, response);

        // Then - the second registration should have overwritten the first
        assertThat(future1.isDone()).isFalse(); // First future was overwritten
        assertThat(future2.isDone()).isTrue();  // Second future was completed
        assertThat(future2.get()).isEqualTo(response);
    }

    @Test
    @Order(51)
    @DisplayName("Should handle empty correlation ID")
    void testEmptyCorrelationId() {
        // Given
        String emptyId = "";
        CompletableFuture<TestResponse> future = correlationManager.register(emptyId);

        // When
        TestResponse response = new TestResponse(emptyId, "Empty ID", true);
        boolean completed = correlationManager.complete(emptyId, response);

        // Then
        assertThat(completed).isTrue();
        assertThat(future.isDone()).isTrue();
    }

    @Test
    @Order(52)
    @DisplayName("Should handle null response in complete")
    void testNullResponse() throws ExecutionException, InterruptedException {
        // Given
        String correlationId = "null-response";
        CompletableFuture<TestResponse> future = correlationManager.register(correlationId);

        // When
        boolean completed = correlationManager.complete(correlationId, null);

        // Then
        assertThat(completed).isTrue();
        assertThat(future.isDone()).isTrue();
        assertThat(future.get()).isNull();
    }

    @Test
    @Order(53)
    @DisplayName("Should handle multiple completions of same correlation ID")
    void testMultipleCompletions() throws ExecutionException, InterruptedException {
        // Given
        String correlationId = "multi-complete";
        CompletableFuture<TestResponse> future = correlationManager.register(correlationId);
        TestResponse response1 = new TestResponse(correlationId, "First", true);
        TestResponse response2 = new TestResponse(correlationId, "Second", true);

        // When
        boolean completed1 = correlationManager.complete(correlationId, response1);
        boolean completed2 = correlationManager.complete(correlationId, response2);

        // Then
        assertThat(completed1).isTrue();
        assertThat(completed2).isFalse(); // Already removed
        assertThat(future.get()).isEqualTo(response1); // First completion wins
    }

    // ==================== Integration Tests ====================

    @Test
    @Order(70)
    @DisplayName("Should handle realistic workflow: register, complete, timeout mix")
    void testRealisticWorkflow() throws InterruptedException, ExecutionException {
        // Given
        List<CompletableFuture<TestResponse>> futures = new ArrayList<>();

        // Register 10 requests
        for (int i = 0; i < 10; i++) {
            futures.add(correlationManager.register("workflow-" + i));
        }

        // Complete some immediately
        correlationManager.complete("workflow-0", new TestResponse("workflow-0", "Fast", true));
        correlationManager.complete("workflow-1", new TestResponse("workflow-1", "Fast", true));

        // Cancel some
        correlationManager.cancel("workflow-2");

        // Complete exceptionally
        correlationManager.completeExceptionally("workflow-3", new RuntimeException("Error"));

        // Let some timeout (workflow-4, workflow-5, etc.)

        // Complete a few more before timeout
        Thread.sleep(1000);
        correlationManager.complete("workflow-6", new TestResponse("workflow-6", "Medium", true));

        // Wait for all to finish (timeout or completion)
        await()
            .atMost(Duration.ofSeconds(SHORT_TIMEOUT_SECONDS + 2))
            .untilAsserted(() -> {
                assertThat(futures).allMatch(CompletableFuture::isDone);
            });

        // Verify states
        assertThat(futures.get(0).get().success()).isTrue(); // Completed
        assertThat(futures.get(1).get().success()).isTrue(); // Completed
        assertThat(futures.get(2).isCompletedExceptionally()).isTrue(); // Cancelled (appears as exceptional)
        assertThat(futures.get(3).isCompletedExceptionally()).isTrue(); // Error
        assertThat(futures.get(6).get().success()).isTrue(); // Completed

        // Others should have timed out
        assertThat(futures.get(4).get().success()).isFalse(); // Timeout
        assertThat(futures.get(5).get().success()).isFalse(); // Timeout
    }
}
