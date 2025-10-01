package com.recplatform.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recplatform.audit.dto.AuditEvent;
import com.recplatform.audit.dto.AuditSearchRequest;
import com.recplatform.audit.entity.AuditLogEntity;
import com.recplatform.audit.mapper.AuditLogMapper;
import com.recplatform.audit.service.AuditLogService;
import com.recplatform.audit.service.AlertRuleService;
import com.recplatform.audit.vo.AuditLogVO;
import com.recplatform.audit.vo.AuditStats;
import com.recplatform.common.model.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Audit log service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;
    private final AlertRuleService alertRuleService;

    private static final DateTimeFormatter CSV_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void log(AuditEvent event) {
        try {
            AuditLogEntity entity = new AuditLogEntity();
            entity.setUserId(event.getUserId());
            entity.setUsername(event.getUsername());
            entity.setAction(event.getAction());
            entity.setResource(event.getResource());
            entity.setResourceId(event.getResourceId());
            entity.setDetail(event.getDetail());
            entity.setResult(event.getResult());
            entity.setErrorMessage(event.getErrorMessage());
            entity.setIp(event.getIp());
            entity.setUserAgent(event.getUserAgent());
            entity.setDuration(event.getDuration());
            entity.setTraceId(event.getTraceId());
            auditLogMapper.insert(entity);

            // Check alert rules asynchronously
            try {
                alertRuleService.checkRules(event);
            } catch (Exception e) {
                log.warn("Failed to check alert rules for event: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage(), e);
        }
    }

    @Override
    public PageResult<AuditLogVO> search(AuditSearchRequest request) {
        LambdaQueryWrapper<AuditLogEntity> wrapper = buildSearchWrapper(request);
        wrapper.orderByDesc(AuditLogEntity::getCreateTime);

        Page<AuditLogEntity> pageResult = auditLogMapper.selectPage(
                new Page<>(request.getPage(), request.getSize()), wrapper
        );

        List<AuditLogVO> vos = pageResult.getRecords().stream()
                .map(this::toAuditLogVO)
                .collect(Collectors.toList());

        return PageResult.<AuditLogVO>builder()
                .records(vos)
                .total(pageResult.getTotal())
                .page(pageResult.getCurrent())
                .size(pageResult.getSize())
                .pages(pageResult.getPages())
                .build();
    }

    @Override
    public AuditStats getStats(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<AuditLogEntity> wrapper = new LambdaQueryWrapper<>();
        if (startTime != null) {
            wrapper.ge(AuditLogEntity::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(AuditLogEntity::getCreateTime, endTime);
        }

        List<AuditLogEntity> logs = auditLogMapper.selectList(wrapper);

        long totalCount = logs.size();
        Map<String, Long> countByAction = logs.stream()
                .filter(l -> l.getAction() != null)
                .collect(Collectors.groupingBy(AuditLogEntity::getAction, Collectors.counting()));
        Map<String, Long> countByResource = logs.stream()
                .filter(l -> l.getResource() != null)
                .collect(Collectors.groupingBy(AuditLogEntity::getResource, Collectors.counting()));
        Map<String, Long> countByResult = logs.stream()
                .filter(l -> l.getResult() != null)
                .collect(Collectors.groupingBy(AuditLogEntity::getResult, Collectors.counting()));

        // Build timeline (daily counts)
        DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, Long> dailyCounts = logs.stream()
                .filter(l -> l.getCreateTime() != null)
                .collect(Collectors.groupingBy(
                        l -> l.getCreateTime().format(dayFormat),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        List<AuditStats.DailyCount> timeline = dailyCounts.entrySet().stream()
                .map(e -> AuditStats.DailyCount.builder()
                        .date(e.getKey())
                        .count(e.getValue())
                        .build())
                .collect(Collectors.toList());

        return AuditStats.builder()
                .totalCount(totalCount)
                .countByAction(countByAction)
                .countByResource(countByResource)
                .countByResult(countByResult)
                .timeline(timeline)
                .build();
    }

    @Override
    public byte[] export(AuditSearchRequest request) {
        LambdaQueryWrapper<AuditLogEntity> wrapper = buildSearchWrapper(request);
        wrapper.orderByDesc(AuditLogEntity::getCreateTime);

        List<AuditLogEntity> logs = auditLogMapper.selectList(wrapper);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {

            // BOM for Excel
            writer.write('\uFEFF');

            // Header
            writer.write("ID,User ID,Username,Action,Resource,Resource ID,Result,Error Message,IP,Duration(ms),Trace ID,Create Time\n");

            // Data rows
            for (AuditLogEntity logEntry : logs) {
                writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                        escapeCsv(String.valueOf(logEntry.getId())),
                        escapeCsv(String.valueOf(logEntry.getUserId())),
                        escapeCsv(logEntry.getUsername()),
                        escapeCsv(logEntry.getAction()),
                        escapeCsv(logEntry.getResource()),
                        escapeCsv(logEntry.getResourceId()),
                        escapeCsv(logEntry.getResult()),
                        escapeCsv(logEntry.getErrorMessage()),
                        escapeCsv(logEntry.getIp()),
                        logEntry.getDuration() != null ? String.valueOf(logEntry.getDuration()) : "",
                        escapeCsv(logEntry.getTraceId()),
                        logEntry.getCreateTime() != null ? logEntry.getCreateTime().format(CSV_DATE_FORMAT) : ""
                ));
            }

            writer.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to export audit logs", e);
            throw new RuntimeException("Failed to export audit logs", e);
        }
    }

    private LambdaQueryWrapper<AuditLogEntity> buildSearchWrapper(AuditSearchRequest request) {
        LambdaQueryWrapper<AuditLogEntity> wrapper = new LambdaQueryWrapper<>();
        if (request.getUserId() != null) {
            wrapper.eq(AuditLogEntity::getUserId, request.getUserId());
        }
        if (StringUtils.hasText(request.getUsername())) {
            wrapper.like(AuditLogEntity::getUsername, request.getUsername());
        }
        if (StringUtils.hasText(request.getAction())) {
            wrapper.eq(AuditLogEntity::getAction, request.getAction());
        }
        if (StringUtils.hasText(request.getResource())) {
            wrapper.eq(AuditLogEntity::getResource, request.getResource());
        }
        if (request.getStartTime() != null) {
            wrapper.ge(AuditLogEntity::getCreateTime, request.getStartTime());
        }
        if (request.getEndTime() != null) {
            wrapper.le(AuditLogEntity::getCreateTime, request.getEndTime());
        }
        if (StringUtils.hasText(request.getResult())) {
            wrapper.eq(AuditLogEntity::getResult, request.getResult());
        }
        return wrapper;
    }

    private AuditLogVO toAuditLogVO(AuditLogEntity entity) {
        AuditLogVO vo = new AuditLogVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setUsername(entity.getUsername());
        vo.setAction(entity.getAction());
        vo.setResource(entity.getResource());
        vo.setResourceId(entity.getResourceId());
        vo.setDetail(entity.getDetail());
        vo.setResult(entity.getResult());
        vo.setErrorMessage(entity.getErrorMessage());
        vo.setIp(entity.getIp());
        vo.setUserAgent(entity.getUserAgent());
        vo.setDuration(entity.getDuration());
        vo.setTraceId(entity.getTraceId());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
