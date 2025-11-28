# Async Correlation ID Web Server

A Javalin-based web server that demonstrates async request/response handling using correlation IDs.

## Features

- Async API endpoints that wait for responses using CompletableFutures
- Correlation ID-based request/response matching
- Automatic timeout handling (30 seconds default)
- RESTful API design

## API Endpoints

### POST /api/request
Sends an async request and waits for a matching response.

**Request:**
```json
{
  "payload": "your request data"
}
```

**Response:**
```json
{
  "correlationId": "generated-uuid",
  "result": "response data",
  "success": true
}
```

The endpoint will block until:
- A matching response is received via `/api/response`
- The request times out (30 seconds)

### POST /api/response
Submits a response that completes a pending request.

**Request:**
```json
{
  "correlationId": "uuid-from-request",
  "result": "your response data",
  "success": true
}
```

**Response:**
```json
{
  "success": true,
  "message": "Response received and processed"
}
```

### GET /api/status
Check server status and pending request count.

**Response:**
```json
{
  "status": "Server is running",
  "pendingRequests": 0
}
```

### POST /api/test/delayed-response
Test endpoint that simulates a delayed response (for testing purposes).

**Request:**
```json
{
  "payload": "test data",
  "delaySeconds": 2
}
```

## Running the Server

```bash
./gradlew run
```

The server will start on port 7070.

## Testing Examples

### Example 1: Manual Request/Response

Terminal 1 - Send a request:
```bash
curl -X POST http://localhost:7070/api/request \
  -H "Content-Type: application/json" \
  -d '{"payload": "Hello, World!"}'
```

This will block and wait for a response.

Terminal 2 - Send the matching response (use the correlation ID from the logs):
```bash
curl -X POST http://localhost:7070/api/response \
  -H "Content-Type: application/json" \
  -d '{
    "correlationId": "PASTE-CORRELATION-ID-HERE",
    "result": "Response received!",
    "success": true
  }'
```

Terminal 1 will now receive the response and complete.

### Example 2: Using the Test Endpoint

```bash
curl -X POST http://localhost:7070/api/test/delayed-response \
  -H "Content-Type: application/json" \
  -d '{"payload": "test", "delaySeconds": 3}'
```

This will automatically send a response after 3 seconds.

### Example 3: Check Status

```bash
curl http://localhost:7070/api/status
```

## How It Works

1. Client sends a POST request to `/api/request`
2. Server generates a correlation ID and stores a CompletableFuture
3. The endpoint waits asynchronously for the future to complete
4. When `/api/response` is called with a matching correlation ID, the future is completed
5. The original request returns with the response data

This pattern is useful for:
- Event-driven architectures
- Message queue-based systems
- Webhook-based integrations
- Any scenario where request and response arrive separately
# test-javalin-swagger-
