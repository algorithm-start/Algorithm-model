package com.recplatform.iam.service;

import com.recplatform.iam.dto.WorkspaceCreateRequest;
import com.recplatform.iam.dto.WorkspaceMemberRequest;
import com.recplatform.iam.vo.WorkspaceVO;

import java.util.List;

public interface WorkspaceService {

    WorkspaceVO create(WorkspaceCreateRequest request);

    WorkspaceVO update(Long id, WorkspaceCreateRequest request);

    void delete(Long id);

    WorkspaceVO getById(Long id);

    List<WorkspaceVO> listMyWorkspaces();

    List<WorkspaceVO> listAll();

    void addMembers(Long workspaceId, WorkspaceMemberRequest request);

    void removeMember(Long workspaceId, Long userId);

    void setMemberRole(Long workspaceId, Long userId, String role);

    List<WorkspaceVO.MemberVO> getMembers(Long workspaceId);
}
