package com.recplatform.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recplatform.audit.entity.AuditLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Audit log mapper.
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLogEntity> {
}
