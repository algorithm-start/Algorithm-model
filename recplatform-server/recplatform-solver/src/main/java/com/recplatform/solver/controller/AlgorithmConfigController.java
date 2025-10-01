package com.recplatform.solver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recplatform.common.exception.BusinessException;
import com.recplatform.common.result.Result;
import com.recplatform.common.result.ResultCode;
import com.recplatform.common.util.JsonUtil;
import com.recplatform.solver.mapper.AlgorithmConfigMapper;
import com.recplatform.solver.mapper.CustomAlgorithmMapper;
import com.recplatform.solver.model.entity.AlgorithmConfigEntity;
import com.recplatform.solver.model.entity.CustomAlgorithmEntity;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/solver")
@RequiredArgsConstructor
public class AlgorithmConfigController {

    private final AlgorithmConfigMapper algorithmConfigMapper;
    private final CustomAlgorithmMapper customAlgorithmMapper;

    /**
     * VIEWER (read-only) users may browse algorithms but must not modify any
     * algorithm configuration or register/delete custom algorithms. A user is
     * treated as read-only when they hold the VIEWER role and do not also hold
     * a writable role (ADMIN or USER). This mirrors the frontend gating and
     * prevents bypassing the UI by calling the API directly.
     */
    private void assertCanEditAlgorithm() {
        List<String> roles = StpUtil.getRoleList();
        boolean hasViewer = roles.contains("VIEWER");
        boolean hasWriteRole = roles.contains("ADMIN") || roles.contains("USER");
        if (hasViewer && !hasWriteRole) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只读用户无权修改算法配置");
        }
    }

    // ==================== Algorithm Config ====================

    @GetMapping("/algorithm-configs/{code}")
    public Result<Map<String, Object>> getConfig(@PathVariable String code) {
        LambdaQueryWrapper<AlgorithmConfigEntity> wrapper = new LambdaQueryWrapper<AlgorithmConfigEntity>()
                .eq(AlgorithmConfigEntity::getAlgorithmCode, code)
                .eq(AlgorithmConfigEntity::getDeleted, 0)
                .orderByDesc(AlgorithmConfigEntity::getUpdateTime)
                .last("LIMIT 1");
        AlgorithmConfigEntity entity = algorithmConfigMapper.selectOne(wrapper);
        if (entity == null) {
            return Result.success(null);
        }
        Map<String, Object> configData = JsonUtil.fromJson(entity.getConfigData(),
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        return Result.success(configData);
    }

    @PostMapping("/algorithm-configs/{code}")
    public Result<Void> saveConfig(@PathVariable String code, @RequestBody Map<String, Object> configData) {
        assertCanEditAlgorithm();
        LambdaQueryWrapper<AlgorithmConfigEntity> wrapper = new LambdaQueryWrapper<AlgorithmConfigEntity>()
                .eq(AlgorithmConfigEntity::getAlgorithmCode, code)
                .eq(AlgorithmConfigEntity::getDeleted, 0);
        AlgorithmConfigEntity existing = algorithmConfigMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setConfigData(JsonUtil.toJson(configData));
            existing.setUpdateTime(LocalDateTime.now());
            algorithmConfigMapper.updateById(existing);
        } else {
            AlgorithmConfigEntity entity = new AlgorithmConfigEntity();
            entity.setAlgorithmCode(code);
            entity.setConfigData(JsonUtil.toJson(configData));
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());
            entity.setDeleted(0);
            Long wsId = com.recplatform.common.workspace.WorkspaceContextHolder.getWorkspaceId();
            entity.setWorkspaceId(wsId != null ? wsId : 1L);
            algorithmConfigMapper.insert(entity);
        }
        log.info("Saved algorithm config for code={}", code);
        return Result.success(null);
    }

    // ==================== Custom Algorithms ====================

    @GetMapping("/custom-algorithms")
    public Result<List<CustomAlgorithmEntity>> listCustomAlgorithms() {
        LambdaQueryWrapper<CustomAlgorithmEntity> wrapper = new LambdaQueryWrapper<CustomAlgorithmEntity>()
                .eq(CustomAlgorithmEntity::getDeleted, 0)
                .orderByDesc(CustomAlgorithmEntity::getCreateTime);
        return Result.success(customAlgorithmMapper.selectList(wrapper));
    }

    @PostMapping("/custom-algorithms")
    public Result<CustomAlgorithmEntity> registerCustomAlgorithm(@RequestBody CustomAlgorithmRequest request) {
        assertCanEditAlgorithm();
        CustomAlgorithmEntity entity = new CustomAlgorithmEntity();
        entity.setName(request.getName());
        entity.setCode(request.getCode().toUpperCase());
        entity.setCategory("CUSTOM");
        entity.setDescription(request.getDescription());
        entity.setProblemTypes(String.join(",", request.getProblemTypes()));
        entity.setIntegrationType(request.getIntegrationType());
        entity.setApiEndpoint(request.getApiEndpoint());
        entity.setScriptContent(request.getScriptContent());
        entity.setParams(request.getParams() != null ? JsonUtil.toJson(request.getParams()) : null);
        entity.setEnabled(1);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        entity.setDeleted(0);
        Long wsId = com.recplatform.common.workspace.WorkspaceContextHolder.getWorkspaceId();
        entity.setWorkspaceId(wsId != null ? wsId : 1L);
        customAlgorithmMapper.insert(entity);
        log.info("Registered custom algorithm: name={}, code={}", entity.getName(), entity.getCode());
        return Result.success(entity);
    }

    @DeleteMapping("/custom-algorithms/{id}")
    public Result<Void> deleteCustomAlgorithm(@PathVariable Long id) {
        assertCanEditAlgorithm();
        CustomAlgorithmEntity entity = customAlgorithmMapper.selectById(id);
        if (entity != null) {
            entity.setDeleted(1);
            entity.setUpdateTime(LocalDateTime.now());
            customAlgorithmMapper.updateById(entity);
        }
        return Result.success(null);
    }

    @Data
    public static class CustomAlgorithmRequest {
        private String name;
        private String code;
        private String description;
        private List<String> problemTypes;
        private String integrationType;
        private String apiEndpoint;
        private String scriptContent;
        private Map<String, String> params;
    }
}
