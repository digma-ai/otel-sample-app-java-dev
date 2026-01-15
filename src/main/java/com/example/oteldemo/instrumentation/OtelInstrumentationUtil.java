package com.example.oteldemo.instrumentation;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

@Component
public class OtelInstrumentationUtil {
    private static final Logger logger = LoggerFactory.getLogger(OtelInstrumentationUtil.class);
    private final Tracer tracer;

    public OtelInstrumentationUtil(OpenTelemetry openTelemetry) {
        this.tracer = openTelemetry.getTracer(getClass().getName());
    }

    /**
     * Traces a callable operation with custom attributes
     */
    public <T> T traceOperation(String operationName, Map<String, String> attributes, Callable<T> operation) {
        Span span = createSpan(operationName, attributes);
        try (Scope scope = span.makeCurrent()) {
            T result = operation.call();
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            logger.error("Error in traced operation: {}", operationName, e);
            throw new RuntimeException(e);
        } finally {
            span.end();
        }
    }

    /**
     * Traces a supplier operation
     */
    public <T> T traceOperation(String operationName, Supplier<T> operation) {
        return traceOperation(operationName, Map.of(), () -> operation.get());
    }

    /**
     * Creates a new span with the given name and attributes
     */
    public Span createSpan(String name, Map<String, String> attributes) {
        Span span = tracer.spanBuilder(name)
            .setSpanKind(SpanKind.INTERNAL)
            .setParent(Context.current())
            .startSpan();
        
        attributes.forEach((key, value) -> span.setAttribute(key, value));
        return span;
    }

    /**
     * Gets the current span context
     */
    public Span getCurrentSpan() {
        return Span.current();
    }

    /**
     * Adds an event to the current span
     */
    public void addEvent(String eventName, Map<String, String> attributes) {
        Span currentSpan = getCurrentSpan();
        if (currentSpan != null) {
            currentSpan.addEvent(eventName, attributes.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue()
                )));
        }
    }
}