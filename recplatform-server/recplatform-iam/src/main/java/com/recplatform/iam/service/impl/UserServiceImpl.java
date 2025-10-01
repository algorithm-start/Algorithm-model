package com.recplatform.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recplatform.common.exception.BusinessException;
import com.recplatform.common.model.PageResult;
import com.recplatform.common.result.ResultCode;
import com.recplatform.iam.dto.UserCreateRequest;
import com.recplatform.iam.dto.UserUpdateRequest;
import com.recplatform.iam.entity.RoleEntity;
import com.recplatform.iam.entity.RolePermissionEntity;
import com.recplatform.iam.entity.UserEntity;
import com.recplatform.iam.entity.UserRoleEntity;
import com.recplatform.iam.entity.WorkspaceEntity;
import com.recplatform.iam.entity.WorkspaceMemberEntity;
import com.recplatform.iam.mapper.RoleMapper;
import com.recplatform.iam.mapper.RolePermissionMapper;
import com.recplatform.iam.mapper.UserMapper;
import com.recplatform.iam.mapper.UserRoleMapper;
import com.recplatform.iam.mapper.WorkspaceMapper;
import com.recplatform.iam.mapper.WorkspaceMemberMapper;
import com.recplatform.iam.service.UserService;
import com.recplatform.iam.vo.RoleVO;
import com.recplatform.iam.vo.UserVO;
import com.recplatform.common.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * User service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final WorkspaceMapper workspaceMapper;
    private final WorkspaceMemberMapper workspaceMemberMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO createUser(UserCreateRequest request) {
        // Check username uniqueness
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, request.getUsername())
        );
        if (count > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Username already exists");
        }

        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus("ACTIVE");
        userMapper.insert(user);

        // Assign roles if provided
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            assignRoles(user.getId(), new HashSet<>(request.getRoleIds()));
        }

        // Every new user gets a personal workspace where they are the ADMIN,
        // so that they always have at least one space they fully control.
        createPersonalWorkspace(user);

        return getUser(user.getId());
    }

    /**
     * Creates a personal workspace owned by the given user and adds the user as
     * its ADMIN member. The workspace code is derived from the username and made
     * unique to avoid collisions with existing workspaces.
     */
    private void createPersonalWorkspace(UserEntity user) {
        String workspaceCode = generateUniqueWorkspaceCode(user.getUsername());

        WorkspaceEntity workspace = new WorkspaceEntity();
        String displayName = StringUtils.hasText(user.getNickname())
                ? user.getNickname()
                : user.getUsername();
        workspace.setName(displayName + " 的空间");
        workspace.setCode(workspaceCode);
        workspace.setDescription("用户 " + user.getUsername() + " 的专属工作空间");
        workspace.setOwnerId(user.getId());
        workspaceMapper.insert(workspace);

        WorkspaceMemberEntity member = new WorkspaceMemberEntity();
        member.setWorkspaceId(workspace.getId());
        member.setUserId(user.getId());
        member.setRole("ADMIN");
        workspaceMemberMapper.insert(member);

        log.info("Created personal workspace '{}' (code={}) for user {}",
                workspace.getName(), workspaceCode, user.getUsername());
    }

    private String generateUniqueWorkspaceCode(String username) {
        String base = "ws-" + username.toLowerCase().replaceAll("[^a-z0-9]", "-");
        String candidate = base;
        int suffix = 1;
        while (workspaceMapper.selectCount(
                new LambdaQueryWrapper<WorkspaceEntity>().eq(WorkspaceEntity::getCode, candidate)) > 0) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    @Override
    public UserVO getUser(Long id) {
        UserEntity user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "User not found");
        }
        return toUserVO(user);
    }

    @Override
    public PageResult<UserVO> listUsers(String username, String status, Integer page, Integer size) {
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(username)) {
            wrapper.like(UserEntity::getUsername, username);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(UserEntity::getStatus, status);
        }
        wrapper.orderByDesc(UserEntity::getCreateTime);

        Page<UserEntity> pageResult = userMapper.selectPage(
                new Page<>(page, size), wrapper
        );

        List<UserVO> userVOs = pageResult.getRecords().stream()
                .map(this::toUserVO)
                .collect(Collectors.toList());

        return PageResult.<UserVO>builder()
                .records(userVOs)
                .total(pageResult.getTotal())
                .page(pageResult.getCurrent())
                .size(pageResult.getSize())
                .pages(pageResult.getPages())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateUser(Long id, UserUpdateRequest request) {
        UserEntity user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "User not found");
        }

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        userMapper.updateById(user);
        return getUser(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        UserEntity user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "User not found");
        }
        // Soft delete by setting status to DISABLED
        user.setStatus("DISABLED");
        userMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long id, String newPassword) {
        UserEntity user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "User not found");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, Set<Long> roleIds) {
        // Remove existing roles
        userRoleMapper.delete(
                new LambdaQueryWrapper<UserRoleEntity>().eq(UserRoleEntity::getUserId, userId)
        );

        // Assign new roles
        for (Long roleId : roleIds) {
            UserRoleEntity userRole = new UserRoleEntity();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRoleMapper.insert(userRole);
        }
    }

    @Override
    public Set<String> getUserPermissions(Long userId) {
        // Get user's roles
        List<UserRoleEntity> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRoleEntity>().eq(UserRoleEntity::getUserId, userId)
        );

        if (userRoles.isEmpty()) {
            return Collections.emptySet();
        }

        // Get permission codes from all roles
        Set<String> permissions = new HashSet<>();
        for (UserRoleEntity userRole : userRoles) {
            List<RolePermissionEntity> rolePermissions = rolePermissionMapper.selectList(
                    new LambdaQueryWrapper<RolePermissionEntity>().eq(RolePermissionEntity::getRoleId, userRole.getRoleId())
            );
            rolePermissions.stream()
                    .map(RolePermissionEntity::getPermissionCode)
                    .forEach(permissions::add);
        }

        return permissions;
    }

    @Override
    public UserVO getUserByUsername(String username) {
        UserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, username)
        );
        if (user == null) {
            return null;
        }
        return toUserVO(user);
    }

    /**
     * Convert entity to VO.
     */
    private UserVO toUserVO(UserEntity user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setLastLoginIp(user.getLastLoginIp());
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());

        // Load roles
        List<UserRoleEntity> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRoleEntity>().eq(UserRoleEntity::getUserId, user.getId())
        );

        if (!userRoles.isEmpty()) {
            List<Long> roleIds = userRoles.stream()
                    .map(UserRoleEntity::getRoleId)
                    .collect(Collectors.toList());
            List<RoleEntity> roles = roleMapper.selectBatchIds(roleIds);
            List<RoleVO> roleVOs = roles.stream().map(role -> {
                RoleVO roleVO = new RoleVO();
                roleVO.setId(role.getId());
                roleVO.setCode(role.getCode());
                roleVO.setName(role.getName());
                roleVO.setDescription(role.getDescription());
                roleVO.setStatus(role.getStatus());
                roleVO.setCreateTime(role.getCreateTime());
                roleVO.setUpdateTime(role.getUpdateTime());

                // Load permission codes for this role
                List<RolePermissionEntity> rpList = rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<RolePermissionEntity>().eq(RolePermissionEntity::getRoleId, role.getId())
                );
                roleVO.setPermissionCodes(rpList.stream()
                        .map(RolePermissionEntity::getPermissionCode)
                        .collect(Collectors.toList()));

                // Deserialize menus
                if (role.getMenus() != null && !role.getMenus().isBlank()) {
                    try {
                        roleVO.setMenus(JsonUtil.fromJson(role.getMenus(),
                                new TypeReference<List<String>>() {}));
                    } catch (Exception e) {
                        log.warn("Failed to parse menus for role {}: {}", role.getId(), e.getMessage());
                        roleVO.setMenus(Collections.emptyList());
                    }
                } else {
                    roleVO.setMenus(Collections.emptyList());
                }

                return roleVO;
            }).collect(Collectors.toList());
            vo.setRoles(roleVOs);

            // Merge menus from all roles (union)
            Set<String> mergedMenus = new LinkedHashSet<>();
            for (RoleVO roleVO : roleVOs) {
                if (roleVO.getMenus() != null) {
                    mergedMenus.addAll(roleVO.getMenus());
                }
            }
            vo.setMenus(new ArrayList<>(mergedMenus));
        } else {
            vo.setRoles(Collections.emptyList());
            vo.setMenus(Collections.emptyList());
        }

        return vo;
    }
}
