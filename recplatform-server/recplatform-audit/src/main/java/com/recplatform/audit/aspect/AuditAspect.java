package com.recplatform.audit.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.recplatform.audit.annotation.Auditable;
import com.recplatform.audit.dto.AuditEvent;
import com.recplatform.audit.service.AuditLogService;
import com.recplatform.common.util.JsonUtil;
import com.recplatform.common.util.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * AOP aspect that intercepts methods annotated with @Auditable.
 * Captures who, what, when, result, duration, and IP address.
 * Fail-safe: never breaks the business flow if logging fails.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogService auditLogService;

    @Around("@annotation(com.recplatform.audit.annotation.Auditable)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            // Always log, even if business method threw exception
            try {
                long duration = System.currentTimeMillis() - startTime;
                logAuditEvent(joinPoint, result, error, duration);
            } catch (Exception e) {
                // Fail-safe: never break the business flow
                log.error("Failed to create audit log: {}", e.getMessage(), e);
            }
        }
    }

    private void logGlobalAuditEvent(ProceedingJoinPoint joinPoint, Throwable error, long duration) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // Determine action from HTTP method annotation
        String action = "UNKNOWN";
        if (method.isAnnotationPresent(org.springframework.web.bind.annotation.PostMapping.class)) {
            action = "CREATE";
        } else if (method.isAnnotationPresent(org.springframework.web.bind.annotation.PutMapping.class)) {
            action = "UPDATE";
        } else if (method.isAnnotationPresent(org.springframework.web.bind.annotation.DeleteMapping.class)) {
            action = "DELETE";
        }

        // Skip if already handled by @Auditable
        if (method.isAnnotationPresent(Auditable.class)) {
            return;
        }

        // Determine resource from Controller class RequestMapping
        String resource = signature.getDeclaringType().getSimpleName().replace("Controller", "");

        // Get current user
        Long userId = null;
        String username = null;
        try {
            if (StpUtil.isLogin()) {
                userId = StpUtil.getLoginIdAsLong();
                username = String.valueOf(StpUtil.getLoginId());
            }
        } catch (Exception e) {
            // not logged in (e.g. login endpoint itself)
        }

        // Get request info
        String ip = null;
        String userAgent = null;
        String requestUri = null;
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                ip = getClientIp(request);
                userAgent = request.getHeader("User-Agent");
                requestUri = request.getRequestURI();
            }
        } catch (Exception e) {
            // ignore
        }

        // For LOGIN action detection
        if (requestUri != null && requestUri.contains("/auth/login")) {
            action = "LOGIN";
            resource = "Auth";
        }

        // Extract resource ID from URL
        String resourceId = extractResourceId(joinPoint);

        AuditEvent event = AuditEvent.builder()
                .userId(userId)
                .username(username)
                .action(action)
                .resource(resource + (requestUri != null ? ": " + requestUri : ""))
                .resourceId(resourceId)
                .detail(null)
                .result(error == null ? "SUCCESS" : "FAILURE")
                .errorMessage(error != null ? error.getMessage() : null)
                .ip(ip)
                .userAgent(userAgent)
                .duration(duration)
                .traceId(com.recplatform.common.util.TraceIdUtil.getTraceId())
                .build();

        auditLogService.log(event);
    }

    private void logAuditEvent(ProceedingJoinPoint joinPoint, Object result, Throwable error, long duration) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Auditable auditable = method.getAnnotation(Auditable.class);

        // Get current user info from Sa-Token
        Long userId = null;
        String username = null;
        try {
            if (StpUtil.isLogin()) {
                userId = StpUtil.getLoginIdAsLong();
                username = String.valueOf(StpUtil.getLoginId());
            }
        } catch (Exception e) {
            log.debug("Failed to get current user from Sa-Token: {}", e.getMessage());
        }

        // Get request info
        String ip = null;
        String userAgent = null;
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                ip = getClientIp(request);
                userAgent = request.getHeader("User-Agent");
            }
        } catch (Exception e) {
            log.debug("Failed to get request info: {}", e.getMessage());
        }

        // Extract detail using SpEL if provided
        String detail = null;
        if (auditable.detail().isEmpty()) {
            // Default: serialize all method arguments
            try {
                String[] paramNames = signature.getParameterNames();
                Object[] args = joinPoint.getArgs();
                if (paramNames != null && paramNames.length > 0) {
                    Map<String, Object> paramMap = new HashMap<>();
                    for (int i = 0; i < paramNames.length; i++) {
                        // Skip HttpServletRequest and HttpServletResponse
                        if (args[i] instanceof HttpServletRequest) {
                            continue;
                        }
                        paramMap.put(paramNames[i], args[i]);
                    }
                    detail = JsonUtil.toJson(paramMap);
                }
            } catch (Exception e) {
                log.debug("Failed to serialize method args: {}", e.getMessage());
            }
        } else {
            try {
                org.springframework.expression.ExpressionParser parser =
                        new org.springframework.expression.spel.standard.SpelExpressionParser();
                org.springframework.expression.EvaluationContext context =
                        new org.springframework.context.expression.MethodBasedEvaluationContext(
                                null, method, joinPoint.getArgs(),
                                new org.springframework.core.DefaultParameterNameDiscoverer()
                        );
                Object value = parser.parseExpression(auditable.detail()).getValue(context);
                detail = value != null ? JsonUtil.toJson(value) : null;
            } catch (Exception e) {
                log.debug("Failed to evaluate SpEL expression '{}': {}", auditable.detail(), e.getMessage());
            }
        }

        // Extract resource ID from result or path variable
        String resourceId = extractResourceId(joinPoint);

        AuditEvent event = AuditEvent.builder()
                .userId(userId)
                .username(username)
                .action(auditable.action())
                .resource(auditable.resource())
                .resourceId(resourceId)
                .detail(detail)
                .result(error == null ? "SUCCESS" : "FAILURE")
                .errorMessage(error != null ? error.getMessage() : null)
                .ip(ip)
                .userAgent(userAgent)
                .duration(duration)
                .traceId(TraceIdUtil.getTraceId())
                .build();

        auditLogService.log(event);
    }

    private String extractResourceId(ProceedingJoinPoint joinPoint) {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String uri = request.getRequestURI();
                // Try to extract ID from path (e.g., /api/v1/iam/users/123)
                String[] segments = uri.split("/");
                if (segments.length > 0) {
                    String lastSegment = segments[segments.length - 1];
                    if (lastSegment.matches("\\d+")) {
                        return lastSegment;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract resource ID: {}", e.getMessage());
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Handle multiple proxies
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
