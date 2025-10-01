-- Solver tables
CREATE TABLE IF NOT EXISTS solver_job (
    id BIGINT PRIMARY KEY,
    problem_name VARCHAR(200) NOT NULL,
    problem_description CLOB,
    problem_definition CLOB,
    algorithm_type VARCHAR(50),
    solver_config CLOB,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    result CLOB,
    objective_value DOUBLE,
    solve_time_ms BIGINT,
    error_message CLOB,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    create_by VARCHAR(100),
    update_by VARCHAR(100)
);

-- Custom algorithm registration table
CREATE TABLE IF NOT EXISTS solver_custom_algorithm (
    id BIGINT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(100) NOT NULL,
    category VARCHAR(50) DEFAULT 'CUSTOM',
    description CLOB,
    problem_types VARCHAR(500),
    integration_type VARCHAR(30) NOT NULL,
    api_endpoint VARCHAR(500),
    script_content CLOB,
    params CLOB,
    enabled INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    create_by VARCHAR(100),
    update_by VARCHAR(100)
);

-- Algorithm parameter config table
CREATE TABLE IF NOT EXISTS solver_algorithm_config (
    id BIGINT PRIMARY KEY,
    algorithm_code VARCHAR(100) NOT NULL,
    config_data CLOB NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    create_by VARCHAR(100),
    update_by VARCHAR(100)
);

-- Data source table
CREATE TABLE IF NOT EXISTS data_source (
    id BIGINT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description CLOB,
    type VARCHAR(50) NOT NULL,
    host VARCHAR(500),
    port INT,
    database_name VARCHAR(200),
    username VARCHAR(200),
    password VARCHAR(500),
    options CLOB,
    status VARCHAR(30) DEFAULT 'ACTIVE',
    last_test_time TIMESTAMP,
    last_test_result VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    create_by VARCHAR(100),
    update_by VARCHAR(100)
);

-- Pipeline tables
CREATE TABLE IF NOT EXISTS data_pipeline (
    id BIGINT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description CLOB,
    definition CLOB,
    mode VARCHAR(30) DEFAULT 'BATCH',
    schedule VARCHAR(100),
    status VARCHAR(30) DEFAULT 'DRAFT',
    last_run_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    create_by VARCHAR(100),
    update_by VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS pipeline_execution (
    id BIGINT PRIMARY KEY,
    pipeline_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    node_statuses CLOB,
    error_message CLOB,
    metrics CLOB,
    workspace_id BIGINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Data API table
CREATE TABLE IF NOT EXISTS data_api (
    id BIGINT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description CLOB,
    path VARCHAR(500) NOT NULL,
    method VARCHAR(10) DEFAULT 'GET',
    data_source_id BIGINT,
    query CLOB,
    parameters CLOB,
    response_mapping CLOB,
    status VARCHAR(30) DEFAULT 'DRAFT',
    auth_required BOOLEAN DEFAULT TRUE,
    rate_limit INT DEFAULT 100,
    call_count BIGINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    create_by VARCHAR(100),
    update_by VARCHAR(100)
);

-- Orchestration tables
CREATE TABLE IF NOT EXISTS orchestration_flow (
    id BIGINT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description CLOB,
    version INT DEFAULT 1,
    definition CLOB,
    status VARCHAR(30) DEFAULT 'DRAFT',
    category VARCHAR(100),
    tags CLOB,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    create_by VARCHAR(100),
    update_by VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS flow_execution (
    id BIGINT PRIMARY KEY,
    flow_id BIGINT NOT NULL,
    flow_version INT,
    status VARCHAR(30) NOT NULL,
    input_params CLOB,
    output_result CLOB,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    node_executions CLOB,
    error_message CLOB,
    workspace_id BIGINT DEFAULT 1,
    create_by VARCHAR(100),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Service & Model registry
CREATE TABLE IF NOT EXISTS registered_service (
    id BIGINT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description CLOB,
    type VARCHAR(30) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    health_check_path VARCHAR(200),
    auth_config CLOB,
    input_schema CLOB,
    output_schema CLOB,
    status VARCHAR(30) DEFAULT 'ACTIVE',
    last_health_check TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    create_by VARCHAR(100),
    update_by VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS registered_model (
    id BIGINT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description CLOB,
    solver_problem_id BIGINT,
    algorithm_type VARCHAR(50),
    default_config CLOB,
    input_schema CLOB,
    output_schema CLOB,
    version VARCHAR(50),
    status VARCHAR(30) DEFAULT 'ACTIVE',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    create_by VARCHAR(100),
    update_by VARCHAR(100)
);

-- IAM tables
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(200) NOT NULL,
    nickname VARCHAR(100),
    email VARCHAR(200),
    phone VARCHAR(20),
    avatar VARCHAR(500),
    status VARCHAR(30) DEFAULT 'ACTIVE',
    last_login_time TIMESTAMP,
    last_login_ip VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    create_by VARCHAR(100),
    update_by VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(30) DEFAULT 'ACTIVE',
    menus CLOB,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    create_by VARCHAR(100),
    update_by VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    create_by VARCHAR(100),
    update_by VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY,
    code VARCHAR(200) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    module VARCHAR(100),
    description VARCHAR(500),
    type VARCHAR(30) DEFAULT 'API',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    create_by VARCHAR(100),
    update_by VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_code VARCHAR(200) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    create_by VARCHAR(100),
    update_by VARCHAR(100)
);

-- Audit tables
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(100),
    action VARCHAR(50) NOT NULL,
    resource VARCHAR(100),
    resource_id VARCHAR(100),
    detail CLOB,
    result VARCHAR(30),
    error_message CLOB,
    ip VARCHAR(50),
    user_agent VARCHAR(500),
    duration BIGINT,
    trace_id VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    create_by VARCHAR(100),
    update_by VARCHAR(100)
);
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS deleted INT DEFAULT 0;
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS create_by VARCHAR(100);
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS update_by VARCHAR(100);

CREATE TABLE IF NOT EXISTS audit_alert_rule (
    id BIGINT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    condition CLOB,
    severity VARCHAR(30) DEFAULT 'MEDIUM',
    enabled BOOLEAN DEFAULT TRUE,
    notification CLOB,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    create_by VARCHAR(100),
    update_by VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS audit_alert_trigger (
    id BIGINT PRIMARY KEY,
    rule_id BIGINT NOT NULL,
    rule_name VARCHAR(200),
    severity VARCHAR(30),
    matched_events CLOB,
    message CLOB,
    acknowledged BOOLEAN DEFAULT FALSE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Migration: add menus column to sys_role if not exists
ALTER TABLE sys_role ADD COLUMN IF NOT EXISTS menus CLOB;

-- Solver example problems (built-in templates loadable in problem creation)
CREATE TABLE IF NOT EXISTS solver_example (
    id BIGINT PRIMARY KEY,
    example_key VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    problem_type VARCHAR(30) NOT NULL,
    description CLOB,
    recommended_algorithm VARCHAR(200),
    difficulty VARCHAR(30),
    problem_definition CLOB NOT NULL,
    sort_order INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    create_by VARCHAR(100),
    update_by VARCHAR(100)
);

-- ==================== Workspace (multi-tenant) tables ====================

CREATE TABLE IF NOT EXISTS workspace (
    id BIGINT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(100) NOT NULL UNIQUE,
    description CLOB,
    owner_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    create_by VARCHAR(100),
    update_by VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS workspace_member (
    id BIGINT PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'MEMBER',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    create_by VARCHAR(100),
    update_by VARCHAR(100)
);
-- Unique constraint: one user can only join a workspace once
CREATE UNIQUE INDEX IF NOT EXISTS uk_ws_member ON workspace_member(workspace_id, user_id);

-- Add workspace_id to all business tables
ALTER TABLE solver_job ADD COLUMN IF NOT EXISTS workspace_id BIGINT;
ALTER TABLE solver_custom_algorithm ADD COLUMN IF NOT EXISTS workspace_id BIGINT;
ALTER TABLE solver_algorithm_config ADD COLUMN IF NOT EXISTS workspace_id BIGINT;
ALTER TABLE solver_example ADD COLUMN IF NOT EXISTS workspace_id BIGINT;
ALTER TABLE data_source ADD COLUMN IF NOT EXISTS workspace_id BIGINT;
ALTER TABLE data_pipeline ADD COLUMN IF NOT EXISTS workspace_id BIGINT;
ALTER TABLE data_api ADD COLUMN IF NOT EXISTS workspace_id BIGINT;
ALTER TABLE orchestration_flow ADD COLUMN IF NOT EXISTS workspace_id BIGINT;
ALTER TABLE registered_service ADD COLUMN IF NOT EXISTS workspace_id BIGINT;
ALTER TABLE registered_model ADD COLUMN IF NOT EXISTS workspace_id BIGINT;
-- Global tables (sys_user / audit_log) also carry a workspace_id column so the
-- interceptor's INSERT injection stays consistent; they are not tenant-isolated.
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS workspace_id BIGINT;
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS workspace_id BIGINT;

-- Create default workspace and assign all existing data to it
MERGE INTO workspace (id, name, code, description, owner_id) KEY(code) VALUES
    (1, '默认空间', 'default', '系统默认工作空间，包含所有历史数据', 1);

-- Assign all existing data to default workspace (id=1)
UPDATE solver_job SET workspace_id = 1 WHERE workspace_id IS NULL;
UPDATE solver_custom_algorithm SET workspace_id = 1 WHERE workspace_id IS NULL;
UPDATE solver_algorithm_config SET workspace_id = 1 WHERE workspace_id IS NULL;
UPDATE solver_example SET workspace_id = 1 WHERE workspace_id IS NULL;
UPDATE data_source SET workspace_id = 1 WHERE workspace_id IS NULL;
UPDATE data_pipeline SET workspace_id = 1 WHERE workspace_id IS NULL;
UPDATE data_api SET workspace_id = 1 WHERE workspace_id IS NULL;
UPDATE orchestration_flow SET workspace_id = 1 WHERE workspace_id IS NULL;
UPDATE registered_service SET workspace_id = 1 WHERE workspace_id IS NULL;
UPDATE registered_model SET workspace_id = 1 WHERE workspace_id IS NULL;
