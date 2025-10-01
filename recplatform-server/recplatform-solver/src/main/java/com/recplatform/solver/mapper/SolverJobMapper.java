package com.recplatform.solver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recplatform.solver.model.entity.SolverJobEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for solver_job table.
 */
@Mapper
public interface SolverJobMapper extends BaseMapper<SolverJobEntity> {
}
