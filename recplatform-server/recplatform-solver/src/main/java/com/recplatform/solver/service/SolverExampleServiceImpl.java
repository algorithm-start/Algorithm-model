package com.recplatform.solver.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recplatform.common.exception.BusinessException;
import com.recplatform.common.result.ResultCode;
import com.recplatform.common.util.JsonUtil;
import com.recplatform.solver.mapper.SolverExampleMapper;
import com.recplatform.solver.model.dto.ProblemDefinition;
import com.recplatform.solver.model.entity.SolverExampleEntity;
import com.recplatform.solver.model.vo.SolverExampleDetailVO;
import com.recplatform.solver.model.vo.SolverExampleSummaryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for built-in solver example problems.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SolverExampleServiceImpl implements SolverExampleService {

    private final SolverExampleMapper solverExampleMapper;

    @Override
    public List<SolverExampleSummaryVO> listExamples() {
        LambdaQueryWrapper<SolverExampleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SolverExampleEntity::getSortOrder);
        List<SolverExampleEntity> entities = solverExampleMapper.selectList(wrapper);
        return entities.stream().map(this::toSummary).collect(Collectors.toList());
    }

    @Override
    public SolverExampleDetailVO getExample(String exampleKey) {
        LambdaQueryWrapper<SolverExampleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SolverExampleEntity::getExampleKey, exampleKey);
        SolverExampleEntity entity = solverExampleMapper.selectOne(wrapper);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Example not found: " + exampleKey);
        }
        return toDetail(entity);
    }

    private SolverExampleSummaryVO toSummary(SolverExampleEntity entity) {
        return SolverExampleSummaryVO.builder()
                .id(entity.getExampleKey())
                .name(entity.getName())
                .type(entity.getProblemType())
                .description(entity.getDescription())
                .recommendedAlgorithm(entity.getRecommendedAlgorithm())
                .difficulty(entity.getDifficulty())
                .build();
    }

    private SolverExampleDetailVO toDetail(SolverExampleEntity entity) {
        ProblemDefinition definition = JsonUtil.fromJson(entity.getProblemDefinition(), ProblemDefinition.class);
        SolverExampleDetailVO.ProblemPayload payload = SolverExampleDetailVO.ProblemPayload.builder()
                .problemName(entity.getName())
                .description(entity.getDescription())
                .problemDefinition(definition)
                .build();
        return SolverExampleDetailVO.builder()
                .id(entity.getExampleKey())
                .name(entity.getName())
                .type(entity.getProblemType())
                .description(entity.getDescription())
                .recommendedAlgorithm(entity.getRecommendedAlgorithm())
                .difficulty(entity.getDifficulty())
                .problem(payload)
                .build();
    }
}
