package com.recplatform.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recplatform.audit.dto.AlertRuleCreateRequest;
import com.recplatform.audit.dto.AlertRuleUpdateRequest;
import com.recplatform.audit.dto.AuditEvent;
import com.recplatform.audit.entity.AlertRuleEntity;
import com.recplatform.audit.entity.AlertTriggerEntity;
import com.recplatform.audit.mapper.AlertRuleMapper;
import com.recplatform.audit.mapper.AlertTriggerMapper;
import com.recplatform.audit.service.AlertRuleService;
import com.recplatform.audit.vo.AlertVO;
import com.recplatform.common.exception.BusinessException;
import com.recplatform.common.model.PageResult;
import com.recplatform.common.result.ResultCode;
import com.recplatform.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Alert rule service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertRuleServiceImpl implements AlertRuleService {

    private final AlertRuleMapper alertRuleMapper;
    private final AlertTriggerMapper alertTriggerMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlertVO createRule(AlertRuleCreateRequest request) {
        AlertRuleEntity entity = new AlertRuleEntity();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setCondition(request.getCondition());
        entity.setSeverity(request.getSeverity() != null ? request.getSeverity() : "MEDIUM");
        entity.setEnabled(true);
        entity.setNotification(request.getNotification());
        alertRuleMapper.insert(entity);

        return toAlertRuleVO(entity);
    }

    @Override
    public PageResult<AlertVO> listRules(Integer page, Integer size) {
        Page<AlertRuleEntity> pageResult = alertRuleMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<AlertRuleEntity>().orderByDesc(AlertRuleEntity::getCreateTime)
        );

        List<AlertVO> vos = pageResult.getRecords().stream()
                .map(this::toAlertRuleVO)
                .collect(Collectors.toList());

        return PageResult.<AlertVO>builder()
                .records(vos)
                .total(pageResult.getTotal())
                .page(pageResult.getCurrent())
                .size(pageResult.getSize())
                .pages(pageResult.getPages())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlertVO updateRule(Long id, AlertRuleUpdateRequest request) {
        AlertRuleEntity entity = alertRuleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Alert rule not found");
        }

        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getCondition() != null) {
            entity.setCondition(request.getCondition());
        }
        if (request.getSeverity() != null) {
            entity.setSeverity(request.getSeverity());
        }
        if (request.getNotification() != null) {
            entity.setNotification(request.getNotification());
        }

        alertRuleMapper.updateById(entity);
        return toAlertRuleVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRule(Long id) {
        AlertRuleEntity entity = alertRuleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Alert rule not found");
        }
        alertRuleMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleRule(Long id, boolean enabled) {
        AlertRuleEntity entity = alertRuleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Alert rule not found");
        }
        entity.setEnabled(enabled);
        alertRuleMapper.updateById(entity);
    }

    @Override
    public void checkRules(AuditEvent event) {
        try {
            // Get all enabled rules
            List<AlertRuleEntity> enabledRules = alertRuleMapper.selectList(
                    new LambdaQueryWrapper<AlertRuleEntity>().eq(AlertRuleEntity::getEnabled, true)
            );

            for (AlertRuleEntity rule : enabledRules) {
                try {
                    if (matchesRule(event, rule)) {
                        triggerAlert(rule, event);
                    }
                } catch (Exception e) {
                    log.warn("Failed to check rule {}: {}", rule.getName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to check alert rules: {}", e.getMessage());
        }
    }

    @Override
    public PageResult<AlertVO> getTriggeredAlerts(Integer page, Integer size) {
        Page<AlertTriggerEntity> pageResult = alertTriggerMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<AlertTriggerEntity>()
                        .orderByDesc(AlertTriggerEntity::getCreateTime)
        );

        List<AlertVO> vos = pageResult.getRecords().stream()
                .map(this::toAlertVO)
                .collect(Collectors.toList());

        return PageResult.<AlertVO>builder()
                .records(vos)
                .total(pageResult.getTotal())
                .page(pageResult.getCurrent())
                .size(pageResult.getSize())
                .pages(pageResult.getPages())
                .build();
    }

    /**
     * Check if an audit event matches a rule's condition.
     */
    private boolean matchesRule(AuditEvent event, AlertRuleEntity rule) {
        if (!StringUtils.hasText(rule.getCondition())) {
            return false;
        }

        try {
            Map<String, Object> condition = JsonUtil.fromJson(rule.getCondition(), Map.class);
            if (condition == null) {
                return false;
            }

            // Check action match
            if (condition.containsKey("action")) {
                if (!condition.get("action").toString().equals(event.getAction())) {
                    return false;
                }
            }

            // Check resource match
            if (condition.containsKey("resource")) {
                if (!condition.get("resource").toString().equals(event.getResource())) {
                    return false;
                }
            }

            // Check result match
            if (condition.containsKey("result")) {
                if (!condition.get("result").toString().equals(event.getResult())) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.warn("Failed to parse rule condition: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Create an alert trigger when a rule is matched.
     */
    private void triggerAlert(AlertRuleEntity rule, AuditEvent event) {
        AlertTriggerEntity trigger = new AlertTriggerEntity();
        trigger.setRuleId(rule.getId());
        trigger.setRuleName(rule.getName());
        trigger.setSeverity(rule.getSeverity());
        trigger.setMatchedEvents(JsonUtil.toJson(Collections.singletonList(event.getResourceId())));
        trigger.setMessage(String.format("Alert '%s' triggered by %s on %s",
                rule.getName(), event.getAction(), event.getResource()));
        trigger.setAcknowledged(false);
        trigger.setCreateTime(LocalDateTime.now());
        alertTriggerMapper.insert(trigger);

        log.warn("Alert triggered: {} - {}", rule.getName(), trigger.getMessage());
    }

    private AlertVO toAlertRuleVO(AlertRuleEntity entity) {
        AlertVO vo = new AlertVO();
        vo.setId(entity.getId());
        vo.setRuleName(entity.getName());
        vo.setSeverity(entity.getSeverity());
        return vo;
    }

    private AlertVO toAlertVO(AlertTriggerEntity entity) {
        AlertVO vo = new AlertVO();
        vo.setId(entity.getId());
        vo.setRuleId(entity.getRuleId());
        vo.setRuleName(entity.getRuleName());
        vo.setSeverity(entity.getSeverity());
        vo.setMatchedEvents(entity.getMatchedEvents());
        vo.setMessage(entity.getMessage());
        vo.setAcknowledged(entity.getAcknowledged());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
