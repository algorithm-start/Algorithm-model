package com.recplatform.web.config;

import com.recplatform.audit.interceptor.AuditInterceptor;
import com.recplatform.common.workspace.WorkspaceFilter;
import com.recplatform.iam.config.AdminWorkspaceInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RequestLoggingInterceptor requestLoggingInterceptor;
    private final AuditInterceptor auditInterceptor;
    private final WorkspaceFilter workspaceFilter;
    private final AdminWorkspaceInterceptor adminWorkspaceInterceptor;

    public WebMvcConfig(RequestLoggingInterceptor requestLoggingInterceptor,
                        AuditInterceptor auditInterceptor,
                        WorkspaceFilter workspaceFilter,
                        AdminWorkspaceInterceptor adminWorkspaceInterceptor) {
        this.requestLoggingInterceptor = requestLoggingInterceptor;
        this.auditInterceptor = auditInterceptor;
        this.workspaceFilter = workspaceFilter;
        this.adminWorkspaceInterceptor = adminWorkspaceInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Workspace context must be set before any business logic
        registry.addInterceptor(workspaceFilter)
                .addPathPatterns("/api/**")
                .order(0);

        // Admin detection runs after workspace header is extracted
        registry.addInterceptor(adminWorkspaceInterceptor)
                .addPathPatterns("/api/**")
                .order(1);

        registry.addInterceptor(requestLoggingInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/h2-console/**", "/swagger-ui/**", "/v3/api-docs/**",
                        "/doc.html", "/webjars/**", "/favicon.ico", "/uploads/**");

        registry.addInterceptor(auditInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/v1/audit/**");
    }
}
