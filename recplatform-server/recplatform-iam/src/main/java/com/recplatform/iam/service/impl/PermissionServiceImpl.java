package com.recplatform.iam.service.impl;

import com.recplatform.iam.entity.PermissionEntity;
import com.recplatform.iam.mapper.PermissionMapper;
import com.recplatform.iam.service.PermissionService;
import com.recplatform.iam.vo.PermissionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Permission service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionMapper permissionMapper;

    @Override
    public List<PermissionVO> listAll() {
        List<PermissionEntity> permissions = permissionMapper.selectList(null);
        return permissions.stream()
                .map(this::toPermissionVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionVO> getPermissionTree() {
        List<PermissionEntity> permissions = permissionMapper.selectList(null);
        List<PermissionVO> allVOs = permissions.stream()
                .map(this::toPermissionVO)
                .collect(Collectors.toList());

        // Group by module
        Map<String, List<PermissionVO>> moduleMap = allVOs.stream()
                .collect(Collectors.groupingBy(PermissionVO::getModule));

        // Build tree with modules as root nodes
        List<PermissionVO> tree = new ArrayList<>();
        moduleMap.forEach((module, modulePermissions) -> {
            PermissionVO moduleNode = new PermissionVO();
            moduleNode.setCode(module);
            moduleNode.setName(module);
            moduleNode.setModule(module);
            moduleNode.setType("MENU");
            moduleNode.setChildren(modulePermissions);
            tree.add(moduleNode);
        });

        return tree;
    }

    private PermissionVO toPermissionVO(PermissionEntity entity) {
        PermissionVO vo = new PermissionVO();
        vo.setId(entity.getId());
        vo.setCode(entity.getCode());
        vo.setName(entity.getName());
        vo.setModule(entity.getModule());
        vo.setDescription(entity.getDescription());
        vo.setType(entity.getType());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
