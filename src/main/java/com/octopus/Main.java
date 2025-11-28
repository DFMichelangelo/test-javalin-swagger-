package com.octopus;

import com.octopus.dto.Request;
import com.octopus.dto.Response;
import com.octopus.service.AsyncRequestHandler;
import com.octopus.service.CorrelationManager;
import com.octopus.webserver.AsyncRoute;
import io.javalin.Javalin;
import io.javalin.openapi.*;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerConfiguration;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class Main {
    // Create correlation manager for managing request/response futures
    private static final CorrelationManager<Response> correlationManager =
            new CorrelationManager<>(
                    30, // timeout in seconds
                    correlationId -> new Response(correlationId, "Request timed out", false) // timeout response factory
            );

    // Create request handler with correlation manager and custom request sender
    private static final AsyncRequestHandler<Request, Response> requestHandler =
            new AsyncRequestHandler<>(
                    correlationManager,
                    request -> {
                        // Custom logic to send the request (e.g., to a message queue, external service)
                        log.info("Processing request: {}", request);
                        // In a real application, you would send this to a queue, external API, etc.
                    }
            );

    void main() {
        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";

            // Configure OpenAPI plugin
            config.registerPlugin(new OpenApiPlugin(pluginConfig -> {
                pluginConfig.withDefinitionConfiguration((version, definition) -> {
                    definition.withOpenApiInfo(info -> {
                        info.setTitle("Correlation ID API");
                        info.setVersion("1.0.0");
                        info.setDescription("Async request/response correlation API");
                    });
                });
            }));

            // Configure Swagger UI plugin
            config.registerPlugin(new SwaggerPlugin(swaggerConfig -> {
                swaggerConfig.setUiPath("/swagger-ui");
                swaggerConfig.setDocumentationPath("/openapi");
            }));
        }).start(7070);

        log.info("Server started on port 7070");

        // Define async routes as objects
        var requestRoute = AsyncRoute.post("/api/request", Main::handleAsyncRequest);
        var responseRoute = AsyncRoute.post("/api/response", Main::handleResponseSubmission);
        var statusRoute = AsyncRoute.get("/api/status", Main::handleStatusRequest);
        var testRoute = AsyncRoute.post("/api/test/delayed-response", Main::handleDelayedTestRequest);

        // Register each route
        requestRoute.registerWith(app);
        responseRoute.registerWith(app);
        statusRoute.registerWith(app);
        testRoute.registerWith(app);

        log.info("API endpoints available:");
        log.info("  POST /api/request - Send async request");
        log.info("  POST /api/response - Submit response to complete pending request");
        log.info("  GET  /api/status - Check server status");
        log.info("  POST /api/test/delayed-response - Test endpoint with simulated delay");
        log.info("");
        log.info("Swagger UI available at: http://localhost:7070/swagger-ui");
    }

    /**
     * Handles response submission to complete pending requests.
     */
    @OpenApi(
        path = "/api/response",
        methods = HttpMethod.POST,
        summary = "Submit response to complete pending request",
        description = "Receives a response that matches a pending request by correlation ID",
        tags = {"Response"},
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = Response.class)),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = ApiResponse.class), description = "Response received and processed")
        }
    )
    private static CompletableFuture<ApiResponse> handleResponseSubmission(io.javalin.http.Context ctx) {
        Response response = ctx.bodyAsClass(Response.class);
        requestHandler.receiveResponse(response);
        return CompletableFuture.completedFuture(
                new ApiResponse(true, "Response received and processed")
        );
    }

    /**
     * Handles server status requests.
     */
    @OpenApi(
        path = "/api/status",
        methods = HttpMethod.GET,
        summary = "Check server status",
        description = "Returns the current server status and number of pending requests",
        tags = {"Status"},
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = StatusResponse.class), description = "Server status")
        }
    )
    private static CompletableFuture<StatusResponse> handleStatusRequest(io.javalin.http.Context ctx) {
        return CompletableFuture.completedFuture(new StatusResponse(
                "Server is running",
                requestHandler.getPendingRequestCount()
        ));
    }

    /**
     * Handles async requests by sending them through the request handler.
     */
    @OpenApi(
        path = "/api/request",
        methods = HttpMethod.POST,
        summary = "Send async request",
        description = "Submits an async request and returns when a response is received or timeout occurs",
        tags = {"Request"},
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = Request.class)),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Response.class), description = "Request processed successfully"),
            @OpenApiResponse(status = "408", content = @OpenApiContent(from = Response.class), description = "Request timed out")
        }
    )
    private static CompletableFuture<Response> handleAsyncRequest(io.javalin.http.Context ctx) {
        Request request = ctx.bodyAsClass(Request.class);

        // Generate correlation ID if not provided
        if (request.getCorrelationId() == null || request.getCorrelationId().isEmpty()) {
            request = new Request(request.getPayload());
        }

        log.info("Received API request with correlation ID: {}", request.getCorrelationId());

        // Send request and get future
        return requestHandler.sendRequest(request)
                .thenApply(response -> {
                    log.info("API request completed for correlation ID: {}", response.getCorrelationId());
                    return response;
                });
    }

    /**
     * Handles test requests with simulated delays.
     */
    @OpenApi(
        path = "/api/test/delayed-response",
        methods = HttpMethod.POST,
        summary = "Test endpoint with simulated delay",
        description = "Sends a request that will be responded to after a specified delay",
        tags = {"Testing"},
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = DelayedTestRequest.class)),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Response.class), description = "Request processed successfully")
        }
    )
    private static CompletableFuture<Response> handleDelayedTestRequest(io.javalin.http.Context ctx) {
        DelayedTestRequest testRequest = ctx.bodyAsClass(DelayedTestRequest.class);
        Request request = new Request(testRequest.payload());

        log.info("Test: Sending request with {} second delay", testRequest.delaySeconds());

        // Send the request
        CompletableFuture<Response> future = requestHandler.sendRequest(request);

        // Simulate delayed response in a separate thread
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(testRequest.delaySeconds() * 1000L);
                Response response = new Response(
                        request.getCorrelationId(),
                        "Delayed response after " + testRequest.delaySeconds() + " seconds",
                        true
                );
                requestHandler.receiveResponse(response);
            } catch (InterruptedException e) {
                log.error("Delayed response interrupted", e);
                Thread.currentThread().interrupt();
            }
        });

        return future;
    }

    // Helper classes for responses
    record ApiResponse(boolean success, String message) {}
    record StatusResponse(String status, int pendingRequests) {}
    record DelayedTestRequest(String payload, int delaySeconds) {}
}
