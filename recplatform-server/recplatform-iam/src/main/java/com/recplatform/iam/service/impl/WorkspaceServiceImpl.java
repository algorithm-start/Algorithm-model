package com.recplatform.iam.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recplatform.iam.dto.WorkspaceCreateRequest;
import com.recplatform.iam.dto.WorkspaceMemberRequest;
import com.recplatform.iam.entity.UserEntity;
import com.recplatform.iam.entity.WorkspaceEntity;
import com.recplatform.iam.entity.WorkspaceMemberEntity;
import com.recplatform.iam.mapper.UserMapper;
import com.recplatform.iam.mapper.WorkspaceMapper;
import com.recplatform.iam.mapper.WorkspaceMemberMapper;
import com.recplatform.iam.service.WorkspaceService;
import com.recplatform.iam.vo.WorkspaceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceMapper workspaceMapper;
    private final WorkspaceMemberMapper memberMapper;
    private final UserMapper userMapper;

    private static final int MAX_ADMINS = 3;

    @Override
    @Transactional
    public WorkspaceVO create(WorkspaceCreateRequest request) {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        WorkspaceEntity entity = new WorkspaceEntity();
        entity.setName(request.getName());
        entity.setCode(request.getCode());
        entity.setDescription(request.getDescription());
        entity.setOwnerId(currentUserId);
        workspaceMapper.insert(entity);

        // Add creator as ADMIN member
        WorkspaceMemberEntity member = new WorkspaceMemberEntity();
        member.setWorkspaceId(entity.getId());
        member.setUserId(currentUserId);
        member.setRole("ADMIN");
        memberMapper.insert(member);

        return getById(entity.getId());
    }

    @Override
    @Transactional
    public WorkspaceVO update(Long id, WorkspaceCreateRequest request) {
        WorkspaceEntity entity = workspaceMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("Workspace not found");
        }
        checkWorkspaceAdmin(id);

        if (request.getName() != null) entity.setName(request.getName());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        workspaceMapper.updateById(entity);
        return getById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        WorkspaceEntity entity = workspaceMapper.selectById(id);
        if (entity == null) return;
        if ("default".equals(entity.getCode())) {
            throw new RuntimeException("Cannot delete default workspace");
        }
        checkWorkspaceAdmin(id);
        workspaceMapper.deleteById(id);
        // Remove all members
        memberMapper.delete(new LambdaQueryWrapper<WorkspaceMemberEntity>()
                .eq(WorkspaceMemberEntity::getWorkspaceId, id));
    }

    @Override
    public WorkspaceVO getById(Long id) {
        WorkspaceEntity entity = workspaceMapper.selectById(id);
        if (entity == null) return null;
        return toVO(entity);
    }

    @Override
    public List<WorkspaceVO> listMyWorkspaces() {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // Check if user is system admin
        boolean isSysAdmin = StpUtil.getRoleList().contains("ADMIN");
        if (isSysAdmin) {
            return listAll();
        }

        List<WorkspaceMemberEntity> memberships = memberMapper.selectList(
                new LambdaQueryWrapper<WorkspaceMemberEntity>()
                        .eq(WorkspaceMemberEntity::getUserId, currentUserId));

        if (memberships.isEmpty()) {
            // Return default workspace at minimum
            WorkspaceEntity defaultWs = workspaceMapper.selectOne(
                    new LambdaQueryWrapper<WorkspaceEntity>().eq(WorkspaceEntity::getCode, "default"));
            if (defaultWs != null) {
                return List.of(toVO(defaultWs));
            }
            return List.of();
        }

        List<Long> wsIds = memberships.stream()
                .map(WorkspaceMemberEntity::getWorkspaceId)
                .collect(Collectors.toList());
        List<WorkspaceEntity> workspaces = workspaceMapper.selectBatchIds(wsIds);
        return workspaces.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<WorkspaceVO> listAll() {
        List<WorkspaceEntity> all = workspaceMapper.selectList(
                new LambdaQueryWrapper<WorkspaceEntity>().orderByAsc(WorkspaceEntity::getCreateTime));
        return all.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addMembers(Long workspaceId, WorkspaceMemberRequest request) {
        checkWorkspaceAdmin(workspaceId);

        String role = request.getRole() != null ? request.getRole() : "MEMBER";

        if ("ADMIN".equals(role)) {
            long currentAdmins = memberMapper.selectCount(
                    new LambdaQueryWrapper<WorkspaceMemberEntity>()
                            .eq(WorkspaceMemberEntity::getWorkspaceId, workspaceId)
                            .eq(WorkspaceMemberEntity::getRole, "ADMIN"));
            if (currentAdmins + request.getUserIds().size() > MAX_ADMINS) {
                throw new RuntimeException("A workspace can have at most " + MAX_ADMINS + " admins");
            }
        }

        for (Long userId : request.getUserIds()) {
            // Check if already a member
            WorkspaceMemberEntity existing = memberMapper.selectOne(
                    new LambdaQueryWrapper<WorkspaceMemberEntity>()
                            .eq(WorkspaceMemberEntity::getWorkspaceId, workspaceId)
                            .eq(WorkspaceMemberEntity::getUserId, userId));
            if (existing != null) continue;

            WorkspaceMemberEntity member = new WorkspaceMemberEntity();
            member.setWorkspaceId(workspaceId);
            member.setUserId(userId);
            member.setRole(role);
            memberMapper.insert(member);
        }
    }

    @Override
    @Transactional
    public void removeMember(Long workspaceId, Long userId) {
        checkWorkspaceAdmin(workspaceId);
        memberMapper.delete(new LambdaQueryWrapper<WorkspaceMemberEntity>()
                .eq(WorkspaceMemberEntity::getWorkspaceId, workspaceId)
                .eq(WorkspaceMemberEntity::getUserId, userId));
    }

    @Override
    @Transactional
    public void setMemberRole(Long workspaceId, Long userId, String role) {
        checkWorkspaceAdmin(workspaceId);

        if ("ADMIN".equals(role)) {
            long currentAdmins = memberMapper.selectCount(
                    new LambdaQueryWrapper<WorkspaceMemberEntity>()
                            .eq(WorkspaceMemberEntity::getWorkspaceId, workspaceId)
                            .eq(WorkspaceMemberEntity::getRole, "ADMIN"));
            if (currentAdmins >= MAX_ADMINS) {
                throw new RuntimeException("A workspace can have at most " + MAX_ADMINS + " admins");
            }
        }

        WorkspaceMemberEntity member = memberMapper.selectOne(
                new LambdaQueryWrapper<WorkspaceMemberEntity>()
                        .eq(WorkspaceMemberEntity::getWorkspaceId, workspaceId)
                        .eq(WorkspaceMemberEntity::getUserId, userId));
        if (member == null) {
            throw new RuntimeException("User is not a member of this workspace");
        }
        member.setRole(role);
        memberMapper.updateById(member);
    }

    @Override
    public List<WorkspaceVO.MemberVO> getMembers(Long workspaceId) {
        List<WorkspaceMemberEntity> members = memberMapper.selectList(
                new LambdaQueryWrapper<WorkspaceMemberEntity>()
                        .eq(WorkspaceMemberEntity::getWorkspaceId, workspaceId));

        if (members.isEmpty()) return List.of();

        List<Long> userIds = members.stream()
                .map(WorkspaceMemberEntity::getUserId)
                .collect(Collectors.toList());
        List<UserEntity> users = userMapper.selectBatchIds(userIds);
        Map<Long, UserEntity> userMap = users.stream()
                .collect(Collectors.toMap(UserEntity::getId, u -> u));

        List<WorkspaceVO.MemberVO> result = new ArrayList<>();
        for (WorkspaceMemberEntity m : members) {
            WorkspaceVO.MemberVO vo = new WorkspaceVO.MemberVO();
            vo.setId(m.getId());
            vo.setUserId(m.getUserId());
            vo.setRole(m.getRole());
            vo.setCreateTime(m.getCreateTime());
            UserEntity user = userMap.get(m.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setNickname(user.getNickname());
            }
            result.add(vo);
        }
        return result;
    }

    private WorkspaceVO toVO(WorkspaceEntity entity) {
        WorkspaceVO vo = new WorkspaceVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setCode(entity.getCode());
        vo.setDescription(entity.getDescription());
        vo.setOwnerId(entity.getOwnerId());
        vo.setCreateTime(entity.getCreateTime());

        // Member count
        long count = memberMapper.selectCount(
                new LambdaQueryWrapper<WorkspaceMemberEntity>()
                        .eq(WorkspaceMemberEntity::getWorkspaceId, entity.getId()));
        vo.setMemberCount((int) count);

        // Owner name
        UserEntity owner = userMapper.selectById(entity.getOwnerId());
        if (owner != null) {
            vo.setOwnerName(owner.getUsername());
        }

        // Current user's role in this workspace
        try {
            Long currentUserId = StpUtil.getLoginIdAsLong();
            WorkspaceMemberEntity membership = memberMapper.selectOne(
                    new LambdaQueryWrapper<WorkspaceMemberEntity>()
                            .eq(WorkspaceMemberEntity::getWorkspaceId, entity.getId())
                            .eq(WorkspaceMemberEntity::getUserId, currentUserId));
            if (membership != null) {
                vo.setMyRole(membership.getRole());
            }
        } catch (Exception ignored) {
        }

        return vo;
    }

    private void checkWorkspaceAdmin(Long workspaceId) {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // System admin can manage any workspace
        if (StpUtil.getRoleList().contains("ADMIN")) return;

        WorkspaceMemberEntity membership = memberMapper.selectOne(
                new LambdaQueryWrapper<WorkspaceMemberEntity>()
                        .eq(WorkspaceMemberEntity::getWorkspaceId, workspaceId)
                        .eq(WorkspaceMemberEntity::getUserId, currentUserId)
                        .eq(WorkspaceMemberEntity::getRole, "ADMIN"));
        if (membership == null) {
            throw new RuntimeException("Only workspace admins can perform this action");
        }
    }
}
