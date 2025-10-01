package com.recplatform.iam.config;

import cn.dev33.satoken.stp.StpUtil;
import com.recplatform.common.workspace.WorkspaceContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that detects system admin users and skips workspace filtering
 * when no workspace header is provided.
 */
@Slf4j
@Component
public class AdminWorkspaceInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            if (StpUtil.isLogin() && StpUtil.getRoleList().contains("ADMIN")) {
                // Admin with explicit workspace header → filter to that workspace
                // Admin without workspace header → see all data
                if (WorkspaceContextHolder.getWorkspaceId() == null) {
                    WorkspaceContextHolder.setSkipFilter(true);
                }
            }
        } catch (Exception ignored) {
        }
        return true;
    }
}
