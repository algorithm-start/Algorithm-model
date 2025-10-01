package com.recplatform.data.query;

import com.recplatform.common.exception.BusinessException;
import com.recplatform.common.result.ResultCode;
import com.recplatform.data.datasource.DataSourceEntity;
import com.recplatform.data.datasource.DataSourceMapper;
import com.recplatform.data.datasource.DataSourceConnectionFactory;
import com.recplatform.data.query.dto.ColumnMeta;
import com.recplatform.data.query.dto.QueryRequest;
import com.recplatform.data.query.dto.QueryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OLAP query service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryServiceImpl implements QueryService {

    private final DataSourceMapper dataSourceMapper;
    private final DataSourceConnectionFactory connectionFactory;

    @Override
    public QueryResult executeQuery(QueryRequest request) {
        return doExecute(request, request.getLimit() != null ? request.getLimit() : 1000);
    }

    @Override
    public QueryResult preview(QueryRequest request) {
        return doExecute(request, Math.min(request.getLimit() != null ? request.getLimit() : 100, 100));
    }

    private QueryResult doExecute(QueryRequest request, int limit) {
        DataSourceEntity dataSource = dataSourceMapper.selectById(request.getDataSourceId());
        if (dataSource == null) {
            throw new BusinessException(ResultCode.NOT_FOUND,
                    "Data source not found: " + request.getDataSourceId());
        }

        String jdbcUrl = connectionFactory.buildJdbcUrl(dataSource);
        DataSource ds = connectionFactory.createTemporaryDataSource(
                jdbcUrl, dataSource.getUsername(), dataSource.getPassword());

        // Phase 1: acquire a connection. Failures here are genuine connectivity
        // problems (host down, auth rejected) and map to DATA_SOURCE_UNREACHABLE.
        Connection conn;
        try {
            conn = ds.getConnection();
        } catch (SQLException e) {
            log.warn("Failed to connect to data source [{}]: {}", dataSource.getName(), e.getMessage());
            throw new BusinessException(ResultCode.DATA_SOURCE_UNREACHABLE,
                    "无法连接数据源: " + e.getMessage(), e);
        }

        // Phase 2: run the query. Failures here are SQL problems (syntax, unknown
        // table/column, type mismatch) and must be reported as query errors so
        // users can see the actual SQL message instead of "source unreachable".
        long start = System.currentTimeMillis();
        try (Connection connection = conn) {
            String sql = appendLimitIfAbsent(request.getQuery(), limit);

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                Map<String, Object> params = request.getParameters();
                if (params != null) {
                    int idx = 1;
                    for (Object value : params.values()) {
                        stmt.setObject(idx++, value);
                    }
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    List<ColumnMeta> columns = new ArrayList<>();
                    for (int i = 1; i <= columnCount; i++) {
                        columns.add(ColumnMeta.builder()
                                .name(metaData.getColumnLabel(i))
                                .type(metaData.getColumnTypeName(i))
                                .nullable(metaData.isNullable(i) == ResultSetMetaData.columnNullable)
                                .build());
                    }

                    List<Map<String, Object>> rows = new ArrayList<>();
                    int rowCount = 0;
                    while (rs.next() && rowCount < limit) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            row.put(metaData.getColumnLabel(i), rs.getObject(i));
                        }
                        rows.add(row);
                        rowCount++;
                    }

                    long executeTimeMs = System.currentTimeMillis() - start;
                    return QueryResult.builder()
                            .columns(columns)
                            .rows(rows)
                            .total(rowCount)
                            .executeTimeMs(executeTimeMs)
                            .build();
                }
            }
        } catch (SQLException e) {
            log.warn("SQL execution failed on data source [{}]: {}", dataSource.getName(), e.getMessage());
            throw new BusinessException(ResultCode.DATA_QUERY_FAILED,
                    "SQL 执行失败: " + e.getMessage(), e);
        }
    }

    // Only append a LIMIT when the statement doesn't already end with one. We
    // check the trailing clause (ignoring a closing semicolon) rather than a
    // naive substring search, so a column or string literal named "limit" won't
    // suppress the safety cap.
    private static final java.util.regex.Pattern TRAILING_LIMIT =
            java.util.regex.Pattern.compile("(?is)\\blimit\\s+\\d+(\\s*,\\s*\\d+)?\\s*;?\\s*$");

    private String appendLimitIfAbsent(String rawSql, int limit) {
        String trimmed = rawSql.trim().replaceAll(";+\\s*$", "");
        if (TRAILING_LIMIT.matcher(trimmed).find()) {
            return trimmed;
        }
        return trimmed + " LIMIT " + limit;
    }
}
