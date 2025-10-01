package com.recplatform.solver.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recplatform.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Built-in solver example problem that can be loaded during problem creation.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("solver_example")
public class SolverExampleEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @com.baomidou.mybatisplus.annotation.TableField(value = "workspace_id", fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private Long workspaceId;

    /**
     * Stable business key, e.g. "transportation", "vrp".
     */
    private String exampleKey;

    private String name;

    /**
     * Problem type: LP, MILP, QP, etc.
     */
    private String problemType;

    private String description;

    private String recommendedAlgorithm;

    /**
     * easy, medium, or hard.
     */
    private String difficulty;

    /**
     * JSON-serialized problem definition (variables, constraints, objective).
     */
    private String problemDefinition;

    private Integer sortOrder;
}
