package com.recplatform.common.util;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Trace ID utility for distributed tracing.
 */
public class TraceIdUtil {

    private static final String TRACE_ID_KEY = "traceId";
    private static final ThreadLocal<String> TRACE_ID_HOLDER = new ThreadLocal<>();

    private TraceIdUtil() {
    }

    /**
     * Generate a new trace ID (UUID without dashes).
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Get current trace ID from MDC or ThreadLocal, generating one if absent.
     */
    public static String getTraceId() {
        String traceId = MDC.get(TRACE_ID_KEY);
        if (traceId == null) {
            traceId = TRACE_ID_HOLDER.get();
        }
        if (traceId == null) {
            traceId = generateTraceId();
            setTraceId(traceId);
        }
        return traceId;
    }

    /**
     * Set trace ID into both MDC and ThreadLocal.
     */
    public static void setTraceId(String traceId) {
        if (traceId != null) {
            MDC.put(TRACE_ID_KEY, traceId);
            TRACE_ID_HOLDER.set(traceId);
        }
    }

    /**
     * Remove trace ID from both MDC and ThreadLocal.
     */
    public static void removeTraceId() {
        MDC.remove(TRACE_ID_KEY);
        TRACE_ID_HOLDER.remove();
    }
}
