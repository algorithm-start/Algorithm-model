package com.recplatform.solver.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recplatform.common.enums.AlgorithmType;
import com.recplatform.common.enums.SolverStatus;
import com.recplatform.common.exception.BusinessException;
import com.recplatform.common.model.PageRequest;
import com.recplatform.common.model.PageResult;
import com.recplatform.common.result.ResultCode;
import com.recplatform.common.util.JsonUtil;
import com.recplatform.solver.config.SolverProperties;
import com.recplatform.solver.mapper.SolverJobMapper;
import com.recplatform.solver.model.dto.AlgorithmInfo;
import com.recplatform.solver.model.dto.AlgorithmRecommendation;
import com.recplatform.solver.model.dto.ProblemDefinition;
import com.recplatform.solver.model.dto.ProblemSubmitRequest;
import com.recplatform.solver.model.dto.SolverConfig;
import com.recplatform.solver.model.entity.SolverJobEntity;
import com.recplatform.solver.model.vo.SolverJobVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of the solver service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SolverServiceImpl implements SolverService {

    private final SolverJobMapper solverJobMapper;
    private final AlgorithmRegistry algorithmRegistry;
    private final SolverEngineClient solverEngineClient;
    private final SolverProperties solverProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SolverJobVO submitProblem(ProblemSubmitRequest request) {
        log.info("Submitting problem: {}", request.getProblemName());

        SolverJobEntity entity = new SolverJobEntity();
        entity.setProblemName(request.getProblemName());
        entity.setProblemDescription(request.getDescription());
        entity.setProblemDefinition(JsonUtil.toJson(request.getProblemDefinition()));
        entity.setStatus(SolverStatus.PENDING);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        entity.setDeleted(0);
        // Set workspace ID from context, default to 1
        Long wsId = com.recplatform.common.workspace.WorkspaceContextHolder.getWorkspaceId();
        entity.setWorkspaceId(wsId != null ? wsId : 1L);

        solverJobMapper.insert(entity);
        log.info("Problem submitted with id={}", entity.getId());

        return toVO(entity);
    }

    @Override
    public SolverJobVO getProblem(Long id) {
        SolverJobEntity entity = getById(id);
        return toVO(entity);
    }

    @Override
    public SolverJobVO solveProblem(Long id, SolverConfig config) {
        SolverJobEntity entity = getById(id);

        if (entity.getStatus() == SolverStatus.RUNNING) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Problem is already being solved");
        }

        // Update solver config
        entity.setSolverConfig(JsonUtil.toJson(config));
        entity.setAlgorithmType(config != null ? config.getAlgorithmType() : null);
        entity.setStatus(SolverStatus.RUNNING);
        entity.setUpdateTime(LocalDateTime.now());
        entity.setErrorMessage(null);
        solverJobMapper.updateById(entity);

        try {
            // Build request to Python engine
            SolverEngineClient.SolverRequest engineRequest = buildEngineRequest(entity, config);

            // Call Python solver engine
            SolverEngineClient.SolverResponse engineResponse = solverEngineClient.submitSolve(engineRequest);

            // Map engine response to entity
            mapEngineResponse(entity, engineResponse);
        } catch (Exception e) {
            log.error("Error solving problem id={}: {}", id, e.getMessage(), e);
            entity.setStatus(SolverStatus.ERROR);
            String errMsg = "Solve failed: " + e.getMessage();
            entity.setErrorMessage(errMsg.length() > 1900 ? errMsg.substring(0, 1900) : errMsg);
        }

        entity.setUpdateTime(LocalDateTime.now());
        solverJobMapper.updateById(entity);

        return toVO(entity);
    }

    @Override
    public List<AlgorithmInfo> listAlgorithms() {
        return algorithmRegistry.getAlgorithms();
    }

    @Override
    public AlgorithmRecommendation autoSelectAlgorithm(ProblemDefinition problemDefinition) {
        return algorithmRegistry.recommend(problemDefinition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SolverJobVO updateParams(Long id, SolverConfig config) {
        SolverJobEntity entity = getById(id);

        if (entity.getStatus() == SolverStatus.RUNNING) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Cannot update params while problem is being solved");
        }

        entity.setSolverConfig(JsonUtil.toJson(config));
        entity.setAlgorithmType(config != null ? config.getAlgorithmType() : null);
        entity.setUpdateTime(LocalDateTime.now());
        solverJobMapper.updateById(entity);

        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelProblem(Long id) {
        SolverJobEntity entity = getById(id);

        if (entity.getStatus() == SolverStatus.RUNNING) {
            // Cancel on the engine side
            solverEngineClient.cancel(String.valueOf(id));
        }
        // Logical delete via MyBatis-Plus @TableLogic
        solverJobMapper.deleteById(id);
        log.info("Problem id={} deleted", id);
    }

    @Override
    public PageResult<SolverJobVO> listProblems(PageRequest pageRequest) {
        Page<SolverJobEntity> page = new Page<>(
                pageRequest.getPage() != null ? pageRequest.getPage() : 1,
                pageRequest.getSize() != null ? pageRequest.getSize() : 20
        );

        LambdaQueryWrapper<SolverJobEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SolverJobEntity::getCreateTime);

        IPage<SolverJobEntity> result = solverJobMapper.selectPage(page, wrapper);

        List<SolverJobVO> voList = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        PageResult<SolverJobVO> pageResult = PageResult.<SolverJobVO>builder()
                .records(voList)
                .total(result.getTotal())
                .page(result.getCurrent())
                .size(result.getSize())
                .pages(result.getPages())
                .build();
        return pageResult;
    }

    // --- Private helper methods ---

    private static void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private SolverJobEntity getById(Long id) {
        SolverJobEntity entity = solverJobMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Solver job not found: " + id);
        }
        return entity;
    }

    @SuppressWarnings("unchecked")
    private SolverEngineClient.SolverRequest buildEngineRequest(SolverJobEntity entity, SolverConfig config) {
        SolverEngineClient.SolverRequest request = new SolverEngineClient.SolverRequest();

        // Parse problem definition and convert to Python engine format
        Map<String, Object> rawDef = JsonUtil.fromJson(
                entity.getProblemDefinition(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        Map<String, Object> problemMap = new HashMap<>();
        problemMap.put("name", entity.getProblemName());

        if (rawDef != null) {
            // Convert variables: type->var_type(lowercase), camelCase->snake_case
            List<Map<String, Object>> rawVars = (List<Map<String, Object>>) rawDef.get("variables");
            if (rawVars != null) {
                List<Map<String, Object>> engineVars = new java.util.ArrayList<>();
                for (Map<String, Object> v : rawVars) {
                    Map<String, Object> ev = new HashMap<>();
                    ev.put("name", v.getOrDefault("name", "x"));
                    String varType = String.valueOf(v.getOrDefault("type", v.getOrDefault("var_type", "continuous")));
                    ev.put("var_type", varType.toLowerCase());
                    ev.put("lower_bound", v.getOrDefault("lowerBound", v.getOrDefault("lower_bound", 0.0)));
                    Object ub = v.getOrDefault("upperBound", v.getOrDefault("upper_bound", null));
                    ev.put("upper_bound", ub != null ? ub : 1e30);
                    engineVars.add(ev);
                }
                problemMap.put("variables", engineVars);
            }

            // Convert constraints: type+rhs -> lower_bound/upper_bound
            List<Map<String, Object>> rawCons = (List<Map<String, Object>>) rawDef.get("constraints");
            if (rawCons != null) {
                List<Map<String, Object>> engineCons = new java.util.ArrayList<>();
                int idx = 0;
                for (Map<String, Object> c : rawCons) {
                    Map<String, Object> ec = new HashMap<>();
                    ec.put("name", c.getOrDefault("name", "c" + idx));
                    ec.put("expression", c.getOrDefault("expression", ""));
                    String conType = String.valueOf(c.getOrDefault("type", c.getOrDefault("sense", "LEQ"))).toUpperCase();
                    Object rhs = c.getOrDefault("rhs", c.getOrDefault("upper_bound", null));
                    Double rhsVal = rhs != null ? Double.valueOf(rhs.toString()) : null;
                    if ("LEQ".equals(conType) || "<=".equals(c.get("sense"))) {
                        ec.put("upper_bound", rhsVal);
                        ec.put("lower_bound", null);
                    } else if ("GEQ".equals(conType) || ">=".equals(c.get("sense"))) {
                        ec.put("lower_bound", rhsVal);
                        ec.put("upper_bound", null);
                    } else {
                        // EQ
                        ec.put("lower_bound", rhsVal);
                        ec.put("upper_bound", rhsVal);
                    }
                    engineCons.add(ec);
                    idx++;
                }
                problemMap.put("constraints", engineCons);
            }

            // Convert objective: sense to lowercase
            Object rawObj = rawDef.get("objective");
            if (rawObj instanceof Map) {
                Map<String, Object> objMap = (Map<String, Object>) rawObj;
                Map<String, Object> engineObj = new HashMap<>();
                engineObj.put("expression", objMap.getOrDefault("expression", "0"));
                String sense = String.valueOf(objMap.getOrDefault("sense", "minimize")).toLowerCase();
                engineObj.put("sense", sense);
                problemMap.put("objective", engineObj);
            }

            // Pass through metadata if present
            if (rawDef.containsKey("metadata")) {
                problemMap.put("metadata", rawDef.get("metadata"));
            }
        }

        request.setProblem(problemMap);

        // Build config map. Only put non-null values so the Python engine falls
        // back to its own defaults for unset fields. The engine's SolverConfig
        // declares gap_tolerance/threads as non-optional floats/ints, so sending
        // an explicit null would fail Pydantic validation with a 422.
        Map<String, Object> configMap = new HashMap<>();
        if (config != null) {
            configMap.put("algorithm", config.getAlgorithmType() != null ? config.getAlgorithmType().getCode() : "auto");
            configMap.put("time_limit", config.getTimeLimit() != null ? config.getTimeLimit() : solverProperties.getDefaultTimeout());
            putIfNotNull(configMap, "gap_tolerance", config.getGapTolerance());
            putIfNotNull(configMap, "max_iterations", config.getMaxIterations());
            putIfNotNull(configMap, "threads", config.getThreads());
            if (config.getCustomParams() != null) {
                configMap.putAll(config.getCustomParams());
            }
        } else {
            configMap.put("algorithm", "auto");
            configMap.put("time_limit", solverProperties.getDefaultTimeout());
        }
        request.setConfig(configMap);

        return request;
    }

    private void mapEngineResponse(SolverJobEntity entity, SolverEngineClient.SolverResponse response) {
        String status = response.getStatus();
        if ("optimal".equalsIgnoreCase(status)) {
            entity.setStatus(SolverStatus.OPTIMAL);
        } else if ("feasible".equalsIgnoreCase(status)) {
            entity.setStatus(SolverStatus.FEASIBLE);
        } else if ("infeasible".equalsIgnoreCase(status)) {
            entity.setStatus(SolverStatus.INFEASIBLE);
        } else if ("unbounded".equalsIgnoreCase(status)) {
            entity.setStatus(SolverStatus.UNBOUNDED);
        } else if ("timeout".equalsIgnoreCase(status)) {
            entity.setStatus(SolverStatus.TIMEOUT);
        } else if ("cancelled".equalsIgnoreCase(status)) {
            entity.setStatus(SolverStatus.CANCELLED);
        } else {
            entity.setStatus(SolverStatus.ERROR);
        }

        entity.setObjectiveValue(response.getObjectiveValue());
        entity.setResult(JsonUtil.toJson(response));
        if (response.getSolveTime() != null) {
            entity.setSolveTimeMs((long) (response.getSolveTime() * 1000));
        }
        if (response.getErrorMessage() != null) {
            entity.setErrorMessage(response.getErrorMessage());
        }
    }

    private SolverJobVO toVO(SolverJobEntity entity) {
        SolverJobVO vo = new SolverJobVO();
        vo.setId(entity.getId());
        vo.setProblemName(entity.getProblemName());
        vo.setProblemDescription(entity.getProblemDescription());
        vo.setProblemDefinition(entity.getProblemDefinition());
        vo.setAlgorithmType(entity.getAlgorithmType());
        vo.setSolverConfig(entity.getSolverConfig());
        vo.setStatus(entity.getStatus());
        vo.setResult(entity.getResult());
        vo.setObjectiveValue(entity.getObjectiveValue());
        vo.setSolveTimeMs(entity.getSolveTimeMs());
        vo.setErrorMessage(entity.getErrorMessage());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        vo.setCreateBy(entity.getCreateBy());

        // Formatted fields
        if (entity.getSolveTimeMs() != null) {
            vo.setSolveTimeFormatted(String.format("%.2fs", entity.getSolveTimeMs() / 1000.0));
        }
        if (entity.getStatus() != null) {
            vo.setStatusDescription(formatStatus(entity.getStatus()));
        }

        return vo;
    }

    private String formatStatus(SolverStatus status) {
        return switch (status) {
            case PENDING -> "Pending - waiting to be solved";
            case QUEUED -> "Queued - waiting for available slot";
            case RUNNING -> "Running - solver is processing";
            case OPTIMAL -> "Optimal solution found";
            case FEASIBLE -> "Feasible solution found (may not be optimal)";
            case INFEASIBLE -> "Problem is infeasible - no solution exists";
            case UNBOUNDED -> "Problem is unbounded - objective can improve infinitely";
            case TIMEOUT -> "Solver timed out before finding solution";
            case ERROR -> "Error occurred during solving";
            case CANCELLED -> "Solve was cancelled by user";
        };
    }
}
