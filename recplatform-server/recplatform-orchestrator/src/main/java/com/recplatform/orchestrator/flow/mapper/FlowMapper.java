package com.recplatform.orchestrator.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recplatform.orchestrator.flow.FlowEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for orchestration flow entities.
 */
@Mapper
public interface FlowMapper extends BaseMapper<FlowEntity> {
}
