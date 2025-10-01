package com.recplatform.audit.controller;

import com.recplatform.common.model.PageResult;
import com.recplatform.common.result.Result;
import com.recplatform.audit.dto.AlertRuleCreateRequest;
import com.recplatform.audit.dto.AlertRuleUpdateRequest;
import com.recplatform.audit.service.AlertRuleService;
import com.recplatform.audit.vo.AlertVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Alert rule controller.
 */
@RestController
@RequestMapping("/api/v1/audit/alerts")
@RequiredArgsConstructor
@Tag(name = "Alert Rules", description = "Alert rule management APIs")
public class AlertRuleController {

    private final AlertRuleService alertRuleService;

    @PostMapping
    @Operation(summary = "Create alert rule")
    public Result<AlertVO> createRule(@Valid @RequestBody AlertRuleCreateRequest request) {
        return Result.success(alertRuleService.createRule(request));
    }

    @GetMapping
    @Operation(summary = "List alert rules")
    public Result<PageResult<AlertVO>> listRules(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(alertRuleService.listRules(page, size));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update alert rule")
    public Result<AlertVO> updateRule(@PathVariable Long id, @RequestBody AlertRuleUpdateRequest request) {
        return Result.success(alertRuleService.updateRule(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete alert rule")
    public Result<Void> deleteRule(@PathVariable Long id) {
        alertRuleService.deleteRule(id);
        return Result.success();
    }

    @PutMapping("/{id}/enable")
    @Operation(summary = "Enable/disable alert rule")
    public Result<Void> toggleRule(@PathVariable Long id, @RequestParam boolean enabled) {
        alertRuleService.toggleRule(id, enabled);
        return Result.success();
    }

    @GetMapping("/triggered")
    @Operation(summary = "List triggered alerts")
    public Result<PageResult<AlertVO>> getTriggeredAlerts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(alertRuleService.getTriggeredAlerts(page, size));
    }
}
