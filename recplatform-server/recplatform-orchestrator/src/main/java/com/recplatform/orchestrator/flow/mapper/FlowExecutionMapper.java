package com.recplatform.orchestrator.flow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recplatform.orchestrator.flow.FlowExecutionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for flow execution entities.
 */
@Mapper
public interface FlowExecutionMapper extends BaseMapper<FlowExecutionEntity> {
}
