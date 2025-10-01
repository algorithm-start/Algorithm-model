package com.recplatform.iam.controller;

import com.recplatform.common.result.Result;
import com.recplatform.iam.dto.WorkspaceCreateRequest;
import com.recplatform.iam.dto.WorkspaceMemberRequest;
import com.recplatform.iam.service.WorkspaceService;
import com.recplatform.iam.vo.WorkspaceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @PostMapping
    public Result<WorkspaceVO> create(@RequestBody WorkspaceCreateRequest request) {
        return Result.success(workspaceService.create(request));
    }

    @PutMapping("/{id}")
    public Result<WorkspaceVO> update(@PathVariable Long id, @RequestBody WorkspaceCreateRequest request) {
        return Result.success(workspaceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        workspaceService.delete(id);
        return Result.success(null);
    }

    @GetMapping("/{id}")
    public Result<WorkspaceVO> getById(@PathVariable Long id) {
        return Result.success(workspaceService.getById(id));
    }

    @GetMapping("/mine")
    public Result<List<WorkspaceVO>> listMyWorkspaces() {
        return Result.success(workspaceService.listMyWorkspaces());
    }

    @GetMapping
    public Result<List<WorkspaceVO>> listAll() {
        return Result.success(workspaceService.listAll());
    }

    @PostMapping("/{id}/members")
    public Result<Void> addMembers(@PathVariable Long id, @RequestBody WorkspaceMemberRequest request) {
        workspaceService.addMembers(id, request);
        return Result.success(null);
    }

    @DeleteMapping("/{id}/members/{userId}")
    public Result<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        workspaceService.removeMember(id, userId);
        return Result.success(null);
    }

    @PutMapping("/{id}/members/{userId}/role")
    public Result<Void> setMemberRole(@PathVariable Long id, @PathVariable Long userId,
                                      @RequestParam String role) {
        workspaceService.setMemberRole(id, userId, role);
        return Result.success(null);
    }

    @GetMapping("/{id}/members")
    public Result<List<WorkspaceVO.MemberVO>> getMembers(@PathVariable Long id) {
        return Result.success(workspaceService.getMembers(id));
    }
}
