-- Demo data for data integration and orchestration modules (PostgreSQL).
-- Uses INSERT ... ON CONFLICT so re-running on every startup is idempotent and
-- refreshes the seed rows without clobbering records the user creates manually
-- (those get auto-generated snowflake ids that never collide with these).

-- ==================== Data sources ====================
INSERT INTO data_source (id, name, description, type, host, port, database_name, username, password, status, deleted, create_by) VALUES
(960001, '平台内置库(演示)', '指向平台自身的内置库，可即席查询工作流/管道/数据源等元数据', 'H2', 'localhost', NULL, 'jdbc:h2:file:./data/recplatform;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;AUTO_SERVER=TRUE', 'sa', '', 'ACTIVE', 0, 'system'),
(960002, '分析库 OLAP(示例)', 'PostgreSQL 分析库示例（演示用，连接信息需按实际环境填写）', 'POSTGRESQL', 'analytics.recplatform.demo', 5432, 'analytics', 'reader', 'demo', 'INACTIVE', 0, 'system'),
(960003, '订单消息流(示例)', 'Kafka 订单消息流示例数据源', 'KAFKA', 'kafka.recplatform.demo', 9092, 'orders', '', '', 'INACTIVE', 0, 'system')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name, description = EXCLUDED.description, type = EXCLUDED.type,
    host = EXCLUDED.host, port = EXCLUDED.port, database_name = EXCLUDED.database_name,
    username = EXCLUDED.username, password = EXCLUDED.password, status = EXCLUDED.status, deleted = EXCLUDED.deleted;

-- ==================== Orchestration flows ====================
INSERT INTO orchestration_flow (id, name, description, version, definition, status, category, tags, deleted, create_by) VALUES
(940001, '每日补货优化流程', '定时拉取库存与销量数据，调用求解器生成补货方案并回写', 1,
 '{"nodes":[{"id":"n1","type":"START","name":"开始","position":{"x":80,"y":160}},{"id":"n2","type":"DATA_TRANSFORM","name":"拉取库存销量","config":{"operation":"map","source":"inventory_daily"},"position":{"x":280,"y":160}},{"id":"n3","type":"SOLVER_INVOKE","name":"补货量求解","config":{"problemId":"replenish-demo","algorithm":"MILP"},"position":{"x":500,"y":160}},{"id":"n4","type":"SERVICE_CALL","name":"回写补货单","config":{"url":"http://erp.internal/api/replenish","method":"POST"},"position":{"x":720,"y":160}},{"id":"n5","type":"END","name":"结束","position":{"x":920,"y":160}}],"edges":[{"id":"e1","source":"n1","target":"n2"},{"id":"e2","source":"n2","target":"n3"},{"id":"e3","source":"n3","target":"n4"},{"id":"e4","source":"n4","target":"n5"}],"globalParams":{"region":"east"}}',
 'PUBLISHED', 'supply-chain', '["补货","调度","定时"]', 0, 'system'),
(940002, '配送路径规划流程', '聚合订单后调用 VRP 求解器规划车辆路径，结果推送调度系统', 1,
 '{"nodes":[{"id":"n1","type":"START","name":"开始","position":{"x":80,"y":160}},{"id":"n2","type":"DATA_TRANSFORM","name":"聚合待配订单","config":{"operation":"aggregate","window":"30m"},"position":{"x":280,"y":160}},{"id":"n3","type":"SOLVER_INVOKE","name":"VRP 路径求解","config":{"problemId":"vrp-demo","algorithm":"ortools"},"position":{"x":500,"y":160}},{"id":"n4","type":"DECISION_GATE","name":"是否超时限","config":{"condition":"cost <= budget"},"position":{"x":720,"y":160}},{"id":"n5","type":"SERVICE_CALL","name":"下发调度系统","config":{"url":"http://scheduler.internal/dispatch","method":"POST"},"position":{"x":940,"y":90}},{"id":"n6","type":"SCRIPT","name":"人工复核告警","config":{"language":"groovy","script":"return [alert: true]"},"position":{"x":940,"y":240}},{"id":"n7","type":"END","name":"结束","position":{"x":1140,"y":160}}],"edges":[{"id":"e1","source":"n1","target":"n2"},{"id":"e2","source":"n2","target":"n3"},{"id":"e3","source":"n3","target":"n4"},{"id":"e4","source":"n4","target":"n5","condition":"true"},{"id":"e5","source":"n4","target":"n6","condition":"false"},{"id":"e6","source":"n5","target":"n7"},{"id":"e7","source":"n6","target":"n7"}],"globalParams":{}}',
 'PUBLISHED', 'logistics', '["路径规划","VRP","实时"]', 0, 'system'),
(940003, '价格弹性建模流程', '每周拉取交易数据，训练价格弹性模型并刷新定价策略', 2,
 '{"nodes":[{"id":"n1","type":"START","name":"开始","position":{"x":80,"y":160}},{"id":"n2","type":"DATA_TRANSFORM","name":"清洗交易数据","config":{"operation":"filter"},"position":{"x":300,"y":160}},{"id":"n3","type":"SCRIPT","name":"训练弹性模型","config":{"language":"groovy","script":"return [elasticity: 1.2]","runtime":"python"},"position":{"x":520,"y":160}},{"id":"n4","type":"SERVICE_CALL","name":"刷新定价策略","config":{"url":"http://pricing.internal/refresh","method":"POST"},"position":{"x":740,"y":160}},{"id":"n5","type":"END","name":"结束","position":{"x":960,"y":160}}],"edges":[{"id":"e1","source":"n1","target":"n2"},{"id":"e2","source":"n2","target":"n3"},{"id":"e3","source":"n3","target":"n4"},{"id":"e4","source":"n4","target":"n5"}],"globalParams":{}}',
 'DRAFT', 'pricing', '["定价","建模","周调度"]', 0, 'system')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name, description = EXCLUDED.description, version = EXCLUDED.version,
    definition = EXCLUDED.definition, status = EXCLUDED.status, category = EXCLUDED.category,
    tags = EXCLUDED.tags, deleted = EXCLUDED.deleted;

-- ==================== Data pipelines ====================
INSERT INTO data_pipeline (id, name, description, definition, mode, schedule, status, last_run_time, deleted, create_by) VALUES
(950001, '库存日增量同步', '每日将业务库 inventory 表增量同步到分析库，供求解器消费', '{"nodes":[{"id":"node-1","type":"EXTRACT","subType":"DB_QUERY","name":"读取库存表","config":{"sourceId":960001,"table":"inventory","sql":"SELECT * FROM inventory WHERE update_time >= :last_run"},"position":{"x":80,"y":160}},{"id":"node-2","type":"TRANSFORM","subType":"FILTER","name":"增量过滤","config":{"condition":"update_time >= :last_run"},"position":{"x":320,"y":160}},{"id":"node-3","type":"TRANSFORM","subType":"MAP","name":"字段映射","config":{"mapping":"sku_id -> sku\\nqty -> stock"},"position":{"x":560,"y":160}},{"id":"node-4","type":"LOAD","subType":"DB_WRITE","name":"写入分析库","config":{"targetId":960002,"table":"ods_inventory","writeMode":"INSERT"},"position":{"x":800,"y":160}}],"edges":[{"id":"e1","source":"node-1","target":"node-2"},{"id":"e2","source":"node-2","target":"node-3"},{"id":"e3","source":"node-3","target":"node-4"}]}', 'BATCH', '0 0 2 * * ?', 'READY', '2026-06-01 02:00:00', 0, 'system'),
(950002, '订单实时清洗管道', '消费订单消息流，实时清洗后写入特征库供路径规划使用', '{"nodes":[{"id":"node-1","type":"EXTRACT","subType":"API_CALL","name":"消费订单流","config":{"topic":"orders"},"position":{"x":80,"y":160}},{"id":"node-2","type":"TRANSFORM","subType":"FILTER","name":"过滤已支付","config":{"condition":"status = ''PAID''"},"position":{"x":300,"y":160}},{"id":"node-3","type":"TRANSFORM","subType":"MAP","name":"字段映射","config":{"mapping":"addr -> address"},"position":{"x":520,"y":160}},{"id":"node-4","type":"TRANSFORM","subType":"AGGREGATE","name":"按城市聚合","config":{"groupBy":"city","aggregation":"SUM(amount)"},"position":{"x":740,"y":160}},{"id":"node-5","type":"LOAD","subType":"DB_WRITE","name":"写入特征库","config":{"targetId":960002,"table":"dwd_order_realtime","writeMode":"UPSERT"},"position":{"x":960,"y":160}}],"edges":[{"id":"e1","source":"node-1","target":"node-2"},{"id":"e2","source":"node-2","target":"node-3"},{"id":"e3","source":"node-3","target":"node-4"},{"id":"e4","source":"node-4","target":"node-5"}]}', 'STREAM', NULL, 'RUNNING', '2026-06-02 00:15:00', 0, 'system'),
(950003, '销量周聚合管道', '每周聚合销量数据生成训练样本，供价格弹性模型使用', '{"nodes":[{"id":"node-1","type":"EXTRACT","subType":"DB_QUERY","name":"读取订单明细","config":{"sourceId":960001,"table":"order_item","sql":"SELECT * FROM order_item"},"position":{"x":80,"y":160}},{"id":"node-2","type":"TRANSFORM","subType":"AGGREGATE","name":"按周聚合销量","config":{"groupBy":"sku_id, week","aggregation":"SUM(qty), AVG(price)"},"position":{"x":340,"y":160}},{"id":"node-3","type":"LOAD","subType":"DB_WRITE","name":"写入周聚合表","config":{"targetId":960002,"table":"dws_sales_weekly","writeMode":"REPLACE"},"position":{"x":600,"y":160}}],"edges":[{"id":"e1","source":"node-1","target":"node-2"},{"id":"e2","source":"node-2","target":"node-3"}]}', 'BATCH', '0 0 3 ? * MON', 'DRAFT', NULL, 0, 'system')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name, description = EXCLUDED.description, definition = EXCLUDED.definition,
    mode = EXCLUDED.mode, schedule = EXCLUDED.schedule, status = EXCLUDED.status,
    last_run_time = EXCLUDED.last_run_time, deleted = EXCLUDED.deleted;

-- Assign all demo data to the default workspace (id=1) so it is visible under workspace filtering.
UPDATE data_source SET workspace_id = 1 WHERE workspace_id IS NULL;
UPDATE orchestration_flow SET workspace_id = 1 WHERE workspace_id IS NULL;
UPDATE data_pipeline SET workspace_id = 1 WHERE workspace_id IS NULL;
