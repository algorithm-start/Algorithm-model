package com.recplatform.common.workspace;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.delete.Delete;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.Set;

/**
 * MyBatis-Plus inner interceptor that automatically appends
 * workspace_id conditions to SELECT/UPDATE/DELETE statements
 * for tables that support workspace isolation.
 */
@Slf4j
public class WorkspaceInterceptor implements InnerInterceptor {

    /**
     * Tables that have workspace_id column and need filtering.
     */
    private static final Set<String> WORKSPACE_TABLES = Set.of(
            "solver_job", "solver_custom_algorithm", "solver_algorithm_config",
            "data_source", "data_pipeline", "data_api", "pipeline_execution",
            "orchestration_flow", "flow_execution", "registered_service", "registered_model"
    );

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
        if (WorkspaceContextHolder.shouldSkipFilter()) return;
        Long workspaceId = WorkspaceContextHolder.getWorkspaceId();
        if (workspaceId == null) return;

        String originalSql = boundSql.getSql();
        try {
            Statement statement = CCJSqlParserUtil.parse(originalSql);
            if (statement instanceof Select select) {
                PlainSelect plainSelect = getPlainSelect(select);
                if (plainSelect != null) {
                    Table table = getMainTable(plainSelect);
                    if (table != null && isWorkspaceTable(table.getName())) {
                        Expression where = plainSelect.getWhere();
                        Expression wsCondition = buildCondition(table, workspaceId);
                        plainSelect.setWhere(where == null ? wsCondition : new AndExpression(where, wsCondition));
                        setNewSql(boundSql, statement.toString());
                    }
                }
            }
        } catch (Exception e) {
            log.debug("WorkspaceInterceptor: failed to parse SQL for workspace filtering: {}", e.getMessage());
        }
    }

    @Override
    public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
        if (WorkspaceContextHolder.shouldSkipFilter()) return;
        // Default to workspace 1 when no context is present, so writes stay consistent with reads.
        Long workspaceId = WorkspaceContextHolder.getWorkspaceId();
        if (workspaceId == null) workspaceId = 1L;

        BoundSql boundSql = sh.getBoundSql();
        String originalSql = boundSql.getSql();

        try {
            Statement statement = CCJSqlParserUtil.parse(originalSql);
            boolean modified = false;

            if (statement instanceof Insert insert) {
                // Only inject workspace_id for tables that actually have the column.
                Table table = insert.getTable();
                if (table != null && isWorkspaceTable(table.getName())) {
                    // INSERT handled via string rewrite for jsqlparser version compatibility.
                    String rewritten = injectWorkspaceIdIntoInsertSql(originalSql, workspaceId);
                    if (rewritten != null) {
                        setNewSql(boundSql, rewritten);
                    }
                }
                return;
            } else if (statement instanceof Update update) {
                Table table = update.getTable();
                if (isWorkspaceTable(table.getName())) {
                    Expression where = update.getWhere();
                    Expression wsCondition = buildCondition(table, workspaceId);
                    update.setWhere(where == null ? wsCondition : new AndExpression(where, wsCondition));
                    modified = true;
                }
            } else if (statement instanceof Delete delete) {
                Table table = delete.getTable();
                if (isWorkspaceTable(table.getName())) {
                    Expression where = delete.getWhere();
                    Expression wsCondition = buildCondition(table, workspaceId);
                    delete.setWhere(where == null ? wsCondition : new AndExpression(where, wsCondition));
                    modified = true;
                }
            }

            if (modified) {
                setNewSql(boundSql, statement.toString());
            }
        } catch (Exception e) {
            log.debug("WorkspaceInterceptor: failed to modify SQL: {}", e.getMessage());
        }
    }

    /**
     * Rewrites an INSERT statement to include a literal workspace_id column/value.
     * Uses string manipulation to stay compatible across jsqlparser versions.
     * Returns the rewritten SQL, or null if no change is needed/possible.
     *
     * Handles the standard form: INSERT INTO table ( colA, colB ) VALUES ( ?, ? )
     */
    private String injectWorkspaceIdIntoInsertSql(String sql, Long workspaceId) {
        String lower = sql.toLowerCase();
        if (lower.contains("workspace_id")) return null;

        int valuesIdx = lower.indexOf(" values");
        if (valuesIdx < 0) return null;

        // Column list closing paren is the last ')' before VALUES.
        int colsClose = sql.lastIndexOf(')', valuesIdx);
        if (colsClose < 0) return null;

        // Values list closing paren is the last ')' in the statement.
        int valuesClose = sql.lastIndexOf(')');
        if (valuesClose <= colsClose) return null;

        // Append the column name and its literal value at the SAME (trailing) position
        // so the column/value alignment with the existing placeholders is preserved.
        StringBuilder rewritten = new StringBuilder(sql);
        // Insert the value literal right before the values list closing paren first
        // (do the later index first so the earlier index stays valid).
        rewritten.insert(valuesClose, ", " + workspaceId + " ");
        // Then insert the column name right before the column list closing paren.
        rewritten.insert(colsClose, ", workspace_id ");
        return rewritten.toString();
    }

    private boolean isWorkspaceTable(String tableName) {
        if (tableName == null) return false;
        return WORKSPACE_TABLES.contains(tableName.toLowerCase().replace("\"", "").replace("`", ""));
    }

    private Expression buildCondition(Table table, Long workspaceId) {
        Column column = new Column();
        if (table.getAlias() != null) {
            column.setTable(new Table(table.getAlias().getName()));
        }
        column.setColumnName("workspace_id");
        EqualsTo equals = new EqualsTo();
        equals.setLeftExpression(column);
        equals.setRightExpression(new LongValue(workspaceId));
        return equals;
    }

    private PlainSelect getPlainSelect(Select select) {
        if (select.getSelectBody() instanceof PlainSelect plainSelect) {
            return plainSelect;
        }
        return null;
    }

    private Table getMainTable(PlainSelect plainSelect) {
        FromItem fromItem = plainSelect.getFromItem();
        if (fromItem instanceof Table table) {
            return table;
        }
        return null;
    }

    private void setNewSql(BoundSql boundSql, String newSql) {
        try {
            Field sqlField = BoundSql.class.getDeclaredField("sql");
            sqlField.setAccessible(true);
            sqlField.set(boundSql, newSql);
        } catch (Exception e) {
            log.error("Failed to set new SQL in BoundSql", e);
        }
    }
}
