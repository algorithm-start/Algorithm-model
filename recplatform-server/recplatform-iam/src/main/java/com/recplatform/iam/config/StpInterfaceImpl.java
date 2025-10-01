package com.recplatform.iam.config;

import cn.dev33.satoken.stp.StpInterface;
import com.recplatform.iam.entity.RoleEntity;
import com.recplatform.iam.entity.UserRoleEntity;
import com.recplatform.iam.mapper.RoleMapper;
import com.recplatform.iam.mapper.UserRoleMapper;
import com.recplatform.iam.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sa-Token StpInterface implementation for permission and role checking.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final UserService userService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        try {
            long userId = Long.parseLong(loginId.toString());
            Set<String> permissions = userService.getUserPermissions(userId);
            return List.copyOf(permissions);
        } catch (Exception e) {
            log.error("Failed to get permission list for user: {}", loginId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        try {
            long userId = Long.parseLong(loginId.toString());
            List<UserRoleEntity> userRoles = userRoleMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserRoleEntity>()
                            .eq(UserRoleEntity::getUserId, userId)
            );

            if (userRoles.isEmpty()) {
                return Collections.emptyList();
            }

            List<Long> roleIds = userRoles.stream()
                    .map(UserRoleEntity::getRoleId)
                    .collect(Collectors.toList());

            List<RoleEntity> roles = roleMapper.selectBatchIds(roleIds);
            return roles.stream()
                    .map(RoleEntity::getCode)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get role list for user: {}", loginId, e);
            return Collections.emptyList();
        }
    }
}
