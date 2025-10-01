package com.recplatform.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.recplatform.common.workspace.WorkspaceInterceptor;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus configuration with pagination interceptor and auto-fill handler.
 */
@Slf4j
@Configuration
public class MybatisPlusConfig {

    /**
     * Registers a JSR310-aware ObjectMapper with MyBatis-Plus JacksonTypeHandler so
     * that entity fields containing java.time types (e.g. LocalDateTime inside
     * nodeExecutions) can be serialized to JSON columns. The default handler's
     * ObjectMapper lacks the JavaTimeModule and would otherwise throw on write.
     */
    @PostConstruct
    public void configureJacksonTypeHandler() {
        ObjectMapper typeHandlerMapper = new ObjectMapper();
        typeHandlerMapper.registerModule(new JavaTimeModule());
        // Serialize java.time types as ISO strings instead of numeric arrays so the
        // JSON stored in the DB can be deserialized back reliably.
        typeHandlerMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        JacksonTypeHandler.setObjectMapper(typeHandlerMapper);
        log.info("JacksonTypeHandler ObjectMapper configured with JavaTimeModule");
    }

    /**
     * Pagination interceptor bean.
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // Workspace isolation interceptor must be added before pagination
        interceptor.addInnerInterceptor(new WorkspaceInterceptor());
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }

    /**
     * Auto-fill handler for createTime and updateTime fields.
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {

            @Override
            public void insertFill(MetaObject metaObject) {
                log.debug("MyBatis-Plus auto-fill on insert");
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

                // Auto-fill workspaceId if entity has the field and it's not already set
                if (metaObject.hasSetter("workspaceId")) {
                    Object existingValue = metaObject.getValue("workspaceId");
                    if (existingValue == null) {
                        Long wsId = com.recplatform.common.workspace.WorkspaceContextHolder.getWorkspaceId();
                        // Default to workspace 1 if no workspace context is set
                        metaObject.setValue("workspaceId", wsId != null ? wsId : 1L);
                    }
                }
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                log.debug("MyBatis-Plus auto-fill on update");
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
