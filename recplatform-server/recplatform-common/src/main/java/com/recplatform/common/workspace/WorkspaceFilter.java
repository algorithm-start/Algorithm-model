package com.recplatform.common.workspace;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Spring MVC interceptor that extracts X-Workspace-Id from the request header
 * and sets it in the WorkspaceContextHolder for downstream MyBatis interceptor.
 * Admin role detection is handled by a separate AdminWorkspaceInterceptor in the IAM module.
 */
@Slf4j
@Component
public class WorkspaceFilter implements HandlerInterceptor {

    public static final String HEADER_WORKSPACE_ID = "X-Workspace-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String wsIdHeader = request.getHeader(HEADER_WORKSPACE_ID);
        if (wsIdHeader != null && !wsIdHeader.isBlank()) {
            try {
                Long workspaceId = Long.parseLong(wsIdHeader.trim());
                WorkspaceContextHolder.setWorkspaceId(workspaceId);
            } catch (NumberFormatException e) {
                log.debug("Invalid X-Workspace-Id header: {}", wsIdHeader);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        WorkspaceContextHolder.clear();
    }
}
