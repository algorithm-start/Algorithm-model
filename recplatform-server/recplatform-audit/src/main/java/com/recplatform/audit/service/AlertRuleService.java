package com.recplatform.audit.service;

import com.recplatform.audit.dto.AlertRuleCreateRequest;
import com.recplatform.audit.dto.AlertRuleUpdateRequest;
import com.recplatform.audit.dto.AuditEvent;
import com.recplatform.audit.vo.AlertVO;
import com.recplatform.common.model.PageResult;

/**
 * Alert rule service.
 */
public interface AlertRuleService {

    /**
     * Create an alert rule.
     */
    AlertVO createRule(AlertRuleCreateRequest request);

    /**
     * List alert rules with pagination.
     */
    PageResult<AlertVO> listRules(Integer page, Integer size);

    /**
     * Update an alert rule.
     */
    AlertVO updateRule(Long id, AlertRuleUpdateRequest request);

    /**
     * Delete an alert rule.
     */
    void deleteRule(Long id);

    /**
     * Enable or disable an alert rule.
     */
    void toggleRule(Long id, boolean enabled);

    /**
     * Evaluate if an event triggers any rules.
     */
    void checkRules(AuditEvent event);

    /**
     * Get triggered alerts with pagination.
     */
    PageResult<AlertVO> getTriggeredAlerts(Integer page, Integer size);
}
