package com.recplatform.web.controller;

import com.recplatform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/platform")
@Tag(name = "Platform", description = "Platform health and module information")
public class PlatformController {

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Returns platform health status and system information")
    public Result<HealthInfo> health() {
        HealthInfo info = new HealthInfo();
        info.setStatus("UP");
        info.setPlatform("RecPlatform Algorithm Platform");
        info.setVersion("1.0.0");
        info.setTimestamp(LocalDateTime.now().toString());
        info.setJavaVersion(System.getProperty("java.version"));
        info.setOsName(System.getProperty("os.name"));
        info.setOsVersion(System.getProperty("os.version"));
        info.setOsArch(System.getProperty("os.arch"));
        return Result.success(info);
    }

    @GetMapping("/modules")
    @Operation(summary = "List modules", description = "Lists all available platform modules with their API base paths")
    public Result<List<ModuleInfo>> modules() {
        List<ModuleInfo> modules = new ArrayList<>();
        modules.add(new ModuleInfo("Solver", "/api/v1/solver"));
        modules.add(new ModuleInfo("Data", "/api/v1/data"));
        modules.add(new ModuleInfo("Orchestrator", "/api/v1/orchestrator"));
        modules.add(new ModuleInfo("IAM", "/api/v1/iam"));
        modules.add(new ModuleInfo("Audit", "/api/v1/audit"));
        return Result.success(modules);
    }

    @Data
    public static class HealthInfo {
        private String status;
        private String platform;
        private String version;
        private String timestamp;
        private String javaVersion;
        private String osName;
        private String osVersion;
        private String osArch;
    }

    @Data
    public static class ModuleInfo {
        private String name;
        private String basePath;

        public ModuleInfo() {
        }

        public ModuleInfo(String name, String basePath) {
            this.name = name;
            this.basePath = basePath;
        }
    }
}
