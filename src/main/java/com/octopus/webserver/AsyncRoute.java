package com.octopus.webserver;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Represents an async HTTP route that returns a CompletableFuture.
 * This object encapsulates all information needed to define an async endpoint
 * and can be registered with Javalin.
 *
 * @param <T> The type of the response
 */
@Slf4j
public record AsyncRoute<T>(
        HandlerType method,
        String path,
        Function<Context, CompletableFuture<T>> routeAction
) {
    /**
     * Registers this async route with the Javalin app.
     */
    public void registerWith(Javalin app) {
        switch (method) {
            case GET -> app.get(path, ctx -> {
                CompletableFuture<T> future = routeAction.apply(ctx);
                ctx.future(() -> future);
            });
            case POST -> app.post(path, ctx -> {
                CompletableFuture<T> future = routeAction.apply(ctx);
                ctx.future(() -> future);
            });
            case PUT -> app.put(path, ctx -> {
                CompletableFuture<T> future = routeAction.apply(ctx);
                ctx.future(() -> future);
            });
            case DELETE -> app.delete(path, ctx -> {
                CompletableFuture<T> future = routeAction.apply(ctx);
                ctx.future(() -> future);
            });
            case PATCH -> app.patch(path, ctx -> {
                CompletableFuture<T> future = routeAction.apply(ctx);
                ctx.future(() -> future);
            });
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
        log.info("Registered {} {}", method, path);
    }

    /**
     * Creates a GET async route.
     */
    public static <T> AsyncRoute<T> get(String path, Function<Context, CompletableFuture<T>> routeAction) {
        return new AsyncRoute<>(HandlerType.GET, path, routeAction);
    }

    /**
     * Creates a POST async route.
     */
    public static <T> AsyncRoute<T> post(String path, Function<Context, CompletableFuture<T>> routeAction) {
        return new AsyncRoute<>(HandlerType.POST, path, routeAction);
    }

    /**
     * Creates a PUT async route.
     */
    public static <T> AsyncRoute<T> put(String path, Function<Context, CompletableFuture<T>> routeAction) {
        return new AsyncRoute<>(HandlerType.PUT, path, routeAction);
    }

    /**
     * Creates a DELETE async route.
     */
    public static <T> AsyncRoute<T> delete(String path, Function<Context, CompletableFuture<T>> routeAction) {
        return new AsyncRoute<>(HandlerType.DELETE, path, routeAction);
    }

    /**
     * Creates a PATCH async route.
     */
    public static <T> AsyncRoute<T> patch(String path, Function<Context, CompletableFuture<T>> routeAction) {
        return new AsyncRoute<>(HandlerType.PATCH, path, routeAction);
    }
}
