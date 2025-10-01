package com.recplatform.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recplatform.common.exception.BusinessException;
import com.recplatform.common.model.PageResult;
import com.recplatform.common.result.ResultCode;
import com.recplatform.iam.dto.RoleCreateRequest;
import com.recplatform.iam.dto.RoleUpdateRequest;
import com.recplatform.iam.entity.PermissionEntity;
import com.recplatform.iam.entity.RoleEntity;
import com.recplatform.iam.entity.RolePermissionEntity;
import com.recplatform.iam.mapper.PermissionMapper;
import com.recplatform.iam.mapper.RoleMapper;
import com.recplatform.iam.mapper.RolePermissionMapper;
import com.recplatform.iam.service.RoleService;
import com.recplatform.iam.vo.RoleVO;
import com.recplatform.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Role service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleVO createRole(RoleCreateRequest request) {
        // Check code uniqueness
        Long count = roleMapper.selectCount(
                new LambdaQueryWrapper<RoleEntity>().eq(RoleEntity::getCode, request.getCode())
        );
        if (count > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Role code already exists");
        }

        RoleEntity role = new RoleEntity();
        role.setCode(request.getCode());
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setStatus("ACTIVE");
        roleMapper.insert(role);

        return getRole(role.getId());
    }

    @Override
    public RoleVO getRole(Long id) {
        RoleEntity role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Role not found");
        }
        return toRoleVO(role);
    }

    @Override
    public PageResult<RoleVO> listRoles(String name, Integer page, Integer size) {
        LambdaQueryWrapper<RoleEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like(RoleEntity::getName, name);
        }
        wrapper.orderByDesc(RoleEntity::getCreateTime);

        Page<RoleEntity> pageResult = roleMapper.selectPage(
                new Page<>(page, size), wrapper
        );

        List<RoleVO> roleVOs = pageResult.getRecords().stream()
                .map(this::toRoleVO)
                .collect(Collectors.toList());

        return PageResult.<RoleVO>builder()
                .records(roleVOs)
                .total(pageResult.getTotal())
                .page(pageResult.getCurrent())
                .size(pageResult.getSize())
                .pages(pageResult.getPages())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleVO updateRole(Long id, RoleUpdateRequest request) {
        RoleEntity role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Role not found");
        }

        if (request.getName() != null) {
            role.setName(request.getName());
        }
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            role.setStatus(request.getStatus());
        }
        if (request.getMenus() != null) {
            role.setMenus(JsonUtil.toJson(request.getMenus()));
        }

        roleMapper.updateById(role);
        return getRole(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        RoleEntity role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Role not found");
        }
        roleMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, Set<String> permissionCodes) {
        // Remove existing permissions
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<RolePermissionEntity>().eq(RolePermissionEntity::getRoleId, roleId)
        );

        // Assign new permissions
        for (String code : permissionCodes) {
            RolePermissionEntity rp = new RolePermissionEntity();
            rp.setRoleId(roleId);
            rp.setPermissionCode(code);
            rolePermissionMapper.insert(rp);
        }
    }

    @Override
    public List<PermissionEntity> getRolePermissions(Long roleId) {
        List<RolePermissionEntity> rpList = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermissionEntity>().eq(RolePermissionEntity::getRoleId, roleId)
        );

        if (rpList.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> codes = rpList.stream()
                .map(RolePermissionEntity::getPermissionCode)
                .collect(Collectors.toList());

        return permissionMapper.selectList(
                new LambdaQueryWrapper<PermissionEntity>().in(PermissionEntity::getCode, codes)
        );
    }

    /**
     * Convert entity to VO.
     */
    private RoleVO toRoleVO(RoleEntity role) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setCode(role.getCode());
        vo.setName(role.getName());
        vo.setDescription(role.getDescription());
        vo.setStatus(role.getStatus());
        vo.setCreateTime(role.getCreateTime());
        vo.setUpdateTime(role.getUpdateTime());

        // Load permission codes
        List<RolePermissionEntity> rpList = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermissionEntity>().eq(RolePermissionEntity::getRoleId, role.getId())
        );
        vo.setPermissionCodes(rpList.stream()
                .map(RolePermissionEntity::getPermissionCode)
                .collect(Collectors.toList()));

        // Deserialize menus JSON array
        if (role.getMenus() != null && !role.getMenus().isBlank()) {
            try {
                vo.setMenus(JsonUtil.fromJson(role.getMenus(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}));
            } catch (Exception e) {
                log.warn("Failed to parse menus for role {}: {}", role.getId(), e.getMessage());
                vo.setMenus(Collections.emptyList());
            }
        } else {
            vo.setMenus(Collections.emptyList());
        }

        return vo;
    }
}
