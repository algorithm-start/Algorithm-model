package com.recplatform.audit.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import com.recplatform.audit.dto.AuditEvent;
import com.recplatform.audit.service.AuditLogService;
import com.recplatform.common.util.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * HTTP interceptor that automatically records audit logs for all write
 * operations (POST, PUT, DELETE). Works at the Spring MVC level so it
 * captures all controllers regardless of AOP proxy limitations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditInterceptor implements HandlerInterceptor {

    private final AuditLogService auditLogService;

    private static final String ATTR_START_TIME = "audit_start_time";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String method = request.getMethod();
        if ("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method)) {
            request.setAttribute(ATTR_START_TIME, System.currentTimeMillis());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        String method = request.getMethod();
        if (!"POST".equals(method) && !"PUT".equals(method) && !"DELETE".equals(method)) {
            return;
        }

        // Skip audit log query endpoints to avoid infinite recursion
        String uri = request.getRequestURI();
        if (uri.contains("/audit/")) {
            return;
        }

        try {
            long startTime = (long) request.getAttribute(ATTR_START_TIME);
            long duration = System.currentTimeMillis() - startTime;

            String action = mapHttpMethodToAction(method, uri);
            String resource = extractResource(uri);

            Long userId = null;
            String username = null;
            try {
                if (StpUtil.isLogin()) {
                    userId = StpUtil.getLoginIdAsLong();
                    // Get actual username from session extra or fall back to request principal
                    Object sessionUsername = StpUtil.getSession().get("username");
                    username = sessionUsername != null ? sessionUsername.toString() : String.valueOf(userId);
                }
            } catch (Exception ignored) {
            }

            // For login, extract username from URI context
            if (uri.contains("/auth/login")) {
                action = "LOGIN";
                resource = "Auth: /api/v1/auth/login";
            }

            String ip = getClientIp(request);
            String userAgent = request.getHeader("User-Agent");
            int status = response.getStatus();
            boolean success = status >= 200 && status < 400;

            AuditEvent event = AuditEvent.builder()
                    .userId(userId)
                    .username(username)
                    .action(action)
                    .resource(resource)
                    .resourceId(extractResourceId(uri))
                    .result(success ? "SUCCESS" : "FAILURE")
                    .errorMessage(ex != null ? ex.getMessage() : (success ? null : "HTTP " + status))
                    .ip(ip)
                    .userAgent(userAgent)
                    .duration(duration)
                    .traceId(TraceIdUtil.getTraceId())
                    .build();

            auditLogService.log(event);
        } catch (Exception e) {
            log.debug("Failed to record audit log: {}", e.getMessage());
        }
    }

    private String mapHttpMethodToAction(String httpMethod, String uri) {
        return switch (httpMethod) {
            case "POST" -> "CREATE";
            case "PUT" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> "UNKNOWN";
        };
    }

    private String extractResource(String uri) {
        // /api/v1/iam/roles/123 -> "IAM Roles"
        String[] segments = uri.replaceFirst("/api/v1/", "").split("/");
        if (segments.length >= 2) {
            String module = capitalize(segments[0]);
            String entity = capitalize(segments[1]);
            return module + ": " + entity;
        } else if (segments.length == 1) {
            return capitalize(segments[0]);
        }
        return uri;
    }

    private String extractResourceId(String uri) {
        String[] segments = uri.split("/");
        for (int i = segments.length - 1; i >= 0; i--) {
            if (segments[i].matches("\\d+")) {
                return segments[i];
            }
        }
        return null;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
