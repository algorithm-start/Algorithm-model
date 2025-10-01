package com.recplatform.common.workspace;

/**
 * Marker interface for entities that support workspace-level data isolation.
 * Entities implementing this interface will have automatic workspace_id filtering
 * applied by the WorkspaceInterceptor.
 */
public interface WorkspaceAware {

    Long getWorkspaceId();

    void setWorkspaceId(Long workspaceId);
}
