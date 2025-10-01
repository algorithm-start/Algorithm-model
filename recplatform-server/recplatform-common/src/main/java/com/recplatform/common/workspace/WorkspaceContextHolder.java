package com.recplatform.common.workspace;

/**
 * Thread-local holder for the current workspace context.
 * Set by the WorkspaceFilter at the HTTP layer, read by the MyBatis interceptor.
 */
public final class WorkspaceContextHolder {

    private static final ThreadLocal<Long> WORKSPACE_ID = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> SKIP_FILTER = new ThreadLocal<>();

    private WorkspaceContextHolder() {}

    public static void setWorkspaceId(Long workspaceId) {
        WORKSPACE_ID.set(workspaceId);
    }

    public static Long getWorkspaceId() {
        return WORKSPACE_ID.get();
    }

    /**
     * When true, workspace filtering is skipped (for system admin users).
     */
    public static void setSkipFilter(boolean skip) {
        SKIP_FILTER.set(skip);
    }

    public static boolean shouldSkipFilter() {
        Boolean skip = SKIP_FILTER.get();
        return skip != null && skip;
    }

    public static void clear() {
        WORKSPACE_ID.remove();
        SKIP_FILTER.remove();
    }
}
