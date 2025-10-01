package com.recplatform.data.datasource;

import com.recplatform.common.enums.DataSourceType;
import com.recplatform.common.exception.BusinessException;
import com.recplatform.common.result.ResultCode;
import com.recplatform.data.datasource.dto.ConnectionTestResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory for testing data source connections.
 */
@Slf4j
@Component
public class DataSourceConnectionFactory {

    /**
     * Test connectivity for the given data source entity.
     */
    public ConnectionTestResult testConnection(DataSourceEntity entity) {
        DataSourceType type = entity.getType();
        long start = System.currentTimeMillis();
        try {
            return switch (type) {
                case POSTGRESQL, MYSQL, ORACLE, GAUSSDB, CLICKHOUSE, H2 -> testJdbc(entity);
                case REST_API -> testRestApi(entity);
                case KAFKA -> testKafka(entity);
                default -> ConnectionTestResult.builder()
                        .success(false)
                        .message("Unsupported data source type: " + type)
                        .build();
            };
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            log.error("Connection test failed for data source [{}] type [{}]", entity.getName(), type, e);
            return ConnectionTestResult.builder()
                    .success(false)
                    .message("Connection failed: " + e.getMessage())
                    .latencyMs(latency)
                    .build();
        }
    }

    private static final int TCP_PROBE_TIMEOUT_MS = 2000;

    /**
     * Strip protocol prefixes (jdbc:mysql://, http://, etc.) that users sometimes
     * paste into the host field by mistake.
     */
    static String sanitizeHost(String host) {
        if (host == null) return null;
        String cleaned = host.trim();
        cleaned = cleaned.replaceFirst("(?i)^jdbc:\\w+://", "");
        cleaned = cleaned.replaceFirst("(?i)^https?://", "");
        // Remove trailing slash or port suffix that may have been included
        cleaned = cleaned.replaceFirst("[:/].*$", "");
        return cleaned;
    }

    /**
     * Builds a user-friendly, actionable message when a database host cannot be reached.
     * When the user filled in localhost/127.0.0.1, surfaces the most common pitfall in a
     * containerized deployment: localhost points to the application container itself, not
     * to the user's own machine. The host value is never auto-rewritten.
     */
    static String buildUnreachableMessage(String host, Integer port) {
        String hostPort = host + ":" + port;
        if (isLocalhost(host)) {
            return "无法连接到数据库 " + hostPort + "。当前应用运行在容器环境中，这里的 localhost / 127.0.0.1 "
                    + "指向的是应用容器自身，而非你的数据库所在主机。请将「主机」填写为数据库的实际访问地址："
                    + "若数据库与应用部署在同一物理机，请填写宿主机 IP 或 host.docker.internal；"
                    + "若是远程数据库，请填写其真实 IP 或域名。";
        }
        return "无法连接到数据库 " + hostPort + "，请检查主机地址、端口是否正确，以及数据库是否已启动、网络与防火墙是否放通。";
    }

    private static boolean isLocalhost(String host) {
        if (host == null) {
            return false;
        }
        String normalized = host.trim().toLowerCase();
        return normalized.equals("localhost")
                || normalized.equals("127.0.0.1")
                || normalized.equals("::1")
                || normalized.equals("0.0.0.0");
    }

    private ConnectionTestResult testJdbc(DataSourceEntity entity) {
        long start = System.currentTimeMillis();

        // H2 is a local/embedded database — skip the TCP socket probe that only
        // applies to remote JDBC servers.
        if (entity.getType() != DataSourceType.H2) {
            String host = sanitizeHost(entity.getHost());
            int port = entity.getPort() != null ? entity.getPort() : 3306;

            // Fast TCP probe: if the port is unreachable, fail immediately.
            try (java.net.Socket probe = new java.net.Socket()) {
                probe.connect(new java.net.InetSocketAddress(host, port), TCP_PROBE_TIMEOUT_MS);
            } catch (Exception e) {
                long latency = System.currentTimeMillis() - start;
                return ConnectionTestResult.builder()
                        .success(false)
                        .message(buildUnreachableMessage(host, entity.getPort()))
                        .latencyMs(latency)
                        .build();
            }
        }

        String jdbcUrl = buildJdbcUrl(entity);
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            java.util.concurrent.Future<Connection> future = executor.submit(
                    () -> java.sql.DriverManager.getConnection(jdbcUrl, entity.getUsername(), entity.getPassword()));
            try (Connection conn = future.get(6, java.util.concurrent.TimeUnit.SECONDS)) {
                conn.createStatement().execute("SELECT 1");
                long latency = System.currentTimeMillis() - start;
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("jdbcUrl", jdbcUrl);
                return ConnectionTestResult.builder()
                        .success(true)
                        .message("Connection successful")
                        .latencyMs(latency)
                        .metadata(metadata)
                        .build();
            }
        } catch (java.util.concurrent.TimeoutException e) {
            long latency = System.currentTimeMillis() - start;
            throw new BusinessException(ResultCode.DATA_SOURCE_UNREACHABLE,
                    "连接数据库超时（" + latency + "ms），请检查主机地址、端口及网络连通性。", e);
        } catch (java.util.concurrent.ExecutionException e) {
            String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            throw new BusinessException(ResultCode.DATA_SOURCE_UNREACHABLE,
                    "数据库连接失败：" + msg, e);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.DATA_SOURCE_UNREACHABLE,
                    "数据库连接失败：" + e.getMessage(), e);
        } finally {
            executor.shutdownNow();
        }
    }

    private ConnectionTestResult testRestApi(DataSourceEntity entity) {
        long start = System.currentTimeMillis();
        try {
            String baseUrl = (entity.getPort() != null)
                    ? entity.getHost() + ":" + entity.getPort()
                    : entity.getHost();
            java.net.URL url = new java.net.URL(baseUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            long latency = System.currentTimeMillis() - start;
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("responseCode", responseCode);
            if (responseCode < 500) {
                return ConnectionTestResult.builder()
                        .success(true)
                        .message("REST API reachable, response code: " + responseCode)
                        .latencyMs(latency)
                        .metadata(metadata)
                        .build();
            } else {
                return ConnectionTestResult.builder()
                        .success(false)
                        .message("REST API returned error: " + responseCode)
                        .latencyMs(latency)
                        .metadata(metadata)
                        .build();
            }
        } catch (Exception e) {
            throw new BusinessException(ResultCode.DATA_SOURCE_UNREACHABLE,
                    "REST API connection failed: " + e.getMessage(), e);
        }
    }

    private ConnectionTestResult testKafka(DataSourceEntity entity) {
        long start = System.currentTimeMillis();
        try {
            String bootstrapServers = entity.getHost() + ":" + entity.getPort();
            java.util.Properties props = new java.util.Properties();
            props.put("bootstrap.servers", bootstrapServers);
            props.put("request.timeout.ms", "5000");
            props.put("default.api.timeout.ms", "5000");
            try (org.apache.kafka.clients.admin.AdminClient adminClient =
                         org.apache.kafka.clients.admin.AdminClient.create(props)) {
                adminClient.describeCluster().clusterId().get();
            }
            long latency = System.currentTimeMillis() - start;
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("bootstrapServers", bootstrapServers);
            return ConnectionTestResult.builder()
                    .success(true)
                    .message("Kafka cluster reachable")
                    .latencyMs(latency)
                    .metadata(metadata)
                    .build();
        } catch (Exception e) {
            throw new BusinessException(ResultCode.DATA_SOURCE_UNREACHABLE,
                    "Kafka connection failed: " + e.getMessage(), e);
        }
    }

    /**
     * Build a JDBC URL based on data source type and configuration.
     */
    public String buildJdbcUrl(DataSourceEntity entity) {
        String host = sanitizeHost(entity.getHost());
        int port = entity.getPort() != null ? entity.getPort() : 3306;
        String database = entity.getDatabase();
        return switch (entity.getType()) {
            case POSTGRESQL -> String.format("jdbc:postgresql://%s:%d/%s?connectTimeout=5&socketTimeout=10&loginTimeout=5",
                    host, port, database);
            case MYSQL -> String.format("jdbc:mysql://%s:%d/%s?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=8000",
                    host, port, database);
            case ORACLE -> String.format("jdbc:oracle:thin:@%s:%d:%s",
                    host, port, database);
            case GAUSSDB -> String.format("jdbc:gaussdb://%s:%d/%s?connectTimeout=5&socketTimeout=10&loginTimeout=5",
                    host, port, database);
            case CLICKHOUSE -> String.format("jdbc:clickhouse://%s:%d/%s?connection_timeout=5000&socket_timeout=10000",
                    host, port, database);
            // H2 stores its full JDBC URL in the database field (file/mem paths
            // don't fit host:port), so we use it verbatim and only prepend the
            // jdbc:h2: scheme when missing.
            case H2 -> database != null && database.startsWith("jdbc:h2:")
                    ? database
                    : "jdbc:h2:" + database;
            default -> throw new BusinessException(ResultCode.BAD_REQUEST,
                    "Unsupported JDBC type: " + entity.getType());
        };
    }

    /**
     * Create a temporary HikariCP DataSource for testing.
     */
    public DataSource createTemporaryDataSource(String jdbcUrl, String username, String password) {
        com.zaxxer.hikari.HikariConfig config = new com.zaxxer.hikari.HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(0);
        // TCP reachability is already verified by the caller's socket probe,
        // so this timeout covers the JDBC authentication + SSL handshake over public network.
        config.setConnectionTimeout(6000);
        config.setValidationTimeout(3000);
        config.setMaxLifetime(30000);
        config.setInitializationFailTimeout(6000);
        return new com.zaxxer.hikari.HikariDataSource(config);
    }

    /**
     * Get schema metadata (tables and columns) for a JDBC data source.
     */
    public Map<String, Object> getMetadata(DataSourceEntity entity) {
        String jdbcUrl = buildJdbcUrl(entity);
        DataSource ds = createTemporaryDataSource(jdbcUrl, entity.getUsername(), entity.getPassword());
        try (Connection conn = ds.getConnection()) {
            java.sql.DatabaseMetaData dbMeta = conn.getMetaData();
            Map<String, Object> result = new HashMap<>();
            result.put("databaseProductName", dbMeta.getDatabaseProductName());
            result.put("databaseProductVersion", dbMeta.getDatabaseProductVersion());

            java.util.List<Map<String, Object>> tables = new java.util.ArrayList<>();
            try (java.sql.ResultSet rs = dbMeta.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    Map<String, Object> table = new HashMap<>();
                    String tableName = rs.getString("TABLE_NAME");
                    table.put("name", tableName);
                    table.put("schema", rs.getString("TABLE_SCHEM"));
                    table.put("type", rs.getString("TABLE_TYPE"));

                    java.util.List<Map<String, Object>> columns = new java.util.ArrayList<>();
                    try (java.sql.ResultSet cols = dbMeta.getColumns(null, null, tableName, "%")) {
                        while (cols.next()) {
                            Map<String, Object> col = new HashMap<>();
                            col.put("name", cols.getString("COLUMN_NAME"));
                            col.put("type", cols.getString("TYPE_NAME"));
                            col.put("size", cols.getInt("COLUMN_SIZE"));
                            col.put("nullable", "YES".equals(cols.getString("IS_NULLABLE")));
                            columns.add(col);
                        }
                    }
                    table.put("columns", columns);
                    tables.add(table);
                }
            }
            result.put("tables", tables);
            return result;
        } catch (SQLException e) {
            throw new BusinessException(ResultCode.DATA_SOURCE_UNREACHABLE,
                    "Failed to get metadata: " + e.getMessage(), e);
        }
    }
}
