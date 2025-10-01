package com.recplatform.iam.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recplatform.iam.entity.*;
import com.recplatform.iam.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * Initializes default data: admin user, default roles, and permissions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final WorkspaceMapper workspaceMapper;
    private final WorkspaceMemberMapper workspaceMemberMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** All menu keys available in the system. ADMIN gets all of them by default. */
    private static final String ALL_MENUS_JSON =
            "[\"dashboard\",\"solver\",\"orchestrator\",\"data\",\"admin\"]";

    @PostConstruct
    public void init() {
        log.info("Initializing IAM default data...");
        initDefaultPermissions();
        initDefaultRoles();
        initDefaultAdminUser();
        initDefaultWorkspace();
        log.info("IAM default data initialization completed.");
    }

    private void initDefaultPermissions() {
        // Define all module permissions
        Map<String, List<String[]>> modulePermissions = new LinkedHashMap<>();
        modulePermissions.put("solver", Arrays.asList(
                new String[]{"solver:problem:create", "Create Problem", "API"},
                new String[]{"solver:problem:read", "Read Problem", "API"},
                new String[]{"solver:problem:update", "Update Problem", "API"},
                new String[]{"solver:problem:delete", "Delete Problem", "API"},
                new String[]{"solver:solve:execute", "Execute Solve", "API"},
                new String[]{"solver:solve:cancel", "Cancel Solve", "API"},
                new String[]{"solver:result:read", "Read Result", "API"},
                new String[]{"solver:result:export", "Export Result", "API"}
        ));
        modulePermissions.put("data", Arrays.asList(
                new String[]{"data:source:create", "Create Data Source", "API"},
                new String[]{"data:source:read", "Read Data Source", "API"},
                new String[]{"data:source:update", "Update Data Source", "API"},
                new String[]{"data:source:delete", "Delete Data Source", "API"},
                new String[]{"data:pipeline:create", "Create Pipeline", "API"},
                new String[]{"data:pipeline:read", "Read Pipeline", "API"},
                new String[]{"data:pipeline:execute", "Execute Pipeline", "API"},
                new String[]{"data:transform:read", "Read Transform", "API"}
        ));
        modulePermissions.put("orchestrator", Arrays.asList(
                new String[]{"orchestrator:flow:create", "Create Flow", "API"},
                new String[]{"orchestrator:flow:read", "Read Flow", "API"},
                new String[]{"orchestrator:flow:update", "Update Flow", "API"},
                new String[]{"orchestrator:flow:delete", "Delete Flow", "API"},
                new String[]{"orchestrator:flow:execute", "Execute Flow", "API"},
                new String[]{"orchestrator:flow:cancel", "Cancel Flow", "API"}
        ));
        modulePermissions.put("iam", Arrays.asList(
                new String[]{"iam:user:create", "Create User", "API"},
                new String[]{"iam:user:read", "Read User", "API"},
                new String[]{"iam:user:update", "Update User", "API"},
                new String[]{"iam:user:delete", "Delete User", "API"},
                new String[]{"iam:role:create", "Create Role", "API"},
                new String[]{"iam:role:read", "Read Role", "API"},
                new String[]{"iam:role:update", "Update Role", "API"},
                new String[]{"iam:role:delete", "Delete Role", "API"},
                new String[]{"iam:permission:read", "Read Permission", "API"}
        ));
        modulePermissions.put("audit", Arrays.asList(
                new String[]{"audit:log:read", "Read Audit Log", "API"},
                new String[]{"audit:log:export", "Export Audit Log", "API"},
                new String[]{"audit:alert:create", "Create Alert Rule", "API"},
                new String[]{"audit:alert:read", "Read Alert Rule", "API"},
                new String[]{"audit:alert:update", "Update Alert Rule", "API"},
                new String[]{"audit:alert:delete", "Delete Alert Rule", "API"}
        ));

        for (Map.Entry<String, List<String[]>> entry : modulePermissions.entrySet()) {
            String module = entry.getKey();
            for (String[] perm : entry.getValue()) {
                createPermissionIfNotExists(perm[0], perm[1], module, perm[2]);
            }
        }
    }

    private void createPermissionIfNotExists(String code, String name, String module, String type) {
        Long count = permissionMapper.selectCount(
                new LambdaQueryWrapper<PermissionEntity>().eq(PermissionEntity::getCode, code)
        );
        if (count == 0) {
            PermissionEntity permission = new PermissionEntity();
            permission.setCode(code);
            permission.setName(name);
            permission.setModule(module);
            permission.setType(type);
            permissionMapper.insert(permission);
            log.debug("Created permission: {}", code);
        }
    }

    private void initDefaultRoles() {
        createRoleIfNotExists("ADMIN", "Administrator", "Full system access", ALL_MENUS_JSON);
        createRoleIfNotExists("USER", "User", "Standard user access",
                "[\"dashboard\",\"solver\",\"orchestrator\",\"data\"]");
        createRoleIfNotExists("VIEWER", "Viewer", "Read-only access",
                "[\"dashboard\",\"solver\",\"data\"]");
    }

    private void createRoleIfNotExists(String code, String name, String description, String menusJson) {
        Long count = roleMapper.selectCount(
                new LambdaQueryWrapper<RoleEntity>().eq(RoleEntity::getCode, code)
        );
        if (count == 0) {
            RoleEntity role = new RoleEntity();
            role.setCode(code);
            role.setName(name);
            role.setDescription(description);
            role.setStatus("ACTIVE");
            role.setMenus(menusJson);
            roleMapper.insert(role);
            log.debug("Created role: {} with menus: {}", code, menusJson);
        } else {
            // Update menus for existing role if menus is null (migration)
            RoleEntity existingRole = roleMapper.selectOne(
                    new LambdaQueryWrapper<RoleEntity>().eq(RoleEntity::getCode, code)
            );
            if (existingRole != null && (existingRole.getMenus() == null || existingRole.getMenus().isBlank())) {
                existingRole.setMenus(menusJson);
                roleMapper.updateById(existingRole);
                log.info("Migrated menus for existing role: {}", code);
            }
        }
    }

    private void initDefaultAdminUser() {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, "admin")
        );
        if (count == 0) {
            // Create admin user
            UserEntity admin = new UserEntity();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setNickname("System Administrator");
            admin.setStatus("ACTIVE");
            userMapper.insert(admin);

            // Find ADMIN role
            RoleEntity adminRole = roleMapper.selectOne(
                    new LambdaQueryWrapper<RoleEntity>().eq(RoleEntity::getCode, "ADMIN")
            );
            if (adminRole != null) {
                UserRoleEntity userRole = new UserRoleEntity();
                userRole.setUserId(admin.getId());
                userRole.setRoleId(adminRole.getId());
                userRoleMapper.insert(userRole);

                // Assign all permissions to ADMIN role
                List<PermissionEntity> allPermissions = permissionMapper.selectList(null);
                for (PermissionEntity perm : allPermissions) {
                    Long rpCount = rolePermissionMapper.selectCount(
                            new LambdaQueryWrapper<RolePermissionEntity>()
                                    .eq(RolePermissionEntity::getRoleId, adminRole.getId())
                                    .eq(RolePermissionEntity::getPermissionCode, perm.getCode())
                    );
                    if (rpCount == 0) {
                        RolePermissionEntity rp = new RolePermissionEntity();
                        rp.setRoleId(adminRole.getId());
                        rp.setPermissionCode(perm.getCode());
                        rolePermissionMapper.insert(rp);
                    }
                }
            }

            log.info("Created default admin user (admin/admin123)");
        }
    }

    private void initDefaultWorkspace() {
        // Ensure default workspace exists
        WorkspaceEntity defaultWs = workspaceMapper.selectOne(
                new LambdaQueryWrapper<WorkspaceEntity>().eq(WorkspaceEntity::getCode, "default"));
        if (defaultWs == null) {
            // Schema.sql creates it via MERGE, but just in case
            defaultWs = new WorkspaceEntity();
            defaultWs.setId(1L);
            defaultWs.setName("默认空间");
            defaultWs.setCode("default");
            defaultWs.setDescription("系统默认工作空间，包含所有历史数据");
            UserEntity admin = userMapper.selectOne(
                    new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, "admin"));
            defaultWs.setOwnerId(admin != null ? admin.getId() : 1L);
            workspaceMapper.insert(defaultWs);
            log.info("Created default workspace");
        }

        // Add all existing users to default workspace if not already members
        List<UserEntity> allUsers = userMapper.selectList(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getDeleted, 0));
        for (UserEntity user : allUsers) {
            Long memberCount = workspaceMemberMapper.selectCount(
                    new LambdaQueryWrapper<WorkspaceMemberEntity>()
                            .eq(WorkspaceMemberEntity::getWorkspaceId, defaultWs.getId())
                            .eq(WorkspaceMemberEntity::getUserId, user.getId()));
            if (memberCount == 0) {
                try {
                    WorkspaceMemberEntity member = new WorkspaceMemberEntity();
                    member.setWorkspaceId(defaultWs.getId());
                    member.setUserId(user.getId());
                    member.setRole("MEMBER");
                    workspaceMemberMapper.insert(member);
                    log.debug("Added user {} to default workspace", user.getUsername());
                } catch (Exception e) {
                    log.debug("User {} already in default workspace (unique constraint)", user.getUsername());
                }
            }
        }
    }
}
